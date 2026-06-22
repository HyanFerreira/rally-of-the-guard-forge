package net.hfstack.rallyguard.client;

import net.hfstack.rallyguard.RallyOfTheGuard;
import net.hfstack.rallyguard.screen.HireGuardScreen;
import net.hfstack.rallyguard.screen.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import static net.hfstack.rallyguard.RallyOfTheGuard.MOD_ID;

@Mod.EventBusSubscriber(modid = RallyOfTheGuard.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClient {
    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.HIRE_MENU.get(), HireGuardScreen::new);
        });
    }
}

