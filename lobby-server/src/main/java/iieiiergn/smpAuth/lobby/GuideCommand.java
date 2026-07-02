package iieiiergn.smpAuth.lobby;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

/** {@code /guide} — opens a written book covering /login, /verify and oh-my-smp server rules. */
public final class GuideCommand extends Command {

    public GuideCommand(LobbyConfig config) {
        super("guide", "rules");

        Book book = GuideBook.build(config);

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("플레이어만 사용할 수 있습니다."));
                return;
            }
            player.openBook(book);
        });
    }
}
