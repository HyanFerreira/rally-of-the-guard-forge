package net.hfstack.rallyguard.network.payload;

import net.hfstack.rallyguard.screen.GuardCommandScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S2C: servidor envia lista de guardas (id, nome, patrulhando).
 */
public final class GuardListS2CPacket {
    public static final class Entry {
        public final int entityId;
        public final String name;
        public final boolean patrolling;

        public Entry(int entityId, String name, boolean patrolling) {
            this.entityId = entityId;
            this.name = name;
            this.patrolling = patrolling;
        }
    }

    private final List<Entry> entries;

    public GuardListS2CPacket(List<Entry> entries) {
        this.entries = entries;
    }

    public List<Entry> entries() {
        return entries;
    }

    public static GuardListS2CPacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int id = buf.readVarInt();
            String name = buf.readUtf(256);
            boolean pat = buf.readBoolean();
            list.add(new Entry(id, name, pat));
        }
        return new GuardListS2CPacket(list);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (Entry e : entries) {
            buf.writeVarInt(e.entityId);
            buf.writeUtf(e.name, 256);
            buf.writeBoolean(e.patrolling);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        var context = ctx.get();
        context.enqueueWork(() -> {
            // Garantia básica de que estamos no cliente
            if (context.getDirection().getReceptionSide().isClient()) {
                GuardCommandScreen.openFromPayload(this);
            }
        });
        context.setPacketHandled(true);
    }
}
