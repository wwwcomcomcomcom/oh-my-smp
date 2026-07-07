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
        // 각 규칙을 개별 페이지로 둔다 — 한 페이지에 여러 규칙을 담으면 내용이 길어져 잘리기 때문.
        val pages = buildList {
            add(borderPage(config))
            add(combatPage(config))
            add(inventorySavePage(config))
            add(tpaPage())
            add(dragonPage(config))
            if (nametagActive) add(nametagPage())
            add(outroPage())
        }
        meta.pages(pages)
        book.itemMeta = meta
        return book
    }

    private fun borderPage(config: PluginConfig): Component =
        Component.text()
            .append(heading("월드보더"))
            .append(Component.newline()).append(Component.newline())
            .append(body(
                "중심 (${num(config.borderCenterX)}, ${num(config.borderCenterZ)})에서 " +
                    "반경 ${num(config.borderRadius)}블록까지만 이동할 수 있습니다."
            ))
            .build()

    private fun combatPage(config: PluginConfig): Component =
        Component.text()
            .append(heading("전투 상태"))
            .append(Component.newline()).append(Component.newline())
            .append(body(
                "플레이어에게 피해를 입으면 전투 상태가 되며, 이 동안 로그아웃하면 사망 처리되어 " +
                    "인벤토리를 전부 바닥에 떨어뜨립니다. " +
                    "${config.combatDurationSeconds}초간 피해를 입지 않으면 전투 상태가 해제됩니다."
            ))
            .build()

    private fun inventorySavePage(config: PluginConfig): Component =
        Component.text()
            .append(heading("인벤세이브"))
            .append(Component.newline()).append(Component.newline())
            .append(body(
                "전투가 아닌 사망(추락, 익사 등) 시에는 인벤토리 전체가 아니라, " +
                    "각 아이템마다 ${num(config.naturalDropChance * 100)}% 확률로 개별 판정하여 잃게 됩니다. " +
                    "잃지 않은 아이템은 부활 시 그대로 돌아옵니다."
            ))
            .build()

    private fun tpaPage(): Component =
        Component.text()
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
            .build()

    private fun dragonPage(config: PluginConfig): Component {
        val immunities = buildList {
            if (config.dragonImmuneExplosion) add("폭발")
            if (config.dragonImmuneProjectile) add("원거리")
        }
        val immunityText = if (immunities.isEmpty()) "" else "${immunities.joinToString("·")} 공격에 면역이며, "
        val regenSeconds = config.dragonRegenIntervalTicks / 20.0

        return Component.text()
            .append(heading("엔더 드래곤"))
            .append(Component.newline()).append(Component.newline())
            .append(body(
                "엔더 드래곤의 죽음은 SMP 서버의 꽃이자 재앙입니다. " +
                    "게임의 진행 속도를 조절하기 위해 아래와 같은 조정이 적용됩니다."
            ))
            .append(Component.newline()).append(Component.newline())
            .append(body(
                "체력 ${num(config.dragonMaxHealth)}로 증가됩니다.\n" +
                    "$immunityText${num(regenSeconds)}초마다 체력 ${num(config.dragonRegenAmount)}을 " +
                    "회복합니다. 정면 승부를 준비하세요."
            ))
            .build()
    }

    private fun nametagPage(): Component =
        Component.text()
            .append(heading("이름표"))
            .append(Component.newline()).append(Component.newline())
            .append(body(
                "DataGSM으로 인증된 플레이어는 머리 위에 이름이 표시됩니다. " +
                    "/student-info 명령어로 다른 플레이어의 정보를 확인할 수 있습니다."
            ))
            .build()

    private fun outroPage(): Component =
        Component.text()
            .append(Component.text("이 안내서는 언제든 /guide 명령어로 다시 볼 수 있습니다.", NamedTextColor.DARK_GRAY))
            .build()

    private fun heading(text: String): Component =
        Component.text(text, NamedTextColor.DARK_BLUE, TextDecoration.BOLD)

    private fun body(text: String): Component =
        Component.text(text, NamedTextColor.BLACK)

    /** 정수 값이면 소수점 없이, 아니면 있는 그대로 표시한다(예: 5000.0 → "5000", 0.3 → "0.3"). */
    private fun num(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
