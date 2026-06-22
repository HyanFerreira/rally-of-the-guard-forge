package net.hfstack.rallyguard.network;

import net.hfstack.rallyguard.RallyOfTheGuard;
import net.minecraft.resources.ResourceLocation;

public final class NetworkConstants {
    private NetworkConstants() {
    }

    // Mantemos os IDs como ResourceLocation (úteis para logs/depuração)
    public static final ResourceLocation OPEN_GUARD_COMMAND =
            new ResourceLocation(RallyOfTheGuard.MOD_ID, "open_guard_command");
    public static final ResourceLocation GUARD_LIST =
            new ResourceLocation(RallyOfTheGuard.MOD_ID, "guard_list");
    public static final ResourceLocation GUARD_ACTION =
            new ResourceLocation(RallyOfTheGuard.MOD_ID, "guard_action");

    // Ações
    public static final int ACTION_SUMMON = 1;
    public static final int ACTION_TOGGLE_PATROL = 2;
}
