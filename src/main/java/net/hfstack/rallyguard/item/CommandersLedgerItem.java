package net.hfstack.rallyguard.item;

import net.hfstack.rallyguard.network.NetworkBootstrap;
import net.hfstack.rallyguard.network.payload.OpenGuardCommandC2SPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Livro de Comando — abre o painel de controle dos guardas.
 * No Forge, basta enviar o packet C2S ao usar no CLIENTE.
 */
public class CommandersLedgerItem extends Item {
    public CommandersLedgerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (level.isClientSide) {
            // Pede a lista ao servidor (ele responde com S2C e a GUI cliente abre/atualiza)
            NetworkBootstrap.CHANNEL.sendToServer(new OpenGuardCommandC2SPacket());
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tips, TooltipFlag flag) {
        tips.add(Component.translatable("item.rallyguard.commanders_ledger.tooltip"));
        super.appendHoverText(stack, level, tips, flag);
    }
}
