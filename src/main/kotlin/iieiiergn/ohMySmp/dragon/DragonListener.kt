package iieiiergn.ohMySmp.dragon

import iieiiergn.ohMySmp.config.PluginConfig
import org.bukkit.entity.EnderDragon
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.Listener

/**
 * - 엔더드래곤이 스폰되면 강화한다.
 * - 폭발/원거리 데미지를 면역 처리한다(설정 가능).
 */
class DragonListener(
    private val config: PluginConfig,
    private val dragonManager: DragonManager,
) : Listener {

    @EventHandler
    fun onSpawn(event: EntitySpawnEvent) {
        val dragon = event.entity as? EnderDragon ?: return
        dragonManager.buff(dragon)
    }

    @EventHandler(ignoreCancelled = true)
    fun onDamage(event: EntityDamageEvent) {
        if (event.entity !is EnderDragon) return

        val immune = when (event.cause) {
            EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,
            EntityDamageEvent.DamageCause.ENTITY_EXPLOSION -> config.dragonImmuneExplosion
            EntityDamageEvent.DamageCause.PROJECTILE -> config.dragonImmuneProjectile
            else -> false
        }

        if (immune) {
            event.isCancelled = true
        }
    }
}
