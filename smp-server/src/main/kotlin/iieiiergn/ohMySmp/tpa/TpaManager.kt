package iieiiergn.ohMySmp.tpa

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * TPA 요청 상태를 관리한다. 대상(target) UUID -> 최근 요청 정보.
 * 한 대상에게는 가장 최근 요청 하나만 유효하다(새 요청이 이전 요청을 덮어씀).
 * 상태 맵은 ConcurrentHashMap + 지연 만료(조회/소비 시 정리) 패턴(CombatManager 참고).
 */
class TpaManager {

    /** 수락 가능 기간(밀리초). 하드코딩 20초. */
    private val expireMillis = 20_000L

    private data class Request(val requester: UUID, val expiry: Long)

    // target UUID -> 요청
    private val pending = ConcurrentHashMap<UUID, Request>()

    /** 수락 가능 기간(초). 메시지 표시용. */
    val expireSeconds: Long get() = expireMillis / 1000L

    /** [requester]가 [target]에게 TPA 요청을 등록(기존 요청은 덮어씀)한다. */
    fun request(requester: UUID, target: UUID) {
        pending[target] = Request(requester, System.currentTimeMillis() + expireMillis)
    }

    /**
     * [target]에게 온 유효한 요청의 요청자 UUID를 반환하고 요청을 소비(제거)한다.
     * 없거나 만료됐으면 null.
     */
    fun consume(target: UUID): UUID? {
        val req = pending.remove(target) ?: return null
        if (System.currentTimeMillis() >= req.expiry) return null
        return req.requester
    }

    /** [target]에게 유효한 요청이 있으면 요청자 UUID(소비하지 않음). 만료 항목은 정리한다. */
    fun peek(target: UUID): UUID? {
        val req = pending[target] ?: return null
        if (System.currentTimeMillis() >= req.expiry) {
            pending.remove(target)
            return null
        }
        return req.requester
    }
}
