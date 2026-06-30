package iieiiergn.ohMySmp.dragon

import iieiiergn.ohMySmp.config.PluginConfig
import org.bukkit.Bukkit
import org.bukkit.attribute.Attribute
import org.bukkit.entity.EnderDragon
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import kotlin.math.min

/**
 * 엔더드래곤 강화: 최대 체력 상향, 주기적 체력 재생.
 */
class DragonManager(
    private val plugin: Plugin,
    private val config: PluginConfig,
) {

    private var regenTask: BukkitTask? = null

    /** 단일 드래곤을 강화한다(최대 체력 상향 + 풀피로 시작). */
    fun buff(dragon: EnderDragon) {
        dragon.getAttribute(Attribute.MAX_HEALTH)?.let { attr ->
            if (attr.baseValue != config.dragonMaxHealth) {
                attr.baseValue = config.dragonMaxHealth
                dragon.health = config.dragonMaxHealth
            }
        }
    }

    /** 이미 로드된 월드에 존재하는 드래곤을 강화한다(플러그인 활성화 시 1회). */
    fun buffExisting() {
        for (world in Bukkit.getWorlds()) {
            for (dragon in world.getEntitiesByClass(EnderDragon::class.java)) {
                buff(dragon)
            }
        }
    }

    /** 주기적으로 모든 드래곤의 체력을 회복하는 반복 태스크를 시작한다. */
    fun startRegenTask() {
        val interval = config.dragonRegenIntervalTicks
        regenTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            for (world in Bukkit.getWorlds()) {
                for (dragon in world.getEntitiesByClass(EnderDragon::class.java)) {
                    val maxHealth = dragon.getAttribute(Attribute.MAX_HEALTH)?.value ?: config.dragonMaxHealth
                    if (dragon.health < maxHealth) {
                        dragon.health = min(dragon.health + config.dragonRegenAmount, maxHealth)
                    }
                }
            }
        }, interval, interval)
    }

    fun stop() {
        regenTask?.cancel()
        regenTask = null
    }
}
