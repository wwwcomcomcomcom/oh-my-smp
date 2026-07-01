package iieiiergn.ohMySmp.nametag

import iieiiergn.smpAuth.paperlib.AuthDataLoadedEvent
import iieiiergn.smpAuth.paperlib.SmpAuth
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * SmpAuth 인증 데이터가 도착하면 학번 이름표를 적용하고, 퇴장 시 정리한다.
 */
class NametagListener(private val manager: NametagManager) : Listener {

    // 인증 데이터가 Velocity 로부터 도착하는 순간 적용(정상 경로).
    @EventHandler
    fun onAuthLoaded(event: AuthDataLoadedEvent) {
        if (!event.isLinked) return
        event.data?.let { manager.apply(event.player, it) }
    }

    // 접속 시점에 이미 캐시된 데이터가 있으면 적용(플러그인 리로드/재접속 대비).
    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        SmpAuth.get(event.player).ifPresent { manager.apply(event.player, it) }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        manager.clear(event.player)
    }
}
