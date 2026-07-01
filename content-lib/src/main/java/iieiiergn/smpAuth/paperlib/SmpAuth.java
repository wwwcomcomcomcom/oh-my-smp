package iieiiergn.smpAuth.paperlib;

import iieiiergn.smpAuth.common.StudentData;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public API content servers use to read a player's DataGSM auth data.
 *
 * <p>Data is fetched automatically when the player joins; it may be momentarily absent
 * for the first tick or two after join — listen to {@link AuthDataLoadedEvent} for the
 * exact moment it lands, or just poll {@link #get(Player)}.
 */
public final class SmpAuth {

    private static final ConcurrentHashMap<UUID, StudentData> CACHE = new ConcurrentHashMap<>();

    private SmpAuth() {
    }

    /** The player's student data, or empty if not linked / not yet loaded. */
    public static Optional<StudentData> get(Player player) {
        return Optional.ofNullable(CACHE.get(player.getUniqueId()));
    }

    /** Whether the player has loaded, linked auth data. */
    public static boolean isLinked(Player player) {
        return CACHE.containsKey(player.getUniqueId());
    }

    // --- internal, called by the plugin's messenger ---

    static void put(UUID uuid, StudentData data) {
        CACHE.put(uuid, data);
    }

    static void remove(UUID uuid) {
        CACHE.remove(uuid);
    }
}
