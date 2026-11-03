package com.minenorth_shops;

import com.minenorth_shops.packet.PhoneShopsPacket;
import com.minenorth_shops.shop.Shop;
import com.minenorth_shops.shop.ShopData;
import com.minenorth_shops.shop.ShopEntry;
import com.minenorth_shops.shop.ShopMode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Annuaire des boutiques de l'application « Boutiques » du téléphone (côté serveur). Seulement les boutiques que le joueur peut ouvrir :
 * jamais les boutiques illégales ni les boutiques d'entreprise, ni celles réservées à un métier qu'il n'a pas. Consultation seulement : achat et vente se font sur place.
 * Emplacement = vendeur lié trouvé parmi les entités chargées ; guidage par le mod carte (minenorth_map, par réflexion, facultatif).
 */
public final class PhoneDirectory {
    private static final Map<UUID, Long> LAST = new ConcurrentHashMap<>();
    private static boolean mapTried;
    private static Method setMarker;

    private PhoneDirectory() {}

    private record Spot(String dim, BlockPos pos) {}

    public static void handle(ServerPlayer p, int action, int shopId) {
        long now = System.currentTimeMillis();
        if (now - LAST.getOrDefault(p.getUUID(), 0L) < 500) return;   // anti-martelage
        LAST.put(p.getUUID(), now);
        Map<Integer, Spot> spots = spots(p);
        String msg = "";
        boolean ok = true;
        if (action == PhoneShopsPacket.GUIDE) {
            Shop shop = ShopData.get(p.server).shop(shopId);
            Spot spot = spots.get(shopId);
            if (shop == null || !visible(p, shop)) { msg = "Boutique introuvable."; ok = false; }
            else if (spot == null) { msg = "Vendeur introuvable pour le moment (zone non chargée)."; ok = false; }
            else if (!guide(p, shop.name, shop.mode == ShopMode.BUY, spot)) { msg = "Guidage indisponible : la carte n'est pas installée."; ok = false; }
            else msg = "Guidage activé vers « " + shop.name + " ».";
        }
        List<PhoneShopsPacket.Row> rows = new ArrayList<>();
        for (Shop shop : ShopData.get(p.server).all()) {
            if (!visible(p, shop)) continue;
            List<PhoneShopsPacket.Item> items = new ArrayList<>();
            for (ShopEntry e : shop.entries) items.add(new PhoneShopsPacket.Item(e.item.copy(), e.quantity, e.price, e.category));
            Spot spot = spots.get(shop.id);
            int distance = -2;
            String place = "";
            if (spot != null) {
                place = spot.pos.getX() + " " + spot.pos.getY() + " " + spot.pos.getZ();
                distance = spot.dim.equals(p.level().dimension().location().toString())
                        ? (int) Math.round(Math.sqrt(spot.pos.distToCenterSqr(p.position()))) : -1;
            }
            rows.add(new PhoneShopsPacket.Row(shop.id, shop.name, shop.mode == ShopMode.BUY, shop.accessLabel(), shop.licence == null ? "" : shop.licence,
                    shop.allowCash, shop.allowCard, place, distance, items));
        }
        rows.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        Network.send(p, new PhoneShopsPacket.Listing(rows, msg, ok));
    }

    /** Boutique visible dans l'annuaire : légale et ouvrable par ce joueur (mêmes règles d'accès que l'écran de la boutique). */
    private static boolean visible(ServerPlayer p, Shop shop) {
        if (hidden(shop)) return false;
        String denied = PompierCompat.denial(p, shop);
        if (denied == null) denied = PoliceCompat.denial(p, shop);
        if (denied == null) denied = AccessCompat.denial(p, shop);
        return denied == null;
    }

    /** Ni marché noir, ni boutiques d'entreprise (gérées par les joueurs, hors de l'annuaire public), même pour leurs membres. */
    private static boolean hidden(Shop shop) {
        return shop.illegal || shop.companyId != 0 || (shop.companyName != null && !shop.companyName.isBlank());
    }

    /** Premier vendeur chargé de chaque boutique (toutes dimensions). */
    private static Map<Integer, Spot> spots(ServerPlayer p) { return spots(p.server); }

    private static Map<Integer, Spot> spots(net.minecraft.server.MinecraftServer server) {
        Map<Integer, Spot> out = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            String dim = level.dimension().location().toString();
            for (Entity e : level.getAllEntities()) {
                if (e == null || !e.isAlive()) continue;
                int id = EntityLinks.shopOf(e);
                if (id >= 0) out.putIfAbsent(id, new Spot(dim, e.blockPosition()));
            }
        }
        return out;
    }

    /**
     * Boutiques publiques pour la carte (mod minenorth_map, ShopWaypoints, par réflexion ; ne pas changer le nom ni le format) : une ligne
     * « id|rachat(0/1)|dimension|x|y|z|nom » par boutique de l'annuaire ouverte à tous (ni illégale, ni d'entreprise, ni réservée à un métier).
     * Dimension vide = vendeur introuvable (zone non chargée) : la carte garde le point où il était.
     */
    public static List<String> mapShops(net.minecraft.server.MinecraftServer server) {
        Map<Integer, Spot> spots = spots(server);
        List<String> out = new ArrayList<>();
        for (Shop shop : ShopData.get(server).all()) {
            if (hidden(shop) || !shop.accessLabel().isEmpty()) continue;
            Spot spot = spots.get(shop.id);
            String where = spot == null ? "|||" : spot.dim + "|" + spot.pos.getX() + "|" + spot.pos.getY() + "|" + spot.pos.getZ();
            out.add(shop.id + "|" + (shop.mode == ShopMode.BUY ? 1 : 0) + "|" + where + "|" + shop.name);
        }
        return out;
    }

    /** Repère + guidage sur la carte du joueur (mod minenorth_map, par réflexion). false si la carte est absente. */
    private static boolean guide(ServerPlayer p, String name, boolean buy, Spot spot) {
        if (!mapTried) {
            mapTried = true;
            try {
                if (ModList.get().isLoaded("minenorth_map")) setMarker = Class.forName("fr.minenorth.map.api.MapApi").getMethod("setMarker",
                        ServerPlayer.class, String.class, String.class, int.class, int.class, int.class, int.class, boolean.class, String.class);
            } catch (Throwable t) {
                setMarker = null;
            }
        }
        if (setMarker == null) return false;
        try {
            // même icône que le point de la boutique sur la carte (sac framboise, ou pastille dorée « rachat »)
            setMarker.invoke(null, p, "Boutique : " + name, spot.dim, spot.pos.getX(), spot.pos.getY(), spot.pos.getZ(), buy ? 0xC79100 : 0xE0457B, true,
                    buy ? "rachat" : "boutiques");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void forget(UUID id) { LAST.remove(id); }
}
