package iieiiergn.smpAuth.paperlib;

import iieiiergn.smpAuth.common.StudentData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * The {@code SmpAuth} content-server plugin: on join it reads the player's DataGSM link
 * directly from the auth-server (the source of truth), caches the answer, and exposes it
 * through {@link SmpAuth} + {@link AuthDataLoadedEvent}.
 *
 * <p>Reading the auth-server directly (rather than asking the Velocity proxy for its cached
 * copy) means a player's link can't be lost to a proxy-side reconnect race — every join
 * re-reads the persistent truth. Connection to the auth-server (base URL + shared secret)
 * is configured in {@code config.yml}, rendered by {@code smp.sh} from the active profile.
 */
public final class SmpAuthPlugin extends JavaPlugin implements Listener {

    private AuthServerClient client;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String baseUrl = getConfig().getString("auth-server-base-url", "http://127.0.0.1:8080");
        String sharedSecret = getConfig().getString("shared-secret", "");
        if (sharedSecret.isEmpty()) {
            getLogger().warning("shared-secret is empty in config.yml — auth lookups will fail until it is set.");
        }
        client = new AuthServerClient(baseUrl, sharedSecret);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SmpAuth ready — reading auth data directly from " + baseUrl
                + " (content plugins use SmpAuth.get(player)).");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        // Off-thread HTTP lookup; hop back to the main thread before touching Bukkit /
        // firing the event. The cache stays empty for the first tick or two after join.
        client.fetchLink(uuid).thenAccept(student ->
                getServer().getScheduler().runTask(this, () -> {
                    if (!player.isOnline()) return;
                    if (student != null) {
                        SmpAuth.put(uuid, student);
                    } else {
                        SmpAuth.remove(uuid);
                    }
                    Bukkit.getPluginManager().callEvent(new AuthDataLoadedEvent(player, student));
                }));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SmpAuth.remove(event.getPlayer().getUniqueId());
    }
}
