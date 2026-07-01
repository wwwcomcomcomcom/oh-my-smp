package iieiiergn.smpAuth.lobby;

import iieiiergn.smpAuth.common.AuthMessage;
import iieiiergn.smpAuth.common.Channels;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;

/** {@code /verify <key>} — submits the pasted key to the auth-server and notifies Velocity on success. */
public final class VerifyCommand extends Command {

    public VerifyCommand(AuthClient client) {
        super("verify");

        Argument<String> keyArg = ArgumentType.String("key");

        setDefaultExecutor((sender, context) ->
                sender.sendMessage(Component.text("사용법: /verify <키>", NamedTextColor.YELLOW)));

        addSyntax((sender, context) -> {
            if (!(sender instanceof Player player)) return;
            String key = context.get(keyArg);
            player.sendMessage(Component.text("인증 확인 중...", NamedTextColor.GRAY));
            client.bind(player.getUuid(), player.getUsername(), key).thenAccept(student -> {
                if (student == null) {
                    player.sendMessage(Component.text(
                            "키가 올바르지 않거나 만료되었습니다. /login 으로 다시 시도하세요.", NamedTextColor.RED));
                    return;
                }
                // Tell Velocity to (re)load this player's link into global state.
                player.sendPluginMessage(Channels.AUTH,
                        AuthMessage.linkUpdated(player.getUuid().toString()).encode());

                String name = student.name() != null ? student.name() : "인증된 사용자";
                player.sendMessage(Component.text("인증 완료! 환영합니다, " + name + "님.", NamedTextColor.GREEN));
                player.sendMessage(Component.text("이제 다른 서버로 이동할 수 있습니다.", NamedTextColor.GRAY));
            });
        }, keyArg);
    }
}
