package net.hfstack.rallyguard.event;

import net.hfstack.rallyguard.contract.GuardOwnership;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

/**
 * Seus guardas contratados NÃO se voltam contra você quando
 * você bate em um guarda que NÃO é seu. Eles ficam neutros por um tempo.
 */
public final class HiredGuardsNeutralityHandler {

    // ~10s ignorando o dono como alvo
    private static final int IGNORE_TICKS = 200;

    // Chave: "<dim>#<entityId>" -> (ownerUuid, ticksRestantes)
    private static final Map<String, Entry> TRACK = new HashMap<>();

    private record Entry(UUID owner, int ticks) {
    }

    // Quando você acerta um guarda NÃO-contratado…
    @SubscribeEvent
    public void onPlayerAttackGuard(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide) return;

        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (!GuardOwnership.isGuard(target)) return;

        // Se o alvo é um guarda seu, não mexe
        if (GuardOwnership.isOwnedBy(target, player.getUUID())) return;

        // Alvo é guarda, mas NÃO é seu -> neutraliza SEUS guardas próximos
        if (!(player.level() instanceof ServerLevel sw)) return;

        var guardType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("guardvillagers", "guard"));
        if (guardType == null) return;

        double r = 24.0;
        AABB area = new AABB(
                player.getX() - r, player.getY() - r, player.getZ() - r,
                player.getX() + r, player.getY() + r, player.getZ() + r
        );

        List<Entity> myGuards = sw.getEntitiesOfClass(
                Entity.class,
                area,
                e -> e.getType() == guardType && GuardOwnership.isOwnedBy(e, player.getUUID())
        );

        for (Entity g : myGuards) {
            // Marca para ignorar o dono por IGNORE_TICKS
            track(sw, g.getId(), player.getUUID(), IGNORE_TICKS);

            // Se já estiver mirando o dono, limpa agora
            if (g instanceof Mob mob && mob.getTarget() != null
                    && player.getUUID().equals(mob.getTarget().getUUID())) {
                mob.setTarget(null);
                mob.setAggressive(false);
                mob.getNavigation().stop();
            }
        }
        // Não cancela o ataque original do jogador (comportamento do Fabric era PASS)
    }

    // A cada tick do mundo servidor, reforça a neutralidade por um tempo
    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sw)) return;
        if (TRACK.isEmpty()) return;

        String dim = sw.dimension().location().toString();
        List<String> toRemove = new ArrayList<>();

        for (Map.Entry<String, Entry> en : TRACK.entrySet()) {
            String key = en.getKey();
            if (!key.startsWith(dim + "#")) continue;

            Entry val = en.getValue();
            int entityId = parseEntityId(key);
            if (entityId < 0) {
                toRemove.add(key);
                continue;
            }

            Entity e = sw.getEntity(entityId);
            if (e == null) {
                toRemove.add(key);
                continue;
            }

            if (!GuardOwnership.isGuard(e) || !GuardOwnership.isOwnedBy(e, val.owner())) {
                toRemove.add(key);
                continue;
            }

            if (e instanceof Mob mob && mob.getTarget() != null
                    && val.owner().equals(mob.getTarget().getUUID())) {
                mob.setTarget(null);
                mob.setAggressive(false);
                mob.getNavigation().stop();
            }

            int left = val.ticks() - 1;
            if (left <= 0) {
                toRemove.add(key);
            } else {
                TRACK.put(key, new Entry(val.owner(), left));
            }
        }

        for (String k : toRemove) TRACK.remove(k);
    }

    // -- helpers --
    private static void track(ServerLevel world, int entityId, UUID owner, int ticks) {
        TRACK.put(key(world, entityId), new Entry(owner, ticks));
    }

    private static String key(ServerLevel world, int entityId) {
        return world.dimension().location().toString() + "#" + entityId;
    }

    private static int parseEntityId(String key) {
        int i = key.lastIndexOf('#');
        if (i < 0 || i + 1 >= key.length()) return -1;
        try {
            return Integer.parseInt(key.substring(i + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
