package com.minenorth_shops;

import com.minenorth_shops.shop.Shop;
import fr.minenorth.api.CompanyService;
import fr.minenorth.api.MineNorth;
import fr.minenorth.api.StateService;
import net.minecraft.server.level.ServerPlayer;

/**
 * Boutiques réservées à la mairie ou à une entreprise (services fournis par les mods État et Entreprises via MineNorth API).
 * Sécurité : si le service est absent, la boutique est BLOQUÉE (comme pour la police et les pompiers) ; les OP passent toujours.
 */
public final class AccessCompat {
    private AccessCompat() {}

    public static boolean townHallAvailable() { return MineNorth.state() != StateService.NONE; }

    public static boolean companiesAvailable() { return MineNorth.companies() != CompanyService.NONE; }

    /** Convertit le nom d'entreprise écrit dans shops.json en identifiant (dès que l'entreprise est connue). */
    public static void resolve(net.minecraft.server.MinecraftServer server, Shop shop) {
        if (shop.companyId != 0 || shop.companyName.isBlank() || !companiesAvailable()) return;
        int id = MineNorth.companies().idOf(server, shop.companyName);
        if (id <= 0) return;
        shop.companyId = id;
        shop.mairie = false;
        shop.pompier = false;
        shop.policeGrade = -1;
        com.minenorth_shops.shop.ShopData.get(server).changed();
    }

    public static void resolveAll(net.minecraft.server.MinecraftServer server) {
        for (Shop s : com.minenorth_shops.shop.ShopData.get(server).all()) resolve(server, s);
    }

    /** Message de refus, ou null si le joueur peut commercer ici. */
    public static String denial(ServerPlayer p, Shop shop) {
        resolve(p.server, shop);
        if (shop.companyId == 0 && !shop.companyName.isBlank() && !shop.mairie) {
            if (p.hasPermissions(2)) return null;
            if (!companiesAvailable()) return "Boutique indisponible : réservée à une entreprise mais le mod Entreprises est absent.";
            return "Cette boutique est réservée à l'entreprise « " + shop.companyName + " » (introuvable ou non active).";
        }
        if (shop.mairie) {
            if (p.hasPermissions(2)) return null;
            if (!townHallAvailable()) return "Boutique indisponible : réservée à la mairie mais le mod État est absent.";
            if (!MineNorth.state().isTownHall(p)) return "Cette boutique est réservée à la mairie (maire et agents).";
            return null;
        }
        if (shop.companyId != 0) {
            if (p.hasPermissions(2)) return null;
            if (shop.companyId < 0) return "Boutique non configurée : aucune entreprise n'a été choisie.";
            if (!companiesAvailable()) return "Boutique indisponible : réservée à une entreprise mais le mod Entreprises est absent.";
            String name = MineNorth.companies().name(p.server, shop.companyId);
            if (name.isEmpty()) return "Cette boutique est réservée à une entreprise qui n'existe plus.";
            if (!MineNorth.companies().isMember(p, shop.companyId, shop.companyDirection))
                return "Cette boutique est réservée " + (shop.companyDirection ? "à la direction de " : "aux employés de ") + name + ".";
        }
        return null;
    }

    /** Nom lisible de l'entreprise de la boutique (« #id » si elle n'existe plus ou si le service est absent). */
    public static String companyName(net.minecraft.server.MinecraftServer s, int id) {
        if (id <= 0) return "Aucune";
        String n = MineNorth.companies().name(s, id);
        return n.isEmpty() ? "#" + id : n;
    }
}
