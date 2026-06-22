package net.hfstack.rallyguard;

import com.mojang.logging.LogUtils;
import net.hfstack.rallyguard.effect.ModEffects;
import net.hfstack.rallyguard.event.HiredGuardsNeutralityHandler;
import net.hfstack.rallyguard.event.InteractGuardHandler;
import net.hfstack.rallyguard.event.RallyFriendlyFireHandler;
import net.hfstack.rallyguard.item.ModItems;
import net.hfstack.rallyguard.network.GuardCommandNetworking;
import net.hfstack.rallyguard.network.NetworkBootstrap;
import net.hfstack.rallyguard.screen.ModMenus;
import net.hfstack.rallyguard.util.GuardVillagersConfigPatcher;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(RallyOfTheGuard.MOD_ID)
public final class RallyOfTheGuard {
    public static final String MOD_ID = "rallyguard";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RallyOfTheGuard() {
        // Bus de eventos do MOD (registries + lifecycle)
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModMenus.register(modBus);

        // ==== REGISTRIES (Forge usa DeferredRegister) ====
        // Obs: nos próximos passos vamos adaptar seus ModItems/ModEffects para DeferredRegister.
        ModItems.REGISTER.register(modBus);
        ModEffects.REGISTER.register(modBus);
        // ModComponents: em Forge isso vira Capability/Attachment. Vamos tratar mais à frente.

        // Lifecycle
        modBus.addListener(this::commonSetup);
        modBus.addListener(ModItems::registerCreativeTabContents);
        modBus.addListener(this::onConfigLoading);
        modBus.addListener(this::onConfigReloading);

        // ==== EVENTOS DE JOGO (Forge EVENT BUS) ====
        MinecraftForge.EVENT_BUS.register(new InteractGuardHandler());
        MinecraftForge.EVENT_BUS.register(new RallyFriendlyFireHandler());
        MinecraftForge.EVENT_BUS.register(new HiredGuardsNeutralityHandler());

        // Ajuste de config do Guard Villagers (executa logo na inicialização do mod)
        GuardVillagersConfigPatcher.patchFollowHeroConfig();

        LOGGER.info("[{}] carregado (Forge 1.20.1).", MOD_ID);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Tarefas que precisam rodar no thread de trabalho do FML
        event.enqueueWork(() -> {
            // Registra codecs/tipos uma única vez (client e server)
            NetworkBootstrap.registerTypesOnce();
            // Registra apenas os handlers/receivers do lado do servidor
            GuardCommandNetworking.registerServer();
        });
    }

    private void onConfigLoading(final ModConfigEvent.Loading event) {
        if ("guardvillagers".equals(event.getConfig().getModId())) {
            GuardVillagersConfigPatcher.patchFollowHeroConfig();
        }
    }

    private void onConfigReloading(final ModConfigEvent.Reloading event) {
        if ("guardvillagers".equals(event.getConfig().getModId())) {
            GuardVillagersConfigPatcher.patchFollowHeroConfig();
        }
    }
}
