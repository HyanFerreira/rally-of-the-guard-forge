package net.hfstack.rallyguard.event;

import net.hfstack.rallyguard.contract.GuardOwnership;
import net.hfstack.rallyguard.screen.HireGuardMenu;
import net.hfstack.rallyguard.screen.ModMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;

/**
 * Se o jogador interagir (botão direito, mão principal) com um guarda SEM dono,
 * abre a tela de contratação.
 */
public final class InteractGuardHandler {

    @SubscribeEvent
    public void onInteractEntity(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        var player = event.getEntity();
        var target = event.getTarget();

        if (!GuardOwnership.isGuard(target)) return;
        if (GuardOwnership.hasOwner(target)) return; // deixa o GuardVillagers tratar

        if (!(player instanceof ServerPlayer sp)) return;

        int guardId = target.getId();
        MenuProvider provider = new SimpleMenuProvider(
                (syncId, inv, p) -> new HireGuardMenu(syncId, inv, guardId),
                Component.translatable("gui.rallyguard.hire.title")
        );

        NetworkHooks.openScreen(sp, provider, buf -> buf.writeVarInt(guardId));

        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
    }
}
