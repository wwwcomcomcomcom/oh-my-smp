package iieiiergn.smpAuth.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
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
    private AuthState state;
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
        state = new AuthState();
        client = new AuthServerClient(config.authServerBaseUrl, config.sharedSecret);
        proxy.getChannelRegistrar().register(channel);
        logger.info("SMP Auth ready. {}", config);
    }

    @Subscribe
    public void onLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        client.fetchLink(player.getUniqueId()).thenAccept(student -> {
            if (student != null) {
                state.put(player.getUniqueId(), student);
                logger.info("Loaded link for {} ({})", player.getUsername(), student.name());
            }
        });
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        state.remove(event.getPlayer().getUniqueId());
    }

    @Subscribe
    public void onPreConnect(ServerPreConnectEvent event) {
        String target = event.getOriginalServer().getServerInfo().getName();
        if (!config.isGated(target)) return;
        if (!state.isLinked(event.getPlayer().getUniqueId())) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            event.getPlayer().sendMessage(Component.text(
                    "인증이 필요합니다. 로비에서 /login 으로 DataGSM 인증을 먼저 완료하세요.",
                    NamedTextColor.RED));
        }
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
        switch (msg.type()) {
            case AUTH_REQUEST -> {
                StudentData data = state.get(player.getUniqueId());
                conn.sendPluginMessage(channel,
                        AuthMessage.response(player.getUniqueId().toString(), data).encode());
            }
            case LINK_UPDATED -> client.fetchLink(player.getUniqueId()).thenAccept(student -> {
                if (student != null) {
                    state.put(player.getUniqueId(), student);
                    logger.info("Reloaded link for {} after /verify", player.getUsername());
                }
            });
            default -> { /* AUTH_RESPONSE is proxy→backend only; ignore inbound */ }
        }
    }
}
