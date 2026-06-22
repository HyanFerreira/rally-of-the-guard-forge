package net.hfstack.rallyguard.mixin.client;

import net.hfstack.rallyguard.util.GuardVillagersConfigPatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tallestegg.guardvillagers.client.gui.GuardInventoryScreen;

@Mixin(GuardInventoryScreen.class)
public abstract class GuardInventoryScreenMixin {
    @Inject(method = "init()V", at = @At("HEAD"))
    private void rallyguard$allowFollowWithoutHero(CallbackInfo ci) {
        GuardVillagersConfigPatcher.forceRuntimeFollowHeroFlag();
    }
}
