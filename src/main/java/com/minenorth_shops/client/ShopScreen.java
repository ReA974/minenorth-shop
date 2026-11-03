package com.minenorth_shops.client;

import com.minenorth_eurobank.Money;
import com.minenorth_shops.MineNorthShops;
import com.minenorth_shops.Network;
import com.minenorth_shops.packet.ShopActionPacket;
import com.minenorth_shops.packet.ShopStatePacket;
import com.minenorth_shops.shop.Shop;
import com.minenorth_shops.shop.ShopEntry;
import com.minenorth_shops.shop.ShopMode;
import com.minenorth_shops.shop.ShopService.Method;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.minenorth_shops.client.MineNorthStyle.*;

/**
 * Écran de boutique côté joueur, charte MineNorth : liste des articles à gauche (noms sur deux lignes, réduits puis coupés
 * avec le nom complet en infobulle), à droite la vitrine 3D de l'article choisi (porté par le joueur pour un vêtement)
 * et l'achat. La taille suit la fenêtre pour rester utilisable en échelle d'interface 3.
 */
@OnlyIn(Dist.CLIENT)
public class ShopScreen extends Screen {
    private static final ResourceLocation LOGO = new ResourceLocation(MineNorthShops.MODID, "textures/gui/logo.png");
    private static final int ROW_H = 24, ROW_GAP = 2, HEAD = 40, FOOT = 14, PAD = 8;

    private ShopStatePacket st;
    private String message;
    private int selected = -1;   // id de l'article
    private int lots = 1;
    private int offset;
    private boolean silent;
    /** Rubrique affichée : null = tout, "" = articles sans rubrique. */
    private String cat;
    private int tabOffset;

    // mise en page (recalculée à chaque init)
    private int W, H, left, top, bodyTop, bodyBottom, listW, rows, rx, rw, viewH;

    // vitrine : rotation automatique, glisser pour tourner
    private float yaw = 200f;
    private long lastFrame;
    private boolean dragging;
    private long dragUntil;

    public ShopScreen(ShopStatePacket st) {
        super(Component.literal(st.shop.name));
        this.st = st;
        this.message = st.message;
        if (!st.shop.entries.isEmpty()) selected = st.shop.entries.get(0).id;
    }

    public int shopId() {
        return st.shop.id;
    }

    /** Remplacé par une autre boutique : ne pas prévenir le serveur de la fermeture. */
    public void silent() {
        silent = true;
    }

    public void closeFromServer() {
        silent = true;
        minecraft.setScreen(null);
    }

    public void update(ShopStatePacket n) {
        this.st = n;
        if (!n.message.isEmpty()) this.message = n.message;
        if (selectedEntry() == null) {
            selected = n.shop.entries.isEmpty() ? -1 : n.shop.entries.get(0).id;
            lots = 1;
        }
        rebuildWidgets();
    }

    private Shop shop() {
        return st.shop;
    }

    private boolean hasCategories() {
        return !shop().categories().isEmpty();
    }

    /** Articles de la rubrique choisie (tous s'il n'y a pas de rubrique). */
    private List<ShopEntry> visible() {
        if (cat == null || !hasCategories()) return shop().entries;
        List<ShopEntry> out = new ArrayList<>();
        for (ShopEntry e : shop().entries) if (e.category.equals(cat)) out.add(e);
        return out;
    }

    /** Onglets : Tous, chaque rubrique, puis « Autres » s'il reste des articles sans rubrique. */
    private List<String> tabs() {
        List<String> t = new ArrayList<>();
        t.add(null);
        t.addAll(shop().categories());
        for (ShopEntry e : shop().entries) if (e.category.isEmpty()) { t.add(""); break; }
        return t;
    }

    private static String tabLabel(String c) {
        return c == null ? "Tous" : c.isEmpty() ? "Autres" : c;
    }

    private void chooseCat(String c) {
        cat = c;
        offset = 0;
        lots = 1;
        List<ShopEntry> v = visible();
        selected = v.isEmpty() ? -1 : v.get(0).id;
        rebuildWidgets();
    }

    private void select(int id) {
        if (id == selected) return;
        selected = id;
        lots = 1;
        yaw = 200f;
        rebuildWidgets();
    }

    @Nullable
    private ShopEntry selectedEntry() {
        return shop().entry(selected);
    }

    private int have(ShopEntry e) {
        int i = shop().entries.indexOf(e);
        return i >= 0 && i < st.have.length ? st.have[i] : 0;
    }

    private long total() {
        ShopEntry e = selectedEntry();
        return e == null ? 0 : e.price * lots;
    }

    private int maxLots() {
        ShopEntry e = selectedEntry();
        if (e == null) return 1;
        long max;
        if (shop().mode == ShopMode.BUY) {
            max = have(e) / Math.max(1, e.quantity);
        } else if (e.price <= 0) {
            max = st.maxLots;
        } else {
            long money = Math.max(shop().allowCash ? st.cash : 0, shop().allowCard && st.cardIssue.isEmpty() ? st.balance : 0);
            max = money / e.price;
        }
        return (int) Math.max(1, Math.min(st.maxLots, max));
    }

    private void setLots(int n) {
        lots = Math.max(1, Math.min(st.maxLots, n));
        rebuildWidgets();
    }

    private void trade(Method method) {
        ShopEntry e = selectedEntry();
        if (e == null) return;
        Network.CHANNEL.sendToServer(new ShopActionPacket(ShopActionPacket.Action.TRADE, e.id, lots, e.price, method));
    }

    private ShopButton btn(int x, int y, int w, int h, String label, int color, Runnable r) {
        return addRenderableWidget(new ShopButton(x, y, w, h, Component.literal(label), color, r));
    }

    // ------------------------------------------------------------------ mise en page

    private void layout() {
        W = Math.min(460, width - 12);
        H = Math.min(280, height - 12);
        left = (width - W) / 2;
        top = (height - H) / 2;
        bodyTop = top + HEAD + 4 + (hasCategories() ? 18 : 0);
        bodyBottom = top + H - FOOT - 4;
        listW = (W - 3 * PAD) * 53 / 100;
        rows = Math.max(1, (bodyBottom - bodyTop + ROW_GAP) / (ROW_H + ROW_GAP));
        rx = left + PAD * 2 + listW;
        rw = left + W - PAD - rx;
        viewH = Math.max(40, bodyBottom - bodyTop - 78);
    }

    private int listX() {
        return left + PAD;
    }

    /** Largeur des lignes (place gardée pour la barre de défilement si besoin). */
    private int rowW() {
        return listW - (visible().size() > rows ? 6 : 0);
    }

    @Override
    protected void init() {
        Shop shop = shop();
        if (cat != null && !tabs().contains(cat)) cat = null;   // la rubrique n'existe plus
        layout();
        ShopEntry e = selectedEntry();
        boolean sel = e != null && st.licenceOk;   // sans la licence : consultation seulement
        long total = total();

        btn(left + W - PAD - 16, top + 7, 16, 16, "✕", DARK, this::onClose).setTooltip(
                net.minecraft.client.gui.components.Tooltip.create(Component.literal("Quitter")));

        if (hasCategories()) initTabs();

        // quantité : [-] xN [+] [Max]
        int qy = bodyTop + viewH + 34;
        btn(rx, qy, 16, 16, "-", DARK, () -> setLots(lots - 1)).enabled(sel && lots > 1);
        btn(rx + 44, qy, 16, 16, "+", DARK, () -> setLots(lots + 1)).enabled(sel && lots < st.maxLots);
        btn(rx + 63, qy, 30, 16, "Max", DARK, () -> setLots(maxLots())).enabled(sel);

        int by = bodyBottom - 20, bw = (rw - 4) / 2;
        if (shop.mode == ShopMode.SELL) {
            boolean cashOk = sel && shop.allowCash && st.cash >= total;
            boolean cardOk = sel && shop.allowCard && st.cardIssue.isEmpty() && st.balance >= total;
            btn(rx, by, bw, 20, shop.allowCash ? "Payer en espèces" : "Espèces refusées", CYAN,
                    () -> trade(Method.CASH)).enabled(cashOk);
            btn(rx + bw + 4, by, rw - bw - 4, 20, shop.allowCard ? "Payer par carte" : "Carte refusée", PINK,
                    () -> trade(Method.CARD)).enabled(cardOk);
        } else {
            boolean enough = sel && have(e) >= e.quantity * lots;
            btn(rx, by, bw, 20, shop.allowCash ? "Vendre (espèces)" : "Espèces refusées", ShopButton.GREEN,
                    () -> trade(Method.CASH)).enabled(enough && shop.allowCash);
            btn(rx + bw + 4, by, rw - bw - 4, 20, shop.allowCard ? "Vendre (compte)" : "Virement refusé", PINK,
                    () -> trade(Method.CARD)).enabled(enough && shop.allowCard && st.hasAccount);
        }
    }

    /** Barre de rubriques sous l'en-tête, avec flèches si elles ne tiennent pas toutes. */
    private void initTabs() {
        List<String> tabs = tabs();
        int x0 = left + PAD, x1 = left + W - PAD, y = top + HEAD + 4;
        int[] w = new int[tabs.size()];
        int total = 0;
        for (int i = 0; i < w.length; i++) {
            w[i] = Math.max(30, Math.min(90, font.width(tabLabel(tabs.get(i))) + 14));
            total += w[i] + 3;
        }
        boolean arrows = total > x1 - x0;
        int from = x0 + (arrows ? 17 : 0), to = x1 - (arrows ? 17 : 0);
        tabOffset = Math.max(0, Math.min(tabOffset, tabs.size() - 1));
        int x = from, last = tabOffset;
        for (int i = tabOffset; i < tabs.size(); i++) {
            if (x + w[i] > to && i > tabOffset) break;
            String c = tabs.get(i);
            btn(x, y, Math.min(w[i], to - x), 14, tabLabel(c), Objects.equals(c, cat) ? CYAN : DARK, () -> chooseCat(c));
            x += w[i] + 3;
            last = i;
        }
        if (arrows) {
            final int lastShown = last;
            btn(x0, y, 14, 14, "<", DARK, () -> { tabOffset = Math.max(0, tabOffset - 1); rebuildWidgets(); }).enabled(tabOffset > 0);
            btn(x1 - 14, y, 14, 14, ">", DARK, () -> { tabOffset = Math.min(tabs.size() - 1, tabOffset + 1); rebuildWidgets(); })
                    .enabled(lastShown < tabs.size() - 1);
        }
    }

    @Override
    public void removed() {
        if (!silent) Network.CHANNEL.sendToServer(ShopActionPacket.close());
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ souris

    private boolean inView(double mx, double my) {
        return mx >= rx && mx < rx + rw && my >= bodyTop && my < bodyTop + viewH;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int max = Math.max(0, visible().size() - rows);
        offset = Math.max(0, Math.min(max, offset - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0 && inView(mx, my) && selectedEntry() != null) {
            dragging = true;
            return true;
        }
        List<ShopEntry> list = visible();
        int x = listX(), w = rowW();
        for (int i = 0; i < rows; i++) {
            int idx = offset + i, y = bodyTop + i * (ROW_H + ROW_GAP);
            if (idx < list.size() && mx >= x && mx < x + w && my >= y && my < y + ROW_H) {
                select(list.get(idx).id);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) {
            yaw += (float) dx * 2.5f;
            dragUntil = System.currentTimeMillis() + 2500;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    // ------------------------------------------------------------------ rendu

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        Shop shop = shop();
        boolean buy = shop.mode == ShopMode.BUY;
        rfill(g, left - 3, top - 3, left + W + 3, top + H + 3, 10, FRAME);
        rfill(g, left, top, left + W, top + H, 8, PANEL);

        Component tooltip = null;
        ShopEntry tipItem = null;

        // en-tête : logo, nom de la boutique (réduit puis coupé), type et licence ; à droite l'argent du joueur
        RenderSystem.enableBlend();
        g.blit(LOGO, left + PAD, top + 6, 28, 28, 0, 0, 96, 96, 96, 96);
        int walletW = Math.min(110, W / 4);
        int walletX = left + W - PAD - 22 - walletW;
        int tx = left + PAD + 34, titleW = walletX - 8 - tx;
        String title = shop.name.toUpperCase(Locale.ROOT);
        if (line(g, font, bold(title), tx, top + 8, titleW, 1.5f, 0.8f, WHITE, false)
                && mx >= tx && mx < tx + titleW && my >= top + 6 && my < top + 22) tooltip = Component.literal(shop.name);
        String header = buy ? "Rachat d'objets" : "Boutique";
        g.drawString(font, header, tx, top + 25, CYAN, false);
        if (!st.licenceName.isEmpty()) {   // licence exigée : verte si le joueur l'a, rouge sinon
            int lx = tx + font.width(header) + 6, lw = walletX - 6 - lx;
            String lic = fit(font, (st.licenceOk ? "✔ " : "✖ ") + st.licenceName, Math.max(10, lw - 12));
            if (lw > 30) tag(g, font, lic, lx, top + 23, st.licenceOk ? OK : ALERT);
        }
        wallet(g, walletX, top + 7, walletW, "Espèces", Money.format(st.cash), WHITE);
        boolean cardBad = shop.allowCard && !buy && !st.cardIssue.isEmpty();
        wallet(g, walletX, top + 21, walletW, "Compte", st.hasAccount ? Money.format(st.balance) : "aucun",
                !st.hasAccount ? MUTED : cardBad ? WARN : WHITE);
        if (cardBad && mx >= walletX && mx < walletX + walletW && my >= top + 19 && my < top + 31)
            tooltip = Component.literal("Carte : " + st.cardIssue);
        rfill(g, left + PAD, top + HEAD, left + W - PAD, top + HEAD + 2, 1, HOVER);

        // liste des articles
        List<ShopEntry> list = visible();
        offset = Math.max(0, Math.min(offset, Math.max(0, list.size() - rows)));
        int x = listX(), w = rowW();
        int priceW = 0;   // colonne de droite : prix et sous-titre (« Lot de 5 ») ; le nom prend le reste
        for (ShopEntry it : list) priceW = Math.max(priceW, Math.max(font.width(bold(Money.format(it.price))),
                (int) Math.ceil(font.width(subtitle(it, buy)) * 0.75f)));
        priceW = Math.min(priceW, w / 3);
        for (int i = 0; i < rows; i++) {
            int idx = offset + i;
            if (idx >= list.size()) break;
            ShopEntry it = list.get(idx);
            int y = bodyTop + i * (ROW_H + ROW_GAP);
            boolean on = it.id == selected, hov = mx >= x && mx < x + w && my >= y && my < y + ROW_H;
            card(g, x, y, w, ROW_H, on ? mix(CYAN, LIST, 0.55f) : hov ? HOVER : LIST, on ? CYAN : hov ? DARK : mix(DARK, LIST, 0.4f));
            g.renderItem(it.item, x + 7, y + 4);
            g.renderItemDecorations(font, it.item, x + 7, y + 4, it.quantity > 1 ? String.valueOf(it.quantity) : null);

            int nx = x + 27, nw = w - 27 - priceW - 10;
            boolean cut = block(g, font, it.item.getHoverName().getString(), nx, y + 2, nw, ROW_H - 4, WHITE, false);
            line(g, font, bold(Money.format(it.price)), x + w - 5 - priceW, y + 4, priceW, 1f, 0.75f, WHITE, true);
            line(g, font, Component.literal(subtitle(it, buy)), x + w - 5 - priceW, y + 14, priceW, 0.75f, 0.6f, on ? TEXT : MUTED, true);
            if (hov && (cut || mx < x + 25)) tipItem = it;
        }
        if (list.isEmpty())
            block(g, font, shop.entries.isEmpty() ? "Cette boutique est vide." : "Aucun article dans cette rubrique.",
                    x + 6, bodyTop + 4, listW - 12, 20, TEXT, false);
        if (list.size() > rows) {   // barre de défilement
            int sx = x + listW - 3, sh = rows * (ROW_H + ROW_GAP) - ROW_GAP;
            rfill(g, sx, bodyTop, sx + 3, bodyTop + sh, 1, LIST);
            int th = Math.max(12, sh * rows / list.size());
            int ty = bodyTop + (sh - th) * offset / Math.max(1, list.size() - rows);
            rfill(g, sx, ty, sx + 3, ty + th, 1, DARK);
        }

        // vitrine et détail de l'article choisi
        ShopEntry e = selectedEntry();
        rgradient(g, rx, bodyTop, rx + rw, bodyTop + viewH, 7, mix(DARK, LIST, 0.55f), LIST);
        if (e != null) {
            showcase(g, e, mx, my);
            if (inView(mx, my) && !dragging) tipItem = e;
            int ny = bodyTop + viewH + 3;
            if (block(g, font, e.item.getHoverName().getString(), rx + 2, ny, rw - 4, 19, WHITE, true)
                    && mx >= rx && mx < rx + rw && my >= ny && my < ny + 19) tipItem = e;
            String unit = Money.format(e.price) + (e.quantity > 1 ? " le lot de " + e.quantity : " l'unité");
            line(g, font, Component.literal(unit), rx + 2, ny + 21, rw - 4, 1f, 0.7f, MUTED, false);

            int qy = bodyTop + viewH + 34;
            g.drawCenteredString(font, "x" + lots, rx + 30, qy + 4, WHITE);
            int ttx = rx + 98, ttw = rx + rw - ttx;
            g.drawString(font, buy ? "Reçu" : "Total", ttx, qy - 1, CYAN, false);
            line(g, font, bold(Money.format(total())), ttx, qy + 8, ttw, 1f, 0.6f, WHITE, false);
        } else {
            block(g, font, "Choisissez un article", rx + 6, bodyTop, rw - 12, viewH, MUTED, false);
        }

        // pied : message du serveur (ou souci de carte)
        String foot = message != null && !message.isEmpty() ? message : cardBad ? "Carte : " + st.cardIssue : "";
        if (!foot.isEmpty()) {
            int fy = top + H - FOOT - 2;
            if (line(g, font, Component.literal(foot), left + PAD + 2, fy + 3, W - 2 * PAD - 4, 1f, 0.75f, WARN, false)
                    && my >= fy && my < fy + FOOT && mx >= left && mx < left + W) tooltip = Component.literal(foot);
        }

        super.render(g, mx, my, pt);
        if (tipItem != null) g.renderTooltip(font, tipItem.item, mx, my);
        else if (tooltip != null) g.renderTooltip(font, font.split(tooltip, 220), mx, my);
    }

    private String subtitle(ShopEntry it, boolean buy) {
        return buy ? "Vous avez " + have(it) : it.quantity > 1 ? "Lot de " + it.quantity : "À l'unité";
    }

    /** Une ligne du porte-monnaie : libellé à gauche, montant en gras à droite. */
    private void wallet(GuiGraphics g, int x, int y, int w, String label, String amount, int color) {
        g.drawString(font, label, x, y, MUTED, false);
        int lw = font.width(label) + 4;
        line(g, font, bold(amount), x + lw, y, w - lw, 1f, 0.6f, color, true);
    }

    /** L'article dans la vitrine : porté par le joueur si c'est un vêtement, sinon agrandi ; tourne tout seul. */
    private void showcase(GuiGraphics g, ShopEntry e, int mx, int my) {
        long now = System.nanoTime();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        if (!dragging && System.currentTimeMillis() > dragUntil) yaw += dt * 40f;

        int cx = rx + rw / 2;
        g.enableScissor(rx + 1, bodyTop + 1, rx + rw - 1, bodyTop + viewH - 1);
        ItemPreview.Wear wear = ItemPreview.wear(e.item);
        if (wear != null) {
            // sol de la vitrine
            rfill(g, cx - rw / 4, bodyTop + viewH - 10, cx + rw / 4, bodyTop + viewH - 5, 3, mix(DARK, LIST, 0.35f));
            int scale = Math.max(14, (int) ((viewH - 14) / 2.05f));
            ItemPreview.player(g, cx, bodyTop + viewH - 7, scale, yaw, e.item, wear);
        } else {
            float size = Math.min(rw, viewH) * 0.95f;
            ItemPreview.item(g, cx, bodyTop + viewH / 2, size, yaw, e.item);
        }
        g.disableScissor();
        if (inView(mx, my) || dragging)
            line(g, font, Component.literal("Glisser pour tourner"), rx + 6, bodyTop + viewH - 10, rw - 12, 0.6f, 0.6f, MUTED, false);
    }
}
