package net.hfstack.rallyguard.network;

import net.hfstack.rallyguard.contract.GuardOwnership;
import net.hfstack.rallyguard.network.payload.GuardActionC2SPacket;
import net.hfstack.rallyguard.network.payload.GuardListS2CPacket;
import net.hfstack.rallyguard.network.payload.OpenGuardCommandC2SPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.nbt.CompoundTag;

import java.util.ArrayList;
import java.util.List;

public final class GuardCommandNetworking {
    private GuardCommandNetworking() {
    }

    private static boolean REGISTERED = false;

    /**
     * Registra as mensagens (chame no commonSetup via enqueueWork).
     */
    public static synchronized void registerServer() {
        if (REGISTERED) return;
        REGISTERED = true;

        int id = 0;

        // C2S: abrir GUI → servidor envia lista
        NetworkBootstrap.CHANNEL.messageBuilder(OpenGuardCommandC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(OpenGuardCommandC2SPacket::decode)
                .encoder(OpenGuardCommandC2SPacket::encode)
                .consumerMainThread(OpenGuardCommandC2SPacket::handle)
                .add();

        // C2S: ação (teleportar / patrulhar)
        NetworkBootstrap.CHANNEL.messageBuilder(GuardActionC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(GuardActionC2SPacket::decode)
                .encoder(GuardActionC2SPacket::encode)
                .consumerMainThread(GuardActionC2SPacket::handle)
                .add();

        // S2C: lista de guards
        NetworkBootstrap.CHANNEL.messageBuilder(GuardListS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(GuardListS2CPacket::decode)
                .encoder(GuardListS2CPacket::encode)
                .consumerMainThread(GuardListS2CPacket::handle)
                .add();
    }

    // ==== Lado Servidor ====

    public static void sendGuardList(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        // EntityType do Guard Villagers
        ResourceLocation guardId = new ResourceLocation("guardvillagers", "guard");
        EntityType<?> guardType = ForgeRegistries.ENTITY_TYPES.getValue(guardId);
        if (guardType == null) {
            player.displayClientMessage(Component.literal("Guard type not found: " + guardId), true);
            return;
        }

        // Pega "todos" no mundo (AABB gigante; suficiente para o caso)
        AABB all = new AABB(-3.0e7, -3.0e7, -3.0e7, 3.0e7, 3.0e7, 3.0e7);
        List<Entity> guards = level.getEntitiesOfClass(
                Entity.class,
                all,
                e -> e.getType() == guardType && GuardOwnership.isOwnedBy(e, player.getUUID())
        );

        List<GuardListS2CPacket.Entry> list = new ArrayList<>(guards.size());
        for (Entity g : guards) {
            String name = g.getName().getString();
            boolean patrolling = readBool(g, "Patrolling");
            list.add(new GuardListS2CPacket.Entry(g.getId(), name, patrolling));
        }

        NetworkBootstrap.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new GuardListS2CPacket(list));
    }

    public static void handleAction(ServerPlayer player, int entityId, int action) {
        ServerLevel level = player.serverLevel();
        Entity e = level.getEntity(entityId);

        if (e == null || !GuardOwnership.isGuard(e)) {
            player.displayClientMessage(Component.translatable("gui.rallyguard.command.not_found"), true);
            return;
        }
        if (!GuardOwnership.isOwnedBy(e, player.getUUID())) {
            player.displayClientMessage(Component.translatable("gui.rallyguard.command.not_owner"), true);
            return;
        }

        switch (action) {
            case NetworkConstants.ACTION_SUMMON -> {
                double ox = (player.getRandom().nextDouble() - 0.5) * 2.5;
                double oz = (player.getRandom().nextDouble() - 0.5) * 2.5;
                e.moveTo(player.getX() + ox, player.getY(), player.getZ() + oz, e.getYRot(), e.getXRot());
                writeBool(e, "rallyguard:in_rally", false);
                player.displayClientMessage(Component.translatable("gui.rallyguard.command.summoned"), true);
            }
            case NetworkConstants.ACTION_TOGGLE_PATROL -> {
                boolean patrolling = readBool(e, "Patrolling");
                if (patrolling) {
                    // DESLIGAR patrulha
                    CompoundTag n = new CompoundTag();
                    e.saveWithoutId(n);
                    n.putBoolean("Patrolling", false);
                    n.putBoolean("Following", false);
                    n.putBoolean("rallyguard:in_rally", false);

                    n.remove("PatrolPosX");
                    n.remove("PatrolPosY");
                    n.remove("PatrolPosZ");
                    n.remove("PatrolPos");
                    n.remove("PatrolPosL");

                    e.load(n);

                    if (e instanceof Mob mob) {
                        mob.setTarget(null);
                        mob.setAggressive(false);
                        mob.getNavigation().stop();
                    }
                    player.displayClientMessage(Component.translatable("gui.rallyguard.command.patrol_off"), true);

                } else {
                    // LIGAR patrulha: marca o ponto na POSIÇÃO ATUAL DO PLAYER
                    var bp = player.blockPosition();

                    CompoundTag n = new CompoundTag();
                    e.saveWithoutId(n);

                    n.putBoolean("Following", false);
                    n.putBoolean("rallyguard:in_rally", false);
                    n.putBoolean("Patrolling", true);

                    n.putInt("PatrolPosX", bp.getX());
                    n.putInt("PatrolPosY", bp.getY());
                    n.putInt("PatrolPosZ", bp.getZ());

                    n.putIntArray("PatrolPos", new int[]{bp.getX(), bp.getY(), bp.getZ()});
                    n.putLong("PatrolPosL", bp.asLong());

                    e.load(n);

                    if (e instanceof Mob mob) {
                        mob.setTarget(null);
                        mob.setAggressive(false);
                        mob.getNavigation().stop();
                    }

                    // Debounce no tick seguinte
                    player.server.execute(() -> {
                        if (e instanceof Mob mob2) mob2.getNavigation().stop();
                    });

                    player.displayClientMessage(Component.translatable("gui.rallyguard.command.patrol_on"), true);
                }
            }
        }
    }

    // --- Helpers NBT (Forge/Mojmap) ---
    private static boolean readBool(Entity e, String key) {
        CompoundTag n = new CompoundTag();
        e.saveWithoutId(n);
        return n.getBoolean(key);
    }

    private static void writeBool(Entity e, String key, boolean v) {
        CompoundTag n = new CompoundTag();
        e.saveWithoutId(n);
        n.putBoolean(key, v);
        e.load(n);
    }
}
