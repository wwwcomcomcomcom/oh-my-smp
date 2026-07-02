package iieiiergn.ohMySmp.guide

import iieiiergn.ohMySmp.config.PluginConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/** 주기적으로 전체 채팅에 `/guide` 사용을 상기시킨다. */
class GuideBroadcaster(
    private val plugin: Plugin,
    private val config: PluginConfig,
) {

    private var task: BukkitTask? = null

    fun start() {
        val intervalTicks = config.guideBroadcastIntervalMinutes * TICKS_PER_MINUTE
        task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable { broadcast() }, intervalTicks, intervalTicks)
    }

    private fun broadcast() {
        Bukkit.broadcast(
            Component.text("서버 규칙이 궁금하다면 ", NamedTextColor.GRAY)
                .append(
                    Component.text("/guide", NamedTextColor.YELLOW, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/guide"))
                )
                .append(Component.text(" 명령어를 입력해보세요.", NamedTextColor.GRAY))
        )
    }

    fun stop() {
        task?.cancel()
        task = null
    }

    companion object {
        private const val TICKS_PER_MINUTE = 60L * 20L
    }
}
