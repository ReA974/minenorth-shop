package com.minenorth_shops.client.phone;

import com.minenorth_eurobank.Money;
import com.minenorth_shops.packet.PhoneShopsPacket;
import com.minenorth_shops.packet.PhoneShopsPacket.Row;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Application « Boutiques » dessinée dans l'écran du téléphone : annuaire des boutiques que le joueur peut ouvrir (magasins qui vendent,
 * boutiques qui rachètent), recherche par nom de boutique ou d'article, catalogue avec prix, distance du vendeur et guidage sur la carte.
 * Consultation seulement : on achète et on vend sur place, auprès du vendeur.
 */
public class PhoneShopsScreen extends PhoneAppBase {
    private PhoneShopsPacket.Listing data;
    /** Boutique ouverte (-1 = liste). */
    private int open = -1;
    private final Field search = new Field("Boutique ou article…", "", 32);

    public PhoneShopsScreen(PhoneShopsPacket.Listing data) {
        super("Boutiques");
        this.data = data;
    }

    public void update(PhoneShopsPacket.Listing m) { this.data = m; }

    @Override protected String title() { return "Boutiques"; }

    @Override protected String[] tabs() { return new String[]{"Magasins", "Rachat"}; }

    @Override protected void tabChanged() { open = -1; }

    @Override protected String badge() { return data.rows().size() + " boutique(s)"; }

    @Override protected int badgeColor() { return PINK; }

    @Override protected String message() { return data.message(); }

    @Override protected boolean messageError() { return !data.ok(); }

    private Row row(int id) {
        for (Row r : data.rows()) if (r.id() == id) return r;
        return null;
    }

    private static String distance(Row r) {
        return r.distance() >= 0 ? "à " + r.distance() + " m" : r.distance() == -1 ? "autre dimension" : "emplacement inconnu";
    }

    private static boolean matches(Row r, String q) {
        if (q.isEmpty() || r.name().toLowerCase(Locale.ROOT).contains(q)) return true;
        for (PhoneShopsPacket.Item i : r.items()) if (i.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) return true;
        return false;
    }

    @Override
    protected float content(GuiGraphics g, int mx, int my, float y) {
        Row r = open >= 0 ? row(open) : null;
        return r != null ? detail(g, mx, my, y, r) : list(g, mx, my, y);
    }

    // ------------------------------------------------------------------ liste

    private float list(GuiGraphics g, int mx, int my, float y) {
        open = -1;
        field(g, mx, my, search, L, y, R - 92);
        button(g, mx, my, R - 86, y, R, y + 22, "Actualiser", 9, DARK, true, () -> PhoneShopsApp.send(PhoneShopsPacket.LIST, 0));
        y += 30;
        boolean buy = tab == 1;
        String q = search.value.trim().toLowerCase(Locale.ROOT);
        List<Row> shown = new ArrayList<>();
        for (Row r : data.rows()) if (r.buy() == buy && matches(r, q)) shown.add(r);
        if (shown.isEmpty()) return wrap(g, !q.isEmpty() ? "Aucune boutique pour cette recherche." : buy ? "Aucune boutique ne rachète d'objets pour le moment." : "Aucun magasin pour le moment.",
                L, y, 9.5f, R - L, MUTED);
        for (Row r : shown) {
            boolean hov = hover(mx, my, L, y, R, y + 46);
            box(g, L, y, R, y + 46, 12, hov ? HOVER : CARD);
            box(g, L + 3, y + 7, L + 6, y + 39, 1.5f, buy ? GREEN : PINK);
            text(g, r.name(), L + 14, y + 8, 10.5f, R - L - 110, WHITE);
            right(g, distance(r), R - 10, y + 9, 8.5f, r.distance() >= 0 ? CYAN : MUTED);
            String info = r.items().size() + " article(s)" + (r.access().isEmpty() ? "" : " · " + r.access()) + (r.licence().isEmpty() ? "" : " · licence " + r.licence());
            text(g, info, L + 14, y + 26, 8.5f, R - L - 28, r.access().isEmpty() && r.licence().isEmpty() ? MUTED : WARN);
            final int id = r.id();
            hit(L, y, R, y + 46, () -> { open = id; resetScroll(); });
            y += 51;
        }
        return y;
    }

    // ------------------------------------------------------------------ catalogue d'une boutique

    private float detail(GuiGraphics g, int mx, int my, float y, Row r) {
        button(g, mx, my, L, y, L + 70, y + 22, "Retour", 9, HOVER, true, () -> { open = -1; resetScroll(); });
        text(g, r.name(), L + 78, y + 6, 11, R - L - 78, WHITE);
        y += 30;
        box(g, L, y, R, y + 58, 14, CARD);
        String pay = r.buy() ? "Rachète vos objets" + (r.card() ? " · paiement sur le compte" : r.cash() ? " · paiement en espèces" : "")
                : "Paiement : " + (r.cash() && r.card() ? "espèces ou carte" : r.card() ? "carte" : r.cash() ? "espèces" : "—");
        text(g, pay, L + 12, y + 8, 9, R - L - 24, TEXT);
        text(g, r.place().isEmpty() ? "Vendeur introuvable (zone non chargée)" : r.place() + " · " + distance(r), L + 12, y + 22, 8.5f, R - L - 24, MUTED);
        if (!r.licence().isEmpty() || !r.access().isEmpty())
            text(g, (r.access().isEmpty() ? "" : "Réservée : " + r.access() + "  ") + (r.licence().isEmpty() ? "" : "Licence : " + r.licence()), L + 12, y + 38, 8.5f, R - L - 24, WARN);
        y += 64;
        final int id = r.id();
        button(g, mx, my, L, y, R, y + 24, "Itinéraire sur la carte", 9.5f, CYAN, !r.place().isEmpty(), () -> PhoneShopsApp.send(PhoneShopsPacket.GUIDE, id));
        y += 32;
        y = wrap(g, r.buy() ? "Venez vendre sur place, auprès du vendeur." : "Achat sur place, auprès du vendeur.", L, y, 8.5f, R - L, MUTED) + 6;
        if (r.items().isEmpty()) return wrap(g, "Aucun article pour le moment.", L, y, 9.5f, R - L, MUTED);
        boolean cats = r.items().stream().anyMatch(i -> !i.category().isEmpty());
        String cat = null;
        for (PhoneShopsPacket.Item it : r.items()) {
            if (cats && !it.category().equals(cat)) {
                text(g, it.category().isEmpty() ? "AUTRES" : it.category().toUpperCase(Locale.ROOT), L, y + 4, 9, R - L, CYAN);
                y += 20;
            }
            cat = it.category();
            box(g, L, y, R, y + 32, 10, CARD);
            if (visible(y, y + 32)) {
                float s = size(18) / 16f;
                g.pose().pushPose();
                g.pose().translate(x(L + 8), y(y + 7), 0);
                g.pose().scale(s, s, 1);
                g.renderItem(it.stack(), 0, 0);
                g.pose().popPose();
            }
            text(g, it.stack().getHoverName().getString(), L + 34, y + 6, 9.5f, R - L - 120, WHITE);
            text(g, "lot de " + it.qty(), L + 34, y + 19, 8, 100, MUTED);
            right(g, Money.format(it.price()), R - 10, y + 11, 10, r.buy() ? OK : WARN);
            y += 36;
        }
        return y;
    }
}
