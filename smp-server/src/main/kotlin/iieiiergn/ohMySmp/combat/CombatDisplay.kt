package iieiiergn.ohMySmp.combat

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID
import kotlin.math.ceil

/**
 * Combat 상태인 플레이어에게 액션바로 남은 시간을 카운트다운으로 표시한다.
 * 전투가 끝나는 순간 한 번 "전투 종료"를 표시한다.
 */
class CombatDisplay(
    private val plugin: Plugin,
    private val combatManager: CombatManager,
) {

    private var task: BukkitTask? = null

    // 직전 틱에 액션바를 표시 중이던 플레이어(전투 종료 알림을 1회만 보내기 위함).
    private val showing = HashSet<UUID>()

    fun start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { tick() }, 0L, INTERVAL_TICKS)
    }

    private fun tick() {
        for (player in Bukkit.getOnlinePlayers()) {
            val remaining = combatManager.remainingMillis(player.uniqueId)
            if (remaining > 0L) {
                val seconds = ceil(remaining / 1000.0).toInt()
                player.sendActionBar(
                    Component.text("⚔ 전투 중 — ${seconds}초", NamedTextColor.RED)
                )
                showing.add(player.uniqueId)
            } else if (showing.remove(player.uniqueId)) {
                // 막 전투가 끝난 플레이어에게 1회 알림.
                player.sendActionBar(Component.text("전투 종료", NamedTextColor.GREEN))
            }
        }
        // 오프라인이 된 플레이어는 추적 집합에서 제거.
        if (showing.isNotEmpty()) {
            showing.retainAll(Bukkit.getOnlinePlayers().mapTo(HashSet()) { it.uniqueId })
        }
    }

    fun stop() {
        task?.cancel()
        task = null
        showing.clear()
    }

    companion object {
        // 0.5초마다 갱신 — 액션바가 사라지기 전에 다시 그려 항상 표시되도록 한다.
        private const val INTERVAL_TICKS = 10L
    }
}
