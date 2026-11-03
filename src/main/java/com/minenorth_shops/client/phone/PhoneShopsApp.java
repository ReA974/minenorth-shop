package com.minenorth_shops.client.phone;

import com.mikasrevs.newphone.client.zphone.PhoneNavigationState;
import com.minenorth_shops.Network;
import com.minenorth_shops.packet.PhoneShopsPacket;
import net.minecraft.client.Minecraft;

/**
 * Seul point d'entrée vers {@link PhoneShopsScreen} (qui hérite d'une classe du téléphone MikasRevs Phone 2). Ne JAMAIS toucher cette classe sans
 * avoir vérifié {@code PhoneShops.phone()} : sa vérification par la JVM charge PhoneShopsScreen, donc la classe du téléphone. Côté client uniquement.
 */
public final class PhoneShopsApp {
    /** Ouverture demandée depuis le téléphone : la prochaine liste ouvre l'application dans le téléphone (pendant 5 s). */
    private static long pendingUntil;

    private PhoneShopsApp() {}

    public static void request() {
        pendingUntil = System.currentTimeMillis() + 5000;
        send(PhoneShopsPacket.LIST, 0);
    }

    static void send(int action, int shopId) { Network.CHANNEL.sendToServer(new PhoneShopsPacket.Request(action, shopId)); }

    public static void handle(PhoneShopsPacket.Listing m) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PhoneShopsScreen ps) {
            ps.update(m);
            return;
        }
        if (System.currentTimeMillis() < pendingUntil && mc.player != null) {
            pendingUntil = 0;
            PhoneNavigationState.open(mc, mc.screen, new PhoneShopsScreen(m));
        }
    }
}
