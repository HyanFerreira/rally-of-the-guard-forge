package net.hfstack.rallyguard.network.payload;

import net.hfstack.rallyguard.network.GuardCommandNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: cliente pede a lista de guardas (sem payload).
 */
public record OpenGuardCommandC2SPacket() {

    public static OpenGuardCommandC2SPacket decode(FriendlyByteBuf buf) {
        return new OpenGuardCommandC2SPacket();
    }

    public void encode(FriendlyByteBuf buf) { /* vazio */ }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        var context = ctx.get();
        ServerPlayer sp = context.getSender();
        context.enqueueWork(() -> {
            if (sp != null) {
                GuardCommandNetworking.sendGuardList(sp);
            }
        });
        context.setPacketHandled(true);
    }
}
