package net.hfstack.rallyguard.contract;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public final class GuardOwnership {
    private GuardOwnership() {
    }

    private static final ResourceLocation GUARD_ID = new ResourceLocation("guardvillagers", "guard");
    private static final int GOLD = 0xFFD700;

    /**
     * Retorna true se a entidade é um Guard do GuardVillagers.
     */
    public static boolean isGuard(Entity e) {
        if (e == null) return false;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(GUARD_ID);
        return type != null && e.getType() == type;
    }

    /**
     * Lê dono do NBT (suporta chaves "Owner" e "rallyguard:Owner").
     */
    public static UUID getOwner(Entity guard) {
        CompoundTag n = new CompoundTag();
        guard.saveWithoutId(n);
        if (n.hasUUID("Owner")) return n.getUUID("Owner");
        if (n.hasUUID("rallyguard:Owner")) return n.getUUID("rallyguard:Owner");
        return null;
    }

    public static boolean hasOwner(Entity guard) {
        return getOwner(guard) != null;
    }

    public static boolean isOwnedBy(Entity guard, UUID player) {
        UUID owner = getOwner(guard);
        return owner != null && owner.equals(player);
    }

    /**
     * Define o dono no NBT e aplica nome dourado; avisa o jogador no chat.
     */
    public static void setOwner(Entity guard, ServerPlayer player) {
        CompoundTag n = new CompoundTag();
        guard.saveWithoutId(n);
        n.putUUID("Owner", player.getUUID());
        n.putUUID("rallyguard:Owner", player.getUUID()); // redundância para nosso mod
        guard.load(n);

        applyGoldName(guard);

        String display = guard.getName().getString();
        // mensagem em chat (não overlay)
        player.sendSystemMessage(Component.translatable("message.rallyguard.guard_presenting", display));
    }

    private static void applyGoldName(Entity guard) {
        if (!(guard instanceof LivingEntity le)) return;

        String base = le.getName().getString();
        Component golden = Component.literal(base).withStyle(s -> s.withColor(GOLD));
        le.setCustomName(golden);
        le.setCustomNameVisible(true);
    }
}
