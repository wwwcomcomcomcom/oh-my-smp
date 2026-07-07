package iieiiergn.smpAuth.lobby;

import iieiiergn.smpAuth.common.AuthMessage;
import iieiiergn.smpAuth.common.Channels;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.timer.TaskSchedule;
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

        Book guideBook = GuideBook.build(config);
        AuthClient authClient = new AuthClient(config.authServerBaseUrl, config.sharedSecret);

        GlobalEventHandler events = MinecraftServer.getGlobalEventHandler();
        events.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instance);
            event.getPlayer().setRespawnPoint(new Pos(0.5, 41, 0.5));
        });
        events.addListener(PlayerSpawnEvent.class, event -> {
            var player = event.getPlayer();
            // Already-authenticated players skip the lobby entirely: check their link and, if
            // present, bounce straight to the content server without showing the guide/login.
            // fetchLink returns null on "not linked" or any error, so the fallback is the lobby flow.
            authClient.fetchLink(player.getUuid()).thenAccept(student -> {
                if (student != null) {
                    player.sendMessage(Component.text(
                            "인증된 사용자입니다. 서버로 이동합니다...", NamedTextColor.GREEN));
                    // Reuse the /verify success path: Velocity reloads the link and auto-connects
                    // the player to the content server (see SmpAuthVelocity#sendToContentServer).
                    player.sendPluginMessage(Channels.AUTH,
                            AuthMessage.linkUpdated(player.getUuid().toString()).encode());
                    return;
                }
                player.sendMessage(Component.text(
                        "인증하려면 /login 을 입력하세요. 안내서는 /guide 로 다시 볼 수 있습니다.",
                        NamedTextColor.YELLOW));
                // The client ignores an OpenBookPacket sent while it's still on the join loading
                // screen, so wait a few ticks until it has actually rendered the world.
                player.scheduler().buildTask(() -> GuideCommand.show(player, guideBook))
                        .delay(TaskSchedule.tick(20))
                        .schedule();
            });
        });

        MinecraftServer.getCommandManager().register(new LoginCommand(config));
        MinecraftServer.getCommandManager().register(new VerifyCommand(authClient));
        MinecraftServer.getCommandManager().register(new GuideCommand(guideBook));

        server.start(config.host, config.port);
        LOGGER.info("Lobby started on {}:{}", config.host, config.port);
    }
}
