package iieiiergn.ohMySmp.combat

import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * - 플레이어가 플레이어(또는 플레이어가 쏜 발사체)에게 피해를 입으면 양쪽 모두 Combat 태깅(최초 발동).
 * - 이미 Combat 상태인 플레이어는 어떤 형태의 데미지를 받아도 남은 시간이 갱신된다.
 * - Combat 상태에서 접속을 종료하면 컴뱃로그로 간주해 즉시 사망 처리(전체 드롭)한다.
 */
class CombatListener(private val combatManager: CombatManager) : Listener {

    @EventHandler(ignoreCancelled = true)
    fun onDamage(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val attacker = resolveAttacker(event) ?: return
        if (attacker.uniqueId == victim.uniqueId) return

        // 상호 전투: 가해자도 컴뱃로그 방지를 위해 함께 태깅.
        combatManager.tag(victim.uniqueId)
        combatManager.tag(attacker.uniqueId)
    }

    /**
     * 이미 Combat 상태인 플레이어가 다른 형태의 데미지(낙하·용암 등 포함)를 받으면
     * 남은 시간을 갱신한다. 최초 발동은 위 onDamage(플레이어 공격)에서만 이뤄진다.
     * EntityDamageByEntityEvent도 EntityDamageEvent 핸들러로 전달되므로
     * 여기서는 "이미 태깅된 경우"에만 갱신해 환경 데미지로 새로 발동되지 않도록 한다.
     */
    @EventHandler(ignoreCancelled = true)
    fun onAnyDamage(event: EntityDamageEvent) {
        val victim = event.entity as? Player ?: return
        if (combatManager.isTagged(victim.uniqueId)) {
            combatManager.tag(victim.uniqueId)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        if (!combatManager.isTagged(player.uniqueId)) return

        // 컴뱃로그 = 플레이어에 의한 사망. 인벤토리를 바닥에 드롭하고 사망 처리.
        val world = player.world
        val location = player.location
        for (item in player.inventory.contents) {
            if (item != null) {
                world.dropItemNaturally(location, item)
            }
        }
        player.inventory.clear()
        combatManager.clear(player.uniqueId)
        // 체력을 0으로 만들어 재접속 시 리스폰되도록 한다.
        player.health = 0.0
    }

    /** 직접 타격이면 플레이어 가해자, 발사체면 그 발사체를 쏜 플레이어를 반환. */
    private fun resolveAttacker(event: EntityDamageByEntityEvent): Player? {
        (event.damager as? Player)?.let { return it }
        val projectile = event.damager as? Projectile ?: return null
        return projectile.shooter as? Player
    }
}
