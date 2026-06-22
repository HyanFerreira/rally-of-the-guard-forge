package net.hfstack.rallyguard.event;

import net.hfstack.rallyguard.contract.GuardOwnership;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

/**
 * Imuniza guardas do PRÓPRIO jogador enquanto estão no Rali.
 * Cobre:
 * - clique direto (AttackEntityEvent)
 * - QUALQUER dano (LivingAttackEvent): sweep, projéteis, tridente, etc.
 * <p>
 * Critério: alvo é guarda do jogador E (in_rally==true OU Following==true).
 */
public final class RallyFriendlyFireHandler {

    // 1) Clique direto com a espada/mão
    @SubscribeEvent
    public void onPlayerAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide) return;

        Player player = event.getEntity();
        Entity target = event.getTarget();

        if (!GuardOwnership.isGuard(target)) return;
        if (!GuardOwnership.isOwnedBy(target, player.getUUID())) return;
        if (!isInRallyOrFollowing(target)) return;

        // Bloqueia o hit direto no seu próprio guarda em rali
        event.setCanceled(true);
    }

    // 2) Qualquer dano (inclui sweep/projéteis/tridente)
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (!GuardOwnership.isGuard(victim)) return;

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();        // normalmente o jogador (corpo-a-corpo)
        Entity direct = source.getDirectEntity();    // projétil/tridente/etc.

        Player attackerPlayer = null;
        if (attacker instanceof Player p) attackerPlayer = p;
        else if (direct instanceof Player p2) attackerPlayer = p2;

        if (attackerPlayer == null) return;
        if (!GuardOwnership.isOwnedBy(victim, attackerPlayer.getUUID())) return;
        if (!isInRallyOrFollowing(victim)) return;

        // Cancela o dano aos guardas do dono quando em rali
        event.setCanceled(true);
    }

    // --- helpers ---
    private static boolean isInRallyOrFollowing(Entity guard) {
        CompoundTag n = new CompoundTag();
        guard.saveWithoutId(n);
        if (n.getBoolean("rallyguard:in_rally")) return true;
        return n.getBoolean("Following");
    }
}
