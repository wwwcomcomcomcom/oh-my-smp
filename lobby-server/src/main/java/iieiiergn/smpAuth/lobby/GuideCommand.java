package iieiiergn.smpAuth.lobby;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

/** {@code /guide} — opens a written book covering /login, /verify and oh-my-smp server rules. */
public final class GuideCommand extends Command {

    public GuideCommand(Book book) {
        super("guide", "rules");

        setDefaultExecutor((sender, context) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("플레이어만 사용할 수 있습니다."));
                return;
            }
            show(player, book);
        });
    }

    /** Opens the guide book for {@code player}; also used to auto-show it on first spawn. */
    static void show(Player player, Book book) {
        player.openBook(book);
        // ClickEvent.suggestCommand has no effect inside written books (Mojang restriction),
        // so offer the working button via chat instead.
        player.sendMessage(Component.text("키를 입력하려면 클릭하세요: ", NamedTextColor.GRAY)
                .append(Component.text("[/verify 입력하기]", NamedTextColor.BLUE, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.suggestCommand("/verify "))));
    }
}
