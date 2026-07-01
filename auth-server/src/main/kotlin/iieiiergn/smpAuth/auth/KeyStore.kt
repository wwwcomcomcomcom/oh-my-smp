package iieiiergn.smpAuth.auth

import iieiiergn.smpAuth.common.StudentData
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory store of issued auth keys. Keys are single-use, expire after [ttlSeconds],
 * and use an ambiguity-free charset. Nothing here is persisted — a lost key just expires.
 */
class KeyStore(private val length: Int, private val ttlSeconds: Long) {

    // No 0/O/1/I/L to avoid copy/paste confusion.
    private val charset = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray()
    private val random = SecureRandom()
    private val entries = ConcurrentHashMap<String, Entry>()

    private data class Entry(val student: StudentData, val expiresAt: Long)

    /** Issue a fresh key bound to [student]. */
    fun issue(student: StudentData): String {
        // NB: capture `length` in a local — inside buildString{} the receiver is a
        // StringBuilder whose own `length` property (0) would otherwise shadow this one.
        val keyLength = length
        var key: String
        do {
            key = buildString { repeat(keyLength) { append(charset[random.nextInt(charset.size)]) } }
        } while (entries.putIfAbsent(key, Entry(student, now() + ttlSeconds * 1000)) != null)
        return key
    }

    /** Consume a key, returning its student if valid and unexpired. Single-use. */
    fun consume(key: String?): StudentData? {
        if (key == null) return null
        val entry = entries.remove(key.trim().uppercase()) ?: return null
        return if (entry.expiresAt >= now()) entry.student else null
    }

    /** Drop expired entries; call periodically. */
    fun evictExpired() {
        val cutoff = now()
        entries.entries.removeIf { it.value.expiresAt < cutoff }
    }

    private fun now() = System.currentTimeMillis()
}
