package iieiiergn.smpAuth.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

/** {@code /login} — shows the DataGSM auth URL and tells the player to paste their key with /verify. */
public final class LoginCommand extends Command {

    public LoginCommand(LobbyConfig config) {
        super("login");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("플레이어만 사용할 수 있습니다."));
                return;
            }
            player.sendMessage(Component.text("DataGSM 인증을 진행하세요:", NamedTextColor.AQUA));
            player.sendMessage(Component.text(config.authLoginUrl, NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.openUrl(config.authLoginUrl))
                    .hoverEvent(HoverEvent.showText(Component.text("클릭하여 브라우저에서 열기"))));
            player.sendMessage(Component.text("로그인 후 발급된 키를 ", NamedTextColor.GRAY)
                    .append(Component.text("/verify <키>", NamedTextColor.YELLOW))
                    .append(Component.text(" 로 입력하세요.", NamedTextColor.GRAY)));
        });
    }
}
