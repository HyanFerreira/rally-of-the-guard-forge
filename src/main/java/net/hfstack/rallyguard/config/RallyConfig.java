package net.hfstack.rallyguard.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RallyConfig {
    private RallyConfig() {
    }

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.IntValue PRICE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        PRICE = b.comment("Emerald cost to hire a guard")
                .defineInRange("price", 3, 0, 64);

        COMMON_SPEC = b.build();
    }

    public static int price() {
        return PRICE.get();
    }
}
