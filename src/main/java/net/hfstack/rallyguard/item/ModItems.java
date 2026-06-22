package net.hfstack.rallyguard.item;

import net.hfstack.rallyguard.RallyOfTheGuard;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> REGISTER =
            DeferredRegister.create(ForgeRegistries.ITEMS, RallyOfTheGuard.MOD_ID);

    public static final RegistryObject<Item> SCROLL_OF_RALLYING =
            REGISTER.register("scroll_of_rallying",
                    () -> new ScrollOfRallyingItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> COMMANDERS_LEDGER =
            REGISTER.register("commanders_ledger",
                    () -> new CommandersLedgerItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    /**
     * Adiciona itens em abas vanilla (equivalente ao ItemGroupEvents do Fabric).
     */
    public static void registerCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(SCROLL_OF_RALLYING);
            event.accept(COMMANDERS_LEDGER);
        }
    }
}
