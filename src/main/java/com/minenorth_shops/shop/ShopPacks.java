package com.minenorth_shops.shop;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Packs de boutiques prêtes à l'emploi ({@code config/minenorth_shops/packs/*.json}, même format que shops.json, ids ignorés).
 * L'import ne détruit rien : une boutique absente (comparée par nom) est créée ; une boutique du même nom reçoit seulement
 * les articles qui lui manquent (même objet + même NBT), sans toucher à ses prix ni à ses réglages.
 * Les packs fournis avec le mod sont copiés dans ce dossier s'ils n'y sont pas (jamais écrasés).
 */
public final class ShopPacks {
    private ShopPacks() {}

    private static final Gson GSON = new Gson();
    /** Packs fournis dans le jar (src/main/resources/shop_packs/). */
    private static final String[] BUNDLED = {"boutiques-minenorth.json"};

    public record Result(List<String> created, List<String> completed, int added, int unknown, int full) {}

    public static Path dir() {
        return FMLPaths.CONFIGDIR.get().resolve("minenorth_shops").resolve("packs");
    }

    /** Noms des packs disponibles (sans .json), après copie des packs fournis manquants. */
    public static List<String> list() {
        extractBundled();
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir())) {
            s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json")).sorted()
                    .forEach(n -> out.add(n.substring(0, n.length() - 5)));
        } catch (IOException ignored) {
        }
        return out;
    }

    private static void extractBundled() {
        try {
            Files.createDirectories(dir());
            for (String n : BUNDLED) {
                Path f = dir().resolve(n);
                if (Files.exists(f)) continue;
                try (InputStream in = ShopPacks.class.getResourceAsStream("/shop_packs/" + n)) {
                    if (in != null) Files.copy(in, f);
                }
            }
        } catch (IOException e) {
            ShopData.LOGGER.error("[Boutiques] Copie des packs impossible", e);
        }
    }

    /** @throws IOException pack introuvable ou illisible (JSON invalide compris). */
    public static Result importPack(ShopData data, String name) throws IOException {
        extractBundled();
        String file = name.endsWith(".json") ? name : name + ".json";
        Path f = dir().resolve(file).normalize();
        if (!f.startsWith(dir()) || !Files.isRegularFile(f)) throw new IOException("pack introuvable : " + file);
        JsonObject root;
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            root = GSON.fromJson(r, JsonObject.class);
        } catch (RuntimeException e) {
            throw new IOException("JSON invalide : " + e.getMessage());
        }
        if (root == null || !root.has("shops")) throw new IOException("pas de liste \"shops\" dans " + file);

        List<String> created = new ArrayList<>(), completed = new ArrayList<>();
        int added = 0, unknown = 0, full = 0;
        for (JsonElement el : root.getAsJsonArray("shops")) {
            JsonObject o = el.getAsJsonObject().deepCopy();
            o.addProperty("id", 0);
            JsonArray raw = o.has("entries") ? o.getAsJsonArray("entries") : new JsonArray();
            Shop pack = Shop.fromJson(o);
            unknown += raw.size() - pack.entries.size();   // objets d'un mod absent : ignorés

            Shop target = byName(data, pack.name);
            boolean isNew = target == null;
            if (isNew) {
                target = data.create(pack.name);
                copySettings(pack, target);
            }
            int n = 0;
            for (ShopEntry e : pack.entries) {
                if (!isNew && has(target, e.item)) continue;
                if (target.entries.size() >= Shop.MAX_ENTRIES) { full++; continue; }
                target.addEntry(e.item, e.quantity, e.price).category = e.category;
                n++;
            }
            added += n;
            if (isNew) created.add("#" + target.id + " " + target.name);
            else if (n > 0) completed.add("#" + target.id + " " + target.name + " (+" + n + ")");
        }
        data.changed();
        return new Result(created, completed, added, unknown, full);
    }

    private static Shop byName(ShopData data, String name) {
        String k = name.trim().toLowerCase(Locale.ROOT);
        for (Shop s : data.all()) if (s.name.trim().toLowerCase(Locale.ROOT).equals(k)) return s;
        return null;
    }

    private static boolean has(Shop s, ItemStack it) {
        for (ShopEntry e : s.entries) if (ItemStack.isSameItemSameTags(e.item, it)) return true;
        return false;
    }

    private static void copySettings(Shop from, Shop to) {
        to.mode = from.mode;
        to.allowCash = from.allowCash;
        to.allowCard = from.allowCard;
        to.licence = from.licence;
        to.policeGrade = from.policeGrade;
        to.pompier = from.pompier;
        to.mairie = from.mairie;
        to.companyId = from.companyId;
        to.companyName = from.companyName;
        to.companyDirection = from.companyDirection;
        to.illegal = from.illegal;
        to.illegalMinutes = from.illegalMinutes;
        to.illegalRadius = from.illegalRadius;
    }
}
