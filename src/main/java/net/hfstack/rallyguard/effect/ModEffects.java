package net.hfstack.rallyguard.effect;

import net.hfstack.rallyguard.RallyOfTheGuard;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> REGISTER =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RallyOfTheGuard.MOD_ID);

    public static final RegistryObject<MobEffect> RALLY_COMMANDER =
            REGISTER.register("rally_commander", RallyCommanderEffect::new);

    private ModEffects() {
    }
}
