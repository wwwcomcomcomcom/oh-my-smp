package iieiiergn.ohMySmp.listener

import iieiiergn.ohMySmp.combat.CombatManager
import iieiiergn.ohMySmp.config.PluginConfig
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import kotlin.random.Random

/**
 * 사망 시 키프인벤토리 로직:
 *  - 플레이어 살해 또는 Combat 상태에서의 죽음 -> 전체 드롭(바닐라 기본).
 *  - 그 외 자연사 -> 각 아이템 스택을 설정된 확률로 독립적으로 굴려 손실 여부 결정.
 */
class DeathListener(
    private val config: PluginConfig,
    private val combatManager: CombatManager,
) : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onDeath(event: PlayerDeathEvent) {
        val player = event.entity
        val killedByPlayer = player.killer != null
        val inCombat = combatManager.isTagged(player.uniqueId)

        // 어떤 경우든 사망하면 Combat 상태는 해제한다.
        combatManager.clear(player.uniqueId)

        if (killedByPlayer || inCombat) {
            // 플레이어에 의한 죽음 -> 전체 드롭. drops/itemsToKeep를 손대지 않는다.
            return
        }

        // 자연사 -> 확률 기반 손실.
        val dropChance = config.naturalDropChance
        val all = event.drops.toList()
        event.drops.clear()
        for (item in all) {
            if (Random.nextDouble() < dropChance) {
                event.drops.add(item)       // 손실(바닥에 드롭)
            } else {
                event.itemsToKeep.add(item) // 유지(부활 시 인벤토리로 복원)
            }
        }
    }
}
