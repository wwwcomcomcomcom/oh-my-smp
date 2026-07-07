package iieiiergn.smpAuth.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import iieiiergn.smpAuth.common.AuthMessage;
import iieiiergn.smpAuth.common.Channels;
import iieiiergn.smpAuth.common.MessageType;
import iieiiergn.smpAuth.common.StudentData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "smp-auth", name = "SMP Auth", version = "1.0.0",
        description = "Proxy-global DataGSM auth state + content-server gating", authors = {"smp"})
public final class SmpAuthVelocity {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDir;
    private final ChannelIdentifier channel = MinecraftChannelIdentifier.from(Channels.AUTH);

    private VelocityConfig config;
    private AuthServerClient client;

    @Inject
    public SmpAuthVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDir) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDir = dataDir;
    }

    @Subscribe
    public void onInit(ProxyInitializeEvent event) {
        try {
            config = VelocityConfig.load(dataDir);
        } catch (Exception e) {
            logger.error("Failed to load config; using safe defaults", e);
            throw new IllegalStateException(e);
        }
        client = new AuthServerClient(config.authServerBaseUrl, config.sharedSecret);
        proxy.getChannelRegistrar().register(channel);
        logger.info("SMP Auth ready. {}", config);
    }

    /**
     * Routes already-authenticated players straight to the content server on join, skipping the
     * lobby (and its guide book) entirely. Runs async because it awaits the auth-server lookup:
     * returning the {@link EventTask} makes Velocity hold the initial connection until the link is
     * resolved, so — unlike a lobby-side plugin message right after spawn — the routing decision is
     * made before the player connects anywhere. Unlinked players (or on lookup failure) fall through
     * to the default initial server (the lobby).
     */
    @Subscribe
    public EventTask onChooseInitialServer(PlayerChooseInitialServerEvent event) {
        Player player = event.getPlayer();
        return EventTask.async(() -> {
            StudentData student = client.fetchLink(player.getUniqueId()).join();
            if (student == null) return;
            proxy.getServer(config.contentServerName).ifPresentOrElse(
                    event::setInitialServer,
                    () -> logger.warn("contentServerName '{}' is not a registered server; leaving {} in the lobby",
                            config.contentServerName, player.getUsername()));
        });
    }

    /**
     * Gates linked-only servers. Reads the link state straight from the auth-server (the source of
     * truth) on each connect rather than from a proxy-side cache: returning an {@link EventTask}
     * lets Velocity await the async lookup, so there is no in-memory state to fall stale across a
     * reconnect. Denies (and nudges to the lobby) when the player has no link.
     */
    @Subscribe
    public EventTask onPreConnect(ServerPreConnectEvent event) {
        String target = event.getOriginalServer().getServerInfo().getName();
        if (!config.isGated(target)) return null;
        Player player = event.getPlayer();
        return EventTask.async(() -> {
            if (client.fetchLink(player.getUniqueId()).join() == null) {
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
                player.sendMessage(Component.text(
                        "인증이 필요합니다. 로비에서 /login 으로 DataGSM 인증을 먼저 완료하세요.",
                        NamedTextColor.RED));
            }
        });
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().equals(channel)) return;
        // Only trust messages originating from a backend server connection.
        if (!(event.getSource() instanceof ServerConnection conn)) return;
        // Never forward auth-channel traffic to the other side.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        Player player = conn.getPlayer();
        AuthMessage msg = AuthMessage.decode(event.getData());
        // The only inbound message we act on is the lobby's LINK_UPDATED signal after /verify;
        // auth data itself is now read directly from the auth-server (no proxy-side cache to serve).
        if (msg.type() == MessageType.LINK_UPDATED) {
            client.fetchLink(player.getUniqueId()).thenAccept(student -> {
                if (student != null) {
                    logger.info("Link confirmed for {} after /verify", player.getUsername());
                    sendToContentServer(player);
                }
            });
        }
    }

    /** Auto-connects a freshly-verified player from the lobby to the content server. */
    private void sendToContentServer(Player player) {
        proxy.getServer(config.contentServerName).ifPresentOrElse(
                server -> player.createConnectionRequest(server).connect().thenAccept(result -> {
                    if (!result.isSuccessful()) {
                        logger.warn("Failed to auto-connect {} to {}: {}",
                                player.getUsername(), config.contentServerName, result.getReasonComponent().orElse(null));
                    }
                }),
                () -> logger.warn("contentServerName '{}' is not a registered server; cannot auto-connect {}",
                        config.contentServerName, player.getUsername())
        );
    }
}
