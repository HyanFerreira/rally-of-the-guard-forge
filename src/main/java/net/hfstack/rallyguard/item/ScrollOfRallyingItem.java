package net.hfstack.rallyguard.item;

import net.hfstack.rallyguard.contract.GuardOwnership;
import net.hfstack.rallyguard.effect.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class ScrollOfRallyingItem extends Item {
    private static final String NBT_ACTIVE = "rallyguard:active";

    public ScrollOfRallyingItem(Properties props) {
        super(props);
    }

    private static boolean isActive(ItemStack stack) {
        return stack.hasTag() && stack.getOrCreateTag().getBoolean(NBT_ACTIVE);
    }

    private static void setActive(ItemStack stack, boolean v) {
        stack.getOrCreateTag().putBoolean(NBT_ACTIVE, v);
    }

    private static boolean isPatrolling(Entity e) {
        CompoundTag n = new CompoundTag();
        e.saveWithoutId(n);
        return n.getBoolean("Patrolling");
    }

    private static void rallyGuardToPlayer(Entity e, Player user, double x, double y, double z) {
        clearPath(e);
        e.setDeltaMovement(0.0, 0.0, 0.0);
        e.moveTo(x, y, z, e.getYRot(), e.getXRot());

        setRallyFollowing(e, false, true);
        setRallyFollowing(e, true, true);

        if (e instanceof Mob mob) {
            mob.setNoAi(false);
            mob.lookAt(user, 30.0F, 30.0F);
        }
    }

    private static void setRallyFollowing(Entity e, boolean following, boolean inRally) {
        CompoundTag nbt = new CompoundTag();
        e.saveWithoutId(nbt);
        nbt.putBoolean("rallyguard:in_rally", inRally);
        nbt.putBoolean("Following", following);
        e.load(nbt);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (level.isClientSide) {
            // deixa o servidor decidir, mas retorna success pro feedback imediato
            return InteractionResultHolder.success(stack);
        }

        if (!(user instanceof ServerPlayer sp)) {
            return InteractionResultHolder.pass(stack);
        }
        // precisa estar agachado (equivalente ao isSneaking do Fabric)
        if (!user.isCrouching()) {
            return InteractionResultHolder.pass(stack);
        }

        boolean rallyOn = user.hasEffect(ModEffects.RALLY_COMMANDER.get()); // TODO: garantir field em ModEffects
        EntityType<?> guardType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("guardvillagers", "guard"));
        ServerLevel sw = sp.serverLevel();

        if (guardType == null) {
            sp.displayClientMessage(Component.literal("guardvillagers:guard não encontrado"), true);
            return InteractionResultHolder.pass(stack);
        }

        // Raio 100 blocos ao redor do player
        AABB box = new AABB(
                user.getX() - 100, user.getY() - 100, user.getZ() - 100,
                user.getX() + 100, user.getY() + 100, user.getZ() + 100
        );

        if (rallyOn) {
            // === DESATIVAR RALI ===
            user.removeEffect(ModEffects.RALLY_COMMANDER.get());
            setActive(stack, false);
            user.displayClientMessage(
                    Component.translatable("alert.rallyguard.scroll_of_rallying.strength_lost")
                            .withStyle(s -> s.withColor(0xFF0000)),
                    false
            );

            List<Entity> myGuards = sw.getEntitiesOfClass(
                    Entity.class, box,
                    e -> e.getType() == guardType && GuardOwnership.isOwnedBy(e, user.getUUID())
            );

            for (Entity g : myGuards) {
                CompoundTag nbt = new CompoundTag();
                g.saveWithoutId(nbt);
                nbt.putBoolean("rallyguard:in_rally", false);
                nbt.putBoolean("Following", false);
                clearPath(g);
                g.load(nbt);
            }

        } else {
            // === ATIVAR RALI === (apenas guardas seus que NÃO estão patrulhando)
            user.addEffect(new MobEffectInstance(
                    ModEffects.RALLY_COMMANDER.get(), Integer.MAX_VALUE, 0, false, false, true
            ));
            setActive(stack, true);
            user.displayClientMessage(
                    Component.translatable("alert.rallyguard.scroll_of_rallying.strength_gained")
                            .withStyle(s -> s.withColor(0x00FF00)),
                    false
            );

            List<Entity> candidates = sw.getEntitiesOfClass(
                    Entity.class, box,
                    e -> e.getType() == guardType && GuardOwnership.isOwnedBy(e, user.getUUID())
            );

            List<Entity> joiners = new ArrayList<>();
            for (Entity g : candidates) {
                if (!isPatrolling(g)) {
                    joiners.add(g);
                }
            }

            int total = joiners.size();
            int i = 0;
            for (Entity g : joiners) {
                double angle = (Math.PI / (total + 1)) * (++i);
                double radius = 3.5;
                double gx = user.getX() + Math.cos(angle) * radius;
                double gz = user.getZ() + Math.sin(angle) * radius;

                rallyGuardToPlayer(g, user, gx, user.getY(), gz);
            }
        }

        user.getCooldowns().addCooldown(this, 60);
        return InteractionResultHolder.success(stack);
    }

    private static void clearPath(Entity e) {
        if (e instanceof Mob mob) {
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.getNavigation().stop();
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isActive(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.rallyguard.scroll_of_rallying.tooltip_desc"));
        super.appendHoverText(stack, level, tip, flag);
    }
}
