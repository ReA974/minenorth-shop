package com.minenorth_shops;

import com.minenorth_shops.items.ModItems;
import com.minenorth_shops.shop.ShopData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod(MineNorthShops.MODID)
public class MineNorthShops {
    public static final String MODID = "minenorth_shops";

    private int tickCounter;

    public MineNorthShops() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::creativeTabs);
        // COMMON (dossier config/) et non SERVER (<monde>/serverconfig/, remis à zéro à chaque redémarrage) ; serveur dédié seulement : le client ne crée aucun fichier.
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ShopConfig.SPEC, "minenorth_shops-server.toml");
        }
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new EntityLinks());
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        e.enqueueWork(Network::register);
    }

    private void creativeTabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.OP_BLOCKS && e.hasPermissions()) e.accept(ModItems.LINKER);
    }

    @SubscribeEvent
    public void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent e) {
        AccessCompat.resolveAll(e.getServer());   // noms d'entreprise de shops.json -> identifiants
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent e) {
        ShopCommands.register(e.getDispatcher());
    }

    /** Pousse les mises à jour : immédiatement si une boutique a changé, sinon chaque seconde (espèces, solde, inventaire). */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        tickCounter++;
        boolean changed = ShopData.get(server).consumeChanged();
        if (changed || tickCounter % 20 == 0) Network.refresh(server, changed);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        Network.forget(e.getEntity().getUUID());
        EntityLinks.forget(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent e) {
        Network.SESSIONS.remove(e.getEntity().getUUID());
    }
}
