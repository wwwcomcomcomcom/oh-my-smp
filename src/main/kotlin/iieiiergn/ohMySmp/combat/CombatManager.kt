package iieiiergn.ohMySmp.combat

import iieiiergn.ohMySmp.config.PluginConfig
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 플레이어별 Combat 상태(만료 시각)를 관리한다.
 * Combat이 활성화된 동안의 죽음은 "플레이어에 의한 죽음"으로 간주된다.
 */
class CombatManager(private val config: PluginConfig) {

    // UUID -> Combat 만료 시각(epoch millis)
    private val tagged = ConcurrentHashMap<UUID, Long>()

    /** 플레이어를 Combat 상태로 만들거나 만료 시각을 갱신한다. */
    fun tag(uuid: UUID) {
        tagged[uuid] = System.currentTimeMillis() + config.combatDurationSeconds * 1000L
    }

    /** 현재 Combat 상태인지 여부. 만료된 항목은 정리한다. */
    fun isTagged(uuid: UUID): Boolean {
        val expiry = tagged[uuid] ?: return false
        if (System.currentTimeMillis() >= expiry) {
            tagged.remove(uuid)
            return false
        }
        return true
    }

    /** 남은 Combat 시간(밀리초). 태깅되지 않았거나 만료된 경우 0. 만료 항목은 정리한다. */
    fun remainingMillis(uuid: UUID): Long {
        val expiry = tagged[uuid] ?: return 0L
        val remaining = expiry - System.currentTimeMillis()
        if (remaining <= 0L) {
            tagged.remove(uuid)
            return 0L
        }
        return remaining
    }

    /** Combat 상태 제거. */
    fun clear(uuid: UUID) {
        tagged.remove(uuid)
    }
}
