package iieiiergn.ohMySmp.guide

import iieiiergn.ohMySmp.config.PluginConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta

/** `/guide` 안내서를 [PluginConfig]의 현재 수치로 만든다. 수치를 하드코딩하지 않는다. */
object GuideBook {

    fun build(config: PluginConfig, nametagActive: Boolean): ItemStack {
        val book = ItemStack(Material.WRITTEN_BOOK)
        val meta = book.itemMeta as BookMeta
        meta.title(Component.text("오미숲 서버 규칙"))
        meta.author(Component.text("oh-my-smp"))
        meta.pages(listOf(rulesPageOne(config), rulesPageTwo(config, nametagActive)))
        book.itemMeta = meta
        return book
    }

    private fun rulesPageOne(config: PluginConfig): Component =
        Component.text()
            .append(heading("서버 규칙 (1/2)"))
            .append(Component.newline()).append(Component.newline())
            .append(subHeading("월드보더"))
            .append(Component.newline())
            .append(body(
                "중심 (${num(config.borderCenterX)}, ${num(config.borderCenterZ)})에서 " +
                    "반경 ${num(config.borderRadius)}블록까지만 이동할 수 있습니다."
            ))
            .append(Component.newline()).append(Component.newline())
            .append(subHeading("전투 상태"))
            .append(Component.newline())
            .append(body(
                "플레이어에게 피해를 입으면 전투 상태가 되며, 이 동안 로그아웃하면 사망 처리됩니다. " +
                    "${config.combatDurationSeconds}초간 피해를 입지 않으면 전투 상태가 해제됩니다."
            ))
            .append(Component.newline()).append(Component.newline())
            .append(subHeading("인벤세이브"))
            .append(Component.newline())
            .append(body(
                "전투가 아닌 사망(추락, 익사 등) 시 모든 인벤토리를 잃는 대신, " +
                    "각 아이템이 ${num(config.naturalDropChance * 100)}% 확률로 드랍됩니다."
            ))
            .build()

    private fun rulesPageTwo(config: PluginConfig, nametagActive: Boolean): Component {
        val immunities = buildList {
            if (config.dragonImmuneExplosion) add("폭발")
            if (config.dragonImmuneProjectile) add("원거리")
        }
        val immunityText = if (immunities.isEmpty()) "" else "${immunities.joinToString("·")} 공격에 면역이며, "
        val regenSeconds = config.dragonRegenIntervalTicks / 20.0

        val builder = Component.text()
            .append(heading("서버 규칙 (2/2)"))
            .append(Component.newline()).append(Component.newline())
            .append(subHeading("엔더 드래곤"))
            .append(Component.newline())
            .append(body(
                "엔더 드래곤의 죽음은 SMP 서버의 꽃이자 재앙입니다. " +
                    "게임의 진행 속도를 조절하기 위해 아래와 같은 조정이 적용됩니다."
            ))
            .append(Component.newline())
            .append(body(
                "체력 ${num(config.dragonMaxHealth)}로 증가됩니다.\n" +
                    "$immunityText${num(regenSeconds)}초마다 체력 ${num(config.dragonRegenAmount)}을 " +
                    "회복합니다. 정면 승부를 준비하세요."
            ))
            .append(Component.newline()).append(Component.newline())

        if (nametagActive) {
            builder
                .append(subHeading("이름표"))
                .append(Component.newline())
                .append(body(
                    "DataGSM으로 인증된 플레이어는 머리 위에 이름이 표시됩니다. " +
                        "/student-info 명령어로 다른 플레이어의 정보를 확인할 수 있습니다."
                ))
                .append(Component.newline()).append(Component.newline())
        }

        return builder
            .append(Component.text("이 안내서는 언제든 /guide 명령어로 다시 볼 수 있습니다.", NamedTextColor.DARK_GRAY))
            .build()
    }

    private fun heading(text: String): Component =
        Component.text(text, NamedTextColor.DARK_BLUE, TextDecoration.BOLD)

    private fun subHeading(text: String): Component =
        Component.text(text, NamedTextColor.DARK_RED, TextDecoration.BOLD)

    private fun body(text: String): Component =
        Component.text(text, NamedTextColor.BLACK)

    /** 정수 값이면 소수점 없이, 아니면 있는 그대로 표시한다(예: 5000.0 → "5000", 0.3 → "0.3"). */
    private fun num(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
