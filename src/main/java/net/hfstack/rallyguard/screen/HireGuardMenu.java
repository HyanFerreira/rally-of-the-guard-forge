package net.hfstack.rallyguard.screen;

import net.hfstack.rallyguard.contract.GuardOwnership;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class HireGuardMenu extends AbstractContainerMenu {
    private final int guardEntityId; // no CLIENTE fica -1 (não usado)
    private final Inventory playerInventory;

    // Construtor CLIENTE (factory IForgeMenuType): (syncId, inv, buf)
    public HireGuardMenu(int syncId, Inventory inv, FriendlyByteBuf buf) {
        this(syncId, inv, -1);
    }

    // Construtor SERVIDOR
    public HireGuardMenu(int syncId, Inventory inv, int guardEntityId) {
        super(ModMenus.HIRE_MENU.get(), syncId);
        this.playerInventory = inv;
        this.guardEntityId = guardEntityId;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * id == 0 => botão "Contratar"
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp)) return false;
        if (id != 0) return false;

        ServerLevel level = sp.serverLevel();
        Entity guard = level.getEntity(this.guardEntityId);

        // Guarda inexistente -> fecha
        if (guard == null || !GuardOwnership.isGuard(guard)) {
            sp.closeContainer();
            return true;
        }

        // Já tem dono -> fecha e avisa
        if (GuardOwnership.hasOwner(guard)) {
            sp.closeContainer();
            sp.displayClientMessage(Component.translatable("gui.rallyguard.hire.already_owned"), true);
            return true;
        }

        final int COST = 3;
        int emeralds = 0;

        // Conta esmeraldas
        for (int i = 0; i < playerInventory.getContainerSize(); i++) {
            ItemStack s = playerInventory.getItem(i);
            if (s.is(Items.EMERALD)) emeralds += s.getCount();
        }

        // Não tem o suficiente -> fecha e avisa em overlay
        if (emeralds < COST) {
            sp.closeContainer();
            sp.displayClientMessage(Component.translatable("gui.rallyguard.hire.not_enough"), true);
            return true;
        }

        // Desconta custo
        int remaining = COST;
        for (int i = 0; i < playerInventory.getContainerSize() && remaining > 0; i++) {
            ItemStack s = playerInventory.getItem(i);
            if (s.is(Items.EMERALD)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }

        // Define dono (aplica nome dourado + mensagem "apresentando-se" em chat)
        GuardOwnership.setOwner(guard, sp);

        // Mensagem de sucesso (overlay)
        sp.displayClientMessage(Component.translatable("gui.rallyguard.hire.success"), true);

        // Fecha a tela
        sp.closeContainer();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }
}
