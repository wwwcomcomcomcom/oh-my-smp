package iieiiergn.smpAuth.lobby;

import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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
        return Book.book(
                Component.text("oh-my-smp 가이드"),
                Component.text("oh-my-smp"),
                List.of(loginPage(config), rulesPageOne(config), rulesPageTwo(config))
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
                .append(Component.text("2. 인증 후 발급된 키를 채팅창에 ", NamedTextColor.BLACK))
                .append(Component.text("/verify <키>", NamedTextColor.DARK_GREEN))
                .append(Component.text(" 로 입력하세요.", NamedTextColor.BLACK))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("인증이 완료되면 자동으로 다른 서버로 이동할 수 있습니다.",
                        NamedTextColor.DARK_GRAY))
                .build();
    }

    private static Component rulesPageOne(LobbyConfig config) {
        return Component.text()
                .append(Component.text("서버 규칙 (1/2)", NamedTextColor.DARK_BLUE, TextDecoration.BOLD))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("월드보더", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("중심 (" + num(config.borderCenterX) + ", " + num(config.borderCenterZ) +
                                ")에서 반경 " + num(config.borderRadius) + "블록까지만 이동할 수 있습니다.",
                        NamedTextColor.BLACK))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("전투 상태", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("플레이어에게 피해를 입으면 전투 상태가 되며, 이 동안 로그아웃하면 " +
                                "사망 처리되어 인벤토리를 전부 바닥에 떨어뜨립니다. " +
                                config.combatDurationSeconds + "초간 피해를 입지 않으면 전투 상태가 해제됩니다.",
                        NamedTextColor.BLACK))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("인벤세이브", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("전투가 아닌 사망(추락, 익사 등) 시 모든 인벤토리를 잃는 대신, " +
                                "각 아이템이 " + num(config.naturalDropChance * 100) + "% 확률로 드랍됩니다.",
                        NamedTextColor.BLACK))
                .build();
    }

    private static Component rulesPageTwo(LobbyConfig config) {
        List<String> immunities = new ArrayList<>();
        if (config.dragonImmuneExplosion) immunities.add("폭발");
        if (config.dragonImmuneProjectile) immunities.add("원거리");
        String immunityText = immunities.isEmpty() ? "" : String.join("·", immunities) + " 공격에 면역이며, ";
        double regenSeconds = config.dragonRegenIntervalTicks / 20.0;

        TextComponent.Builder builder = Component.text()
                .append(Component.text("서버 규칙 (2/2)", NamedTextColor.DARK_BLUE, TextDecoration.BOLD))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("엔더 드래곤", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("엔더 드래곤의 죽음은 SMP 서버의 꽃이자 재앙입니다. 게임의 진행 속도를 조절하기 위해 아래와 같은 조정이 적용됩니다.",
                        NamedTextColor.BLACK))
                .append(Component.newline())
                .append(Component.text("체력 " + num(config.dragonMaxHealth) + "로 증가됩니다.\n" +
                                immunityText + num(regenSeconds) + "초마다 체력 " + num(config.dragonRegenAmount) +
                                "을 회복합니다. 정면 승부를 준비하세요.",
                        NamedTextColor.BLACK))
                .append(Component.newline()).append(Component.newline());

        if (config.nametagEnabled) {
            builder.append(Component.text("이름표", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                    .append(Component.newline())
                    .append(Component.text("DataGSM으로 인증된 플레이어는 머리 위에 이름이 표시됩니다. " +
                                    "/student-info 명령어로 다른 플레이어의 정보를 확인할 수 있습니다.",
                            NamedTextColor.BLACK))
                    .append(Component.newline()).append(Component.newline());
        }

        return builder
                .append(Component.text("이 안내서는 언제든 /guide 명령어로 다시 볼 수 있습니다.",
                        NamedTextColor.DARK_GRAY))
                .build();
    }

    /** 정수 값이면 소수점 없이 표시(예: 5000.0 → "5000", 0.3 → "0.3"). smp-server GuideBook.num()과 동일. */
    private static String num(double value) {
        return value == (long) value ? Long.toString((long) value) : Double.toString(value);
    }
}
