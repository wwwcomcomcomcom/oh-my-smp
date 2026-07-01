package iieiiergn.smpAuth.paperlib;

import iieiiergn.smpAuth.common.StudentData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired (on the main thread) once Velocity has answered the join-time auth request for a player.
 * {@link #getData()} is null when the player is not linked.
 */
public final class AuthDataLoadedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final StudentData data;

    public AuthDataLoadedEvent(Player player, StudentData data) {
        this.player = player;
        this.data = data;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isLinked() {
        return data != null;
    }

    @Nullable
    public StudentData getData() {
        return data;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
