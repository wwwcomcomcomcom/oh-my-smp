package iieiiergn.smpAuth.paperlib;

import iieiiergn.smpAuth.common.AuthMessage;
import iieiiergn.smpAuth.common.Channels;
import iieiiergn.smpAuth.common.MessageType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code SmpAuth} content-server plugin: owns the {@link Channels#AUTH} channel,
 * requests each joining player's auth data from Velocity, caches the answer, and
 * exposes it through {@link SmpAuth} + {@link AuthDataLoadedEvent}.
 */
public final class SmpAuthPlugin extends JavaPlugin implements Listener, PluginMessageListener {

    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, Channels.AUTH);
        getServer().getMessenger().registerIncomingPluginChannel(this, Channels.AUTH, this);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SmpAuth ready — content plugins can now use SmpAuth.get(player).");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Ask Velocity for this player's auth state (next tick, once the connection is settled).
        getServer().getScheduler().runTask(this, () ->
                player.sendPluginMessage(this, Channels.AUTH, AuthMessage.request().encode()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        SmpAuth.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals(Channels.AUTH)) return;
        AuthMessage msg = AuthMessage.decode(message);
        if (msg.type() != MessageType.AUTH_RESPONSE) return;

        if (msg.linked() && msg.student() != null) {
            SmpAuth.put(player.getUniqueId(), msg.student());
        } else {
            SmpAuth.remove(player.getUniqueId());
        }
        Bukkit.getPluginManager().callEvent(new AuthDataLoadedEvent(player, msg.student()));
    }
}
