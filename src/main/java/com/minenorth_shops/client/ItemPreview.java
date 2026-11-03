package com.minenorth_shops.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.lang.reflect.Method;

/**
 * Aperçu 3D de l'article choisi : un vêtement (armure ou accessoire du mod Vêtements) est porté par le joueur local,
 * équipé le temps d'une image seulement côté client ; tout autre objet (armes...) est affiché agrandi et tourne.
 */
@OnlyIn(Dist.CLIENT)
public final class ItemPreview {
    private ItemPreview() {}

    /** Ce que l'aperçu fait de l'objet : le porter (pièce d'armure / accessoire) ou l'afficher seul. */
    public record Wear(EquipmentSlot armor, int accessory) {}

    /** null si l'objet ne se porte pas. */
    public static Wear wear(ItemStack st) {
        if (st.isEmpty() || Minecraft.getInstance().player == null) return null;
        int acc = Vetements.slot(st);
        if (acc >= 0) return new Wear(null, acc);
        EquipmentSlot slot = Mob.getEquipmentSlotForItem(st);
        return slot.getType() == EquipmentSlot.Type.ARMOR ? new Wear(slot, -1) : null;
    }

    /** Joueur local qui porte la pièce, pieds en (cx, feetY), tourné de yaw degrés. */
    public static void player(GuiGraphics g, int cx, int feetY, int scale, float yaw, ItemStack st, Wear w) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        ItemStack oldArmor = w.armor != null ? p.getInventory().armor.get(w.armor.getIndex()) : null;
        ItemStack[] oldAcc = w.accessory >= 0 ? Vetements.worn(p.getId()) : null;
        float body = p.yBodyRot, yRot = p.getYRot(), xRot = p.getXRot(), headO = p.yHeadRotO, head = p.yHeadRot;
        try {
            if (w.armor != null) p.getInventory().armor.set(w.armor.getIndex(), st);
            if (oldAcc != null) {
                ItemStack[] now = new ItemStack[Vetements.n];
                for (int i = 0; i < now.length; i++) now[i] = i < oldAcc.length ? oldAcc[i] : ItemStack.EMPTY;
                now[w.accessory] = st;
                Vetements.put(p.getId(), now);
            }
            p.yBodyRot = 180f;
            p.setYRot(180f);
            p.setXRot(0f);
            p.yHeadRot = 180f;
            p.yHeadRotO = 180f;
            Quaternionf tilt = Axis.XP.rotationDegrees(-8f);
            Quaternionf pose = Axis.ZP.rotationDegrees(180f).mul(tilt).mul(Axis.YP.rotationDegrees(yaw));
            InventoryScreen.renderEntityInInventory(g, cx, feetY, scale, pose, tilt, p);
        } finally {
            if (w.armor != null) p.getInventory().armor.set(w.armor.getIndex(), oldArmor);
            if (oldAcc != null) Vetements.put(p.getId(), oldAcc.length == Vetements.n ? oldAcc : empty());
            p.yBodyRot = body;
            p.setYRot(yRot);
            p.setXRot(xRot);
            p.yHeadRotO = headO;
            p.yHeadRot = head;
        }
    }

    private static ItemStack[] empty() {
        ItemStack[] a = new ItemStack[Vetements.n];
        java.util.Arrays.fill(a, ItemStack.EMPTY);
        return a;
    }

    /** Objet agrandi (rendu « posé dans un cadre », en relief), centré en (cx, cy), tourné de yaw degrés. */
    public static void item(GuiGraphics g, int cx, int cy, float size, float yaw, ItemStack st) {
        Minecraft mc = Minecraft.getInstance();
        g.pose().pushPose();
        g.pose().translate(cx, cy, 150);
        g.pose().scale(size, -size, size);
        g.pose().mulPose(Axis.XP.rotationDegrees(-12f));
        g.pose().mulPose(Axis.YP.rotationDegrees(yaw));
        Lighting.setupFor3DItems();
        mc.getItemRenderer().renderStatic(st, ItemDisplayContext.FIXED, 0xF000F0, OverlayTexture.NO_OVERLAY, g.pose(),
                g.bufferSource(), mc.level, 0);
        g.flush();
        g.pose().popPose();
    }

    /** Accessoires du mod minenorth-vetements (dépendance facultative : tout passe par réflexion). */
    private static final class Vetements {
        static Method slotOf, get, set;
        static int n;
        static boolean ok;

        static {
            try {
                Class<?> acc = Class.forName("com.minenorth_vetements.Accessories");
                slotOf = acc.getMethod("slotOf", ItemStack.class);
                n = acc.getField("N").getInt(null);
                Class<?> cl = Class.forName("com.minenorth_vetements.client.AccessoryClient");
                get = cl.getMethod("get", int.class);
                set = cl.getMethod("set", int.class, ItemStack[].class);
                ok = true;
            } catch (Throwable t) {
                ok = false;
            }
        }

        /** Emplacement d'accessoire (0..n-1), -1 si ce n'est pas un accessoire. */
        static int slot(ItemStack st) {
            if (!ok) return -1;
            try {
                Object s = slotOf.invoke(null, st);
                return s instanceof Enum<?> e ? e.ordinal() : -1;
            } catch (Throwable t) {
                return -1;
            }
        }

        static ItemStack[] worn(int id) {
            try {
                return ((ItemStack[]) get.invoke(null, id)).clone();
            } catch (Throwable t) {
                return new ItemStack[0];
            }
        }

        static void put(int id, ItemStack[] st) {
            try {
                set.invoke(null, id, st);
            } catch (Throwable ignored) {
            }
        }
    }
}
