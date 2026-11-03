package com.minenorth_shops.client;

import com.minenorth_shops.packet.PhoneShopsPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Application « Boutiques » du téléphone MRPhone patché (appelée par réflexion par le jar patché, JobsHook) : visible pour tous les joueurs,
 * ouvre dans l'écran du téléphone l'annuaire des boutiques ({@code phone.PhoneShopsScreen}). Les données viennent du serveur.
 */
@OnlyIn(Dist.CLIENT)
public final class PhoneShops {
    private PhoneShops() {}

    public static boolean visible() { return true; }

    /** Téléphone MikasRevs Phone 2 présent : à tester AVANT de toucher le paquet client.phone. */
    public static boolean phone() { return net.minecraftforge.fml.ModList.get().isLoaded("newphone"); }

    /** Clic sur l'icône : on garde le téléphone ouvert et on demande la liste au serveur ; la réponse ouvre l'application. */
    public static void openFromPhone() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !phone()) return;
        com.minenorth_shops.client.phone.PhoneShopsApp.request();
    }

    /** Réponse du serveur (thread client). */
    public static void receive(PhoneShopsPacket.Listing m) {
        if (phone()) com.minenorth_shops.client.phone.PhoneShopsApp.handle(m);
    }
}
