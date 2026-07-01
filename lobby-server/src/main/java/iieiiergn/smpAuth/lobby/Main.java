package iieiiergn.smpAuth.lobby;

import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger("smp-lobby");

    public static void main(String[] args) throws Exception {
        LobbyConfig config = LobbyConfig.load();

        boolean useVelocity = config.velocitySecret != null && !config.velocitySecret.isBlank()
                && !config.velocitySecret.startsWith("CHANGE_ME");
        MinecraftServer server = useVelocity
                ? MinecraftServer.init(new Auth.Velocity(config.velocitySecret))
                : MinecraftServer.init();
        if (useVelocity) {
            LOGGER.info("Velocity modern forwarding enabled.");
        } else {
            LOGGER.warn("Velocity forwarding secret not configured — starting standalone (dev only).");
        }

        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));

        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0.5, 41, 0.5));
        });

        AuthClient authClient = new AuthClient(config.authServerBaseUrl, config.sharedSecret);
        MinecraftServer.getCommandManager().register(new LoginCommand(config));
        MinecraftServer.getCommandManager().register(new VerifyCommand(authClient));

        server.start(config.host, config.port);
        LOGGER.info("Lobby started on {}:{}", config.host, config.port);
    }
}
