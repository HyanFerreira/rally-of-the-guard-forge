package net.hfstack.rallyguard.screen;

import net.hfstack.rallyguard.RallyOfTheGuard;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    private ModMenus() {
    }

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, RallyOfTheGuard.MOD_ID);

    // O ID precisa bater com o que apareceu no crash: rallyguard:hire_menu
    public static final RegistryObject<MenuType<HireGuardMenu>> HIRE_MENU =
            MENUS.register("hire_menu",
                    // ESCOLHA UMA das duas linhas abaixo conforme o construtor do seu menu:
                    // (a) se o construtor NÃO usa FriendlyByteBuf:
                    // () -> IForgeMenuType.create(HireGuardMenu::new)
                    //
                    // (b) se o construtor usa FriendlyByteBuf (windowId, inv, buf):
                    () -> IForgeMenuType.create((windowId, inv, buf) -> new HireGuardMenu(windowId, inv, buf))
            );

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
