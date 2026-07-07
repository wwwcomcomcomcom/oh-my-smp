package iieiiergn.smpAuth.lobby;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the {@code /guide} written book: login steps + oh-my-smp server rules.
 * Rule numbers come from {@link LobbyConfig} (rendered from the profile by smp.sh),
 * so this book always shows the same values as the Paper server — never hardcode them.
 */
final class GuideBook {

    private GuideBook() {
    }

    static Book build(LobbyConfig config) {
        // 각 규칙을 개별 페이지로 둔다 — 한 페이지에 여러 규칙을 담으면 내용이 길어져 잘리기 때문.
        List<Component> pages = new ArrayList<>();
        pages.add(borderPage(config));
        pages.add(combatPage(config));
        pages.add(inventorySavePage(config));
        pages.add(tpaPage());
        pages.add(dragonPage(config));
        if (config.nametagEnabled) pages.add(nametagPage());
        pages.add(loginPage(config));
        return Book.book(
                Component.text("oh-my-smp 가이드"),
                Component.text("oh-my-smp"),
                pages
        );
    }

    private static Component loginPage(LobbyConfig config) {
        return Component.text()
                .append(heading("로그인 방법"))
                .append(Component.newline()).append(Component.newline())
                .append(body("1. 아래 링크를 클릭해 DataGSM 인증을 진행하세요."))
                .append(Component.newline())
                .append(Component.text("[인증 페이지 열기]", NamedTextColor.BLUE, TextDecoration.UNDERLINED)
                        .clickEvent(ClickEvent.openUrl(config.authLoginUrl))
                        .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(
                                Component.text(config.authLoginUrl))))
                .append(Component.newline()).append(Component.newline())
                .append(body("2. 인증 후 발급된 키를 채팅창에 "))
                .append(Component.text("/verify <키>", NamedTextColor.DARK_GREEN))
                .append(body(" 로 입력하세요."))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("인증이 완료되면 자동으로 다른 서버로 이동할 수 있습니다.",
                        NamedTextColor.DARK_GRAY))
                .build();
    }

    private static Component borderPage(LobbyConfig config) {
        return Component.text()
                .append(heading("월드보더"))
                .append(Component.newline()).append(Component.newline())
                .append(body("중심 (" + num(config.borderCenterX) + ", " + num(config.borderCenterZ) +
                        ")에서 반경 " + num(config.borderRadius) + "블록까지만 이동할 수 있습니다."))
                .build();
    }

    private static Component combatPage(LobbyConfig config) {
        return Component.text()
                .append(heading("전투 상태"))
                .append(Component.newline()).append(Component.newline())
                .append(body("플레이어에게 피해를 입으면 전투 상태가 되며, 이 동안 로그아웃하면 " +
                        "사망 처리되어 인벤토리를 전부 바닥에 떨어뜨립니다. " +
                        config.combatDurationSeconds + "초간 피해를 입지 않으면 전투 상태가 해제됩니다."))
                .build();
    }

    private static Component inventorySavePage(LobbyConfig config) {
        return Component.text()
                .append(heading("인벤세이브"))
                .append(Component.newline()).append(Component.newline())
                .append(body("전투가 아닌 사망(추락, 익사 등) 시에는 인벤토리 전체가 아니라, " +
                        "각 아이템마다 " + num(config.naturalDropChance * 100) + "% 확률로 개별 판정하여 잃게 됩니다. " +
                        "잃지 않은 아이템은 부활 시 그대로 돌아옵니다."))
                .build();
    }

    private static Component tpaPage() {
        return Component.text()
                .append(heading("TPA (텔레포트 요청)"))
                .append(Component.newline()).append(Component.newline())
                .append(body("/tpa <플레이어>"))
                .append(body(" — 대상에게 요청을 보냅니다. 수락하면 내가 대상에게 이동합니다."))
                .append(Component.newline()).append(Component.newline())
                .append(body("/tpa accept"))
                .append(body(" — 받은 요청을 수락합니다."))
                .append(Component.newline())
                .append(body("/tpa deny"))
                .append(body(" — 받은 요청을 거절합니다."))
                .append(Component.newline()).append(Component.newline())
                .append(body("컴뱃 중에는 사용할 수 없습니다. 자세한 사용법은 "))
                .append(Component.text("/tpa help", NamedTextColor.DARK_GREEN))
                .append(body(" 로 확인하세요."))
                .build();
    }

    private static Component dragonPage(LobbyConfig config) {
        List<String> immunities = new ArrayList<>();
        if (config.dragonImmuneExplosion) immunities.add("폭발");
        if (config.dragonImmuneProjectile) immunities.add("원거리");
        String immunityText = immunities.isEmpty() ? "" : String.join("·", immunities) + " 공격에 면역이며, ";
        double regenSeconds = config.dragonRegenIntervalTicks / 20.0;

        return Component.text()
                .append(heading("엔더 드래곤"))
                .append(Component.newline()).append(Component.newline())
                .append(body("엔더 드래곤의 죽음은 SMP 서버의 꽃이자 재앙입니다. " +
                        "게임의 진행 속도를 조절하기 위해 아래와 같은 조정이 적용됩니다."))
                .append(Component.newline()).append(Component.newline())
                .append(body("체력 " + num(config.dragonMaxHealth) + "로 증가됩니다.\n" +
                        immunityText + num(regenSeconds) + "초마다 체력 " + num(config.dragonRegenAmount) +
                        "을 회복합니다. 정면 승부를 준비하세요."))
                .build();
    }

    private static Component nametagPage() {
        return Component.text()
                .append(heading("이름표"))
                .append(Component.newline()).append(Component.newline())
                .append(body("DataGSM으로 인증된 플레이어는 머리 위에 이름이 표시됩니다. " +
                        "/student-info 명령어로 다른 플레이어의 정보를 확인할 수 있습니다."))
                .build();
    }

    private static Component heading(String text) {
        return Component.text(text, NamedTextColor.DARK_BLUE, TextDecoration.BOLD);
    }

    private static Component body(String text) {
        return Component.text(text, NamedTextColor.BLACK);
    }

    /** 정수 값이면 소수점 없이 표시(예: 5000.0 → "5000", 0.3 → "0.3"). smp-server GuideBook.num()과 동일. */
    private static String num(double value) {
        return value == (long) value ? Long.toString((long) value) : Double.toString(value);
    }
}
