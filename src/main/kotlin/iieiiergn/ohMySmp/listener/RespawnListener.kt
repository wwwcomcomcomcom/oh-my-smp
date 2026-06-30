package iieiiergn.ohMySmp.listener

import iieiiergn.ohMySmp.config.PluginConfig
import iieiiergn.ohMySmp.spawn.SafeLocationFinder
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

/**
 * 침대/리스폰 정박기가 없는 경우 보더 내 랜덤 안전 위치에 스폰시킨다.
 *  - 첫 접속(PlayerJoinEvent + PDC 마커로 최초 여부 판별, 메인 스레드에서 안전하게 처리)
 *  - 침대/정박기 없는 사망 후 리스폰(PlayerRespawnEvent)
 *
 * 첫 스폰 위치는 Paper의 AsyncPlayerSpawnLocationEvent(비동기, 청크 접근 불가)나
 * deprecated된 Spigot PlayerSpawnLocationEvent 대신, 메인 스레드에서 도는
 * PlayerJoinEvent에서 텔레포트로 처리한다.
 */
class RespawnListener(plugin: Plugin, private val config: PluginConfig) : Listener {

    // 첫 스폰 처리 완료를 표시하는 영속 키.
    private val spawnedKey = NamespacedKey(plugin, "first-spawn-done")

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!config.randomOnFirstJoin) return
        val player = event.player
        val pdc = player.persistentDataContainer
        if (pdc.has(spawnedKey, PersistentDataType.BYTE)) return

        // 최초 접속: 마커를 남기고 랜덤 안전 위치로 이동.
        pdc.set(spawnedKey, PersistentDataType.BYTE, 1)
        randomLocation()?.let { player.teleport(it) }
    }

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) {
        // 침대나 정박기로 리스폰하는 경우는 그대로 둔다.
        if (event.isBedSpawn || event.isAnchorSpawn) return
        randomLocation()?.let { event.respawnLocation = it }
    }

    private fun randomLocation() =
        Bukkit.getWorld(config.borderWorld)?.let { world ->
            SafeLocationFinder.find(
                world,
                config.borderCenterX,
                config.borderCenterZ,
                config.borderRadius,
                config.respawnMaxAttempts,
            )
        }
}
