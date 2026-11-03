package com.minenorth_shops.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * Charte graphique MineNorth (même que le garage, Permis, EuroBank : fond bleu nuit, blocs arrondis, boutons plats),
 * plus le texte qui doit tenir dans une case : réduction, retour à la ligne, coupure avec « .. ».
 */
@OnlyIn(Dist.CLIENT)
public final class MineNorthStyle {
    public static final int FRAME = 0xFF0E0E10;
    /** Fond du panneau. */
    public static final int PANEL = 0xFF161048;
    /** Fond des blocs / listes. */
    public static final int LIST = 0xFF0E0A34;
    public static final int HOVER = 0xFF2E2480;
    public static final int CYAN = 0xFF20AAEB;
    public static final int DARK = 0xFF4A3CB4;
    public static final int PINK = 0xFFC83CF0;
    public static final int TEXT = 0xFFCFE3FF;
    public static final int MUTED = 0xFF8FA8E0;
    public static final int WARN = 0xFFFFE066;
    public static final int ALERT = 0xFFFF6A9A;
    public static final int OK = 0xFF6CFF9A;
    public static final int WHITE = 0xFFFFFFFF;

    private MineNorthStyle() {}

    /** Rectangle aux coins arrondis (rayon r en pixels GUI). */
    public static void rfill(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int c) {
        r = Math.max(0, Math.min(r, Math.min(x2 - x1, y2 - y1) / 2));
        g.fill(x1, y1 + r, x2, y2 - r, c);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * (double) r - dy * dy));
            g.fill(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, c);
            g.fill(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, c);
        }
    }

    /** Bloc arrondi en dégradé vertical (vitrine de l'aperçu). */
    public static void rgradient(GuiGraphics g, int x1, int y1, int x2, int y2, int r, int top, int bottom) {
        r = Math.max(0, Math.min(r, Math.min(x2 - x1, y2 - y1) / 2));
        rfill(g, x1, y1, x2, y1 + 2 * r, r, top);
        rfill(g, x1, y2 - 2 * r, x2, y2, r, bottom);
        g.fillGradient(x1, y1 + r, x2, y2 - r, top, bottom);
    }

    /** Bloc sombre avec liseré de couleur à gauche. */
    public static void card(GuiGraphics g, int x, int y, int w, int h, int bg, int accent) {
        rfill(g, x, y, x + w, y + h, 5, accent);
        rfill(g, x + 3, y, x + w, y + h, 5, bg);
    }

    /** Pastille : fond teinté, texte de la couleur. Renvoie sa largeur. */
    public static int tag(GuiGraphics g, Font font, String text, int x, int y, int color) {
        int w = font.width(text) + 12;
        rfill(g, x, y, x + w, y + 12, 6, mix(color, LIST, 0.78f));
        g.drawString(font, text, x + 6, y + 2, color, false);
        return w;
    }

    public static Component bold(String s) {
        return Component.literal(s).withStyle(ChatFormatting.BOLD);
    }

    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gg = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bb = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | r << 16 | gg << 8 | bb;
    }

    /** Texte coupé avec « .. » s'il dépasse maxW. */
    public static String fit(Font font, String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        while (!s.isEmpty() && font.width(s + "..") > maxW) s = s.substring(0, s.length() - 1);
        return s.stripTrailing() + "..";
    }

    /** Texte à l'échelle s, coin haut-gauche en (x, y). */
    public static void scaled(GuiGraphics g, Font font, Component c, float x, float y, float s, int color) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(s, s, 1f);
        g.drawString(font, c, 0, 0, color, false);
        g.pose().popPose();
    }

    /**
     * Une ligne qui tient dans maxW : taille normale (ou maxScale), réduite jusqu'à minScale, puis coupée.
     * (x, y) = coin haut-gauche ; alignée à droite si right. Renvoie true si le texte a été coupé.
     */
    public static boolean line(GuiGraphics g, Font font, Component c, int x, int y, int maxW, float maxScale, float minScale,
                               int color, boolean right) {
        int w = Math.max(1, font.width(c));
        float s = Math.max(minScale, Math.min(maxScale, maxW / (float) w));
        boolean cut = w * s > maxW + 0.5f;
        Component shown = cut ? Component.literal(fitStyled(font, c.getString(), (int) (maxW / s), c.getStyle())).withStyle(c.getStyle()) : c;
        float dx = right ? x + maxW - font.width(shown) * s : x;
        scaled(g, font, shown, dx, y, s, color);
        return cut;
    }

    /** Libellé centré sur (cx, cy), réduit (jusqu'à 3/4) puis coupé pour tenir dans maxW. */
    public static void centered(GuiGraphics g, Font font, String text, int cx, int cy, int maxW, int color) {
        int w = Math.max(1, font.width(text));
        float s = Math.max(0.75f, Math.min(1f, maxW / (float) w));
        if (w * s > maxW) text = fit(font, text, (int) (maxW / s));
        scaled(g, font, Component.literal(text), cx - font.width(text) * s / 2f, cy - 4 * s + 0.5f, s, color);
    }

    /**
     * Bloc de texte dans une case w x h : retour à la ligne à taille normale, sinon réduit aux 3/4 (plus de lignes),
     * sinon la dernière ligne finit par « .. ». Centré verticalement. Renvoie true si le texte a été coupé.
     */
    public static boolean block(GuiGraphics g, Font font, String text, int x, int y, int w, int h, int color, boolean boldText) {
        Style style = boldText ? Style.EMPTY.withBold(true) : Style.EMPTY;
        for (float s : new float[]{1f, 0.75f}) {
            int lw = (int) (w / s);
            List<String> lines = wrap(font, text, lw, style);
            int max = Math.max(1, (int) ((h + 2 * s) / (9 * s)));
            if (lines.size() <= max || s < 1f) {
                boolean cut = lines.size() > max;
                if (cut) {   // la dernière ligne visible reprend tout le reste, coupé avec « .. »
                    String rest = String.join(" ", lines.subList(max - 1, lines.size()));
                    lines = new ArrayList<>(lines.subList(0, max));
                    lines.set(max - 1, fitStyled(font, rest, lw, style));
                }
                int n = lines.size();
                float y0 = y + (h - (n * 9 - 1) * s) / 2f;
                for (int i = 0; i < n; i++)
                    scaled(g, font, Component.literal(lines.get(i)).withStyle(style), x, y0 + i * 9 * s, s, color);
                return cut;
            }
        }
        return false;
    }

    private static String fitStyled(Font font, String s, int maxW, Style style) {
        if (font.width(Component.literal(s).withStyle(style)) <= maxW) return s;
        while (!s.isEmpty() && font.width(Component.literal(s + "..").withStyle(style)) > maxW) s = s.substring(0, s.length() - 1);
        return s.stripTrailing() + "..";
    }

    private static List<String> wrap(Font font, String text, int w, Style style) {
        List<String> out = new ArrayList<>();
        for (FormattedText t : font.getSplitter().splitLines(text, Math.max(8, w), style)) out.add(t.getString().strip());
        if (out.isEmpty()) out.add("");
        return out;
    }
}
