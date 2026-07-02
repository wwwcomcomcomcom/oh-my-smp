package iieiiergn.smpAuth.lobby;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

/** Builds the {@code /guide} written book: login steps + oh-my-smp server rules. */
final class GuideBook {

    private GuideBook() {
    }

    static Book build(LobbyConfig config) {
        return Book.book(
                Component.text("오미숲 안내서"),
                Component.text("oh-my-smp"),
                List.of(loginPage(config), rulesPageOne(), rulesPageTwo())
        );
    }

    private static Component loginPage(LobbyConfig config) {
        return Component.text()
                .append(Component.text("로그인 방법", NamedTextColor.DARK_BLUE, TextDecoration.BOLD))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("1. 아래 링크를 클릭해 DataGSM 인증을 진행하세요.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("[인증 페이지 열기]", NamedTextColor.BLUE, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(config.authLoginUrl))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                Component.text(config.authLoginUrl))))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("2. 인증 후 발급된 키를 아래 버튼으로 입력하세요.", NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("[/verify 입력하기]", NamedTextColor.BLUE, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.suggestCommand("/verify ")))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("인증이 완료되면 자동으로 다른 서버로 이동할 수 있습니다.",
                        NamedTextColor.DARK_GRAY))
                .build();
    }

    private static Component rulesPageOne() {
        return Component.text()
                .append(Component.text("서버 규칙 (1/2)", NamedTextColor.DARK_BLUE, TextDecoration.BOLD))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("월드보더", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("중심 (0, 0)에서 반경 5000블록까지만 이동할 수 있습니다.",
                        NamedTextColor.BLACK))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("전투 태그", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("피해를 입으면 15초간 전투 상태가 되며, 이 동안 로그아웃하면 " +
                                "다음 접속 시 위치를 보장받지 못할 수 있습니다.",
                        NamedTextColor.BLACK))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("자연사 드롭", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("전투가 아닌 사망(추락, 익사 등) 시 인벤토리의 각 아이템 스택이 " +
                                "30% 확률로 사라집니다.",
                        NamedTextColor.BLACK))
                .build();
    }

    private static Component rulesPageTwo() {
        return Component.text()
                .append(Component.text("서버 규칙 (2/2)", NamedTextColor.DARK_BLUE, TextDecoration.BOLD))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("엔더 드래곤", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("체력 1000, 폭발·원거리 공격에 면역이며 매초 서서히 체력을 " +
                                "회복합니다. 정면 승부를 준비하세요.",
                        NamedTextColor.BLACK))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("이름표", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("인증된 플레이어는 머리 위에 학번과 이름이 표시됩니다. " +
                                "/student-info 명령어로 다른 플레이어의 정보를 확인할 수 있습니다.",
                        NamedTextColor.BLACK))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("이 안내서는 언제든 /guide 명령어로 다시 볼 수 있습니다.",
                        NamedTextColor.DARK_GRAY))
                .build();
    }
}
