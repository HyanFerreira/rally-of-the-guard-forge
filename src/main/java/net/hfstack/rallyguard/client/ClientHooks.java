package net.hfstack.rallyguard.client;

import net.hfstack.rallyguard.network.NetworkBootstrap;
import net.hfstack.rallyguard.network.payload.OpenGuardCommandC2SPacket;

/**
 * Atalho client-only para pedir abertura do painel ao servidor.
 */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void requestOpenGuardPanel() {
        NetworkBootstrap.CHANNEL.sendToServer(new OpenGuardCommandC2SPacket());
    }
}
