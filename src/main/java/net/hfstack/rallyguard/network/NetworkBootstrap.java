package net.hfstack.rallyguard.network;

import net.hfstack.rallyguard.RallyOfTheGuard;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.concurrent.atomic.AtomicBoolean;

public final class NetworkBootstrap {
    private static final String PROTOCOL = "1";
    private static final AtomicBoolean INIT = new AtomicBoolean(false);

    public static SimpleChannel CHANNEL;

    private NetworkBootstrap() {
    }

    public static void registerTypesOnce() {
        if (INIT.compareAndSet(false, true)) {
            CHANNEL = NetworkRegistry.ChannelBuilder
                    .named(new ResourceLocation(RallyOfTheGuard.MOD_ID, "main"))
                    .networkProtocolVersion(() -> PROTOCOL)
                    .clientAcceptedVersions(PROTOCOL::equals)
                    .serverAcceptedVersions(PROTOCOL::equals)
                    .simpleChannel();
        }
    }
}
