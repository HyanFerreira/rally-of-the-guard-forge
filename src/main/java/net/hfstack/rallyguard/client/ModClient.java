package net.hfstack.rallyguard.client;

import net.hfstack.rallyguard.RallyOfTheGuard;
import net.hfstack.rallyguard.screen.HireGuardScreen;
import net.hfstack.rallyguard.screen.ModMenus;
import net.hfstack.rallyguard.util.GuardVillagersConfigPatcher;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = RallyOfTheGuard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClient {
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.HIRE_MENU.get(), HireGuardScreen::new);
            GuardVillagersConfigPatcher.forceRuntimeFollowHeroFlag();
        });
    }

    @Mod.EventBusSubscriber(modid = RallyOfTheGuard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeClientEvents {
        private static int ticksToForce = 200;

        private ForgeClientEvents() {
        }

        @SubscribeEvent
        public static void onClientTick(final TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END && ticksToForce-- > 0) {
                GuardVillagersConfigPatcher.forceRuntimeFollowHeroFlag();
            }
        }

        @SubscribeEvent
        public static void onScreenInit(final ScreenEvent.Init.Pre event) {
            GuardVillagersConfigPatcher.forceRuntimeFollowHeroFlag();
        }
    }
}

