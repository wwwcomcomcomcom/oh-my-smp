package iieiiergn.smpAuth.auth

import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks in-flight OAuth round-trips: maps the {@code state} parameter to the PKCE
 * code verifier so {@code /callback} can complete the exchange. Short-lived (5 min).
 */
class PendingStore(private val ttlSeconds: Long = 300) {

    private val random = SecureRandom()
    private val entries = ConcurrentHashMap<String, Entry>()

    private data class Entry(val codeVerifier: String, val expiresAt: Long)

    /** Create a new state token bound to [codeVerifier]. */
    fun create(codeVerifier: String): String {
        val state = newState()
        entries[state] = Entry(codeVerifier, now() + ttlSeconds * 1000)
        return state
    }

    /** Consume the state, returning its verifier if valid. Single-use. */
    fun consume(state: String?): String? {
        if (state == null) return null
        val entry = entries.remove(state) ?: return null
        return if (entry.expiresAt >= now()) entry.codeVerifier else null
    }

    fun evictExpired() {
        val cutoff = now()
        entries.entries.removeIf { it.value.expiresAt < cutoff }
    }

    private fun newState(): String {
        val bytes = ByteArray(24).also(random::nextBytes)
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun now() = System.currentTimeMillis()
}
