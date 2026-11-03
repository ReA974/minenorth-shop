package com.minenorth_shops.packet;

import com.minenorth_shops.PhoneDirectory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Application « Boutiques » du téléphone MRPhone patché : annuaire des boutiques (consultation seulement, on achète sur place auprès du vendeur).
 * {@link Request} : client -> serveur (liste, ou guidage sur la carte vers une boutique) ; {@link Listing} : serveur -> client.
 */
public final class PhoneShopsPacket {
    private PhoneShopsPacket() {}

    public static final int LIST = 0, GUIDE = 1;

    /** Client -> serveur : action LIST (shopId ignoré) ou GUIDE (repère + guidage vers le vendeur de la boutique shopId). */
    public record Request(int action, int shopId) {
        public static void encode(Request m, FriendlyByteBuf b) { b.writeVarInt(m.action); b.writeVarInt(m.shopId); }
        public static Request decode(FriendlyByteBuf b) { return new Request(b.readVarInt(), b.readVarInt()); }
        public static void handle(Request m, Supplier<NetworkEvent.Context> sup) {
            NetworkEvent.Context ctx = sup.get();
            ctx.enqueueWork(() -> {
                ServerPlayer p = ctx.getSender();
                if (p != null) PhoneDirectory.handle(p, m.action, m.shopId);
            });
            ctx.setPacketHandled(true);
        }
    }

    /** Article tel qu'affiché dans l'annuaire. */
    public record Item(ItemStack stack, int qty, long price, String category) {
        static void encode(FriendlyByteBuf b, Item i) { b.writeItem(i.stack); b.writeVarInt(i.qty); b.writeVarLong(i.price); b.writeUtf(i.category, 32); }
        static Item decode(FriendlyByteBuf b) { return new Item(b.readItem(), b.readVarInt(), b.readVarLong(), b.readUtf(32)); }
    }

    /**
     * Boutique de l'annuaire. buy = rachète aux joueurs ; access = accès réservé (« Police »…, vide = tous) ; distance : mètres jusqu'au vendeur,
     * -1 = autre dimension, -2 = vendeur introuvable (zone non chargée).
     */
    public record Row(int id, String name, boolean buy, String access, String licence, boolean cash, boolean card, String place, int distance,
                      List<Item> items) {
        static void encode(FriendlyByteBuf b, Row r) {
            b.writeVarInt(r.id); b.writeUtf(r.name, 64); b.writeBoolean(r.buy); b.writeUtf(r.access, 32); b.writeUtf(r.licence, 64);
            b.writeBoolean(r.cash); b.writeBoolean(r.card); b.writeUtf(r.place, 96); b.writeInt(r.distance);
            b.writeCollection(r.items, Item::encode);
        }
        static Row decode(FriendlyByteBuf b) {
            return new Row(b.readVarInt(), b.readUtf(64), b.readBoolean(), b.readUtf(32), b.readUtf(64), b.readBoolean(), b.readBoolean(),
                    b.readUtf(96), b.readInt(), b.readList(Item::decode));
        }
    }

    /** Serveur -> client : boutiques accessibles au joueur, et message du serveur (résultat du guidage). */
    public record Listing(List<Row> rows, String message, boolean ok) {
        public static void encode(Listing m, FriendlyByteBuf b) { b.writeCollection(m.rows, Row::encode); b.writeUtf(m.message, 200); b.writeBoolean(m.ok); }
        public static Listing decode(FriendlyByteBuf b) { return new Listing(b.readList(Row::decode), b.readUtf(200), b.readBoolean()); }
        public static void handle(Listing m, Supplier<NetworkEvent.Context> sup) {
            sup.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.minenorth_shops.client.PhoneShops.receive(m)));
            sup.get().setPacketHandled(true);
        }
    }
}
