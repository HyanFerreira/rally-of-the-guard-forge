package net.hfstack.rallyguard.network.payload;

import net.hfstack.rallyguard.network.GuardCommandNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C2S: cliente manda ação para um guarda específico.
 */
public record GuardActionC2SPacket(int entityId, int action) {

    public static GuardActionC2SPacket decode(FriendlyByteBuf buf) {
        int entity = buf.readVarInt();
        int action = buf.readVarInt();
        return new GuardActionC2SPacket(entity, action);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeVarInt(action);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        var context = ctx.get();
        ServerPlayer sp = context.getSender();
        context.enqueueWork(() -> {
            if (sp != null) {
                GuardCommandNetworking.handleAction(sp, entityId, action);
            }
        });
        context.setPacketHandled(true);
    }
}
