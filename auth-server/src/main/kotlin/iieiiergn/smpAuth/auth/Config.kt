package iieiiergn.smpAuth.auth

import java.io.File
import java.util.Properties

/**
 * Auth-server configuration. Resolved from (in order of precedence):
 *  1. environment variables (e.g. SMP_AUTH_SHARED_SECRET)
 *  2. a properties file (path via SMP_AUTH_CONFIG, default ./config.properties)
 *  3. built-in defaults
 *
 * Secrets must come from env or an un-committed config file — never hard-coded.
 */
data class Config(
    val port: Int,
    val publicBaseUrl: String,
    val redirectUri: String,
    val authorizationBaseUrl: String,
    val userInfoBaseUrl: String,
    val datagsmClientId: String,
    val datagsmClientSecret: String,
    val scope: String,
    val sharedSecret: String,
    val dbPath: String,
    val keyTtlSeconds: Long,
    val keyLength: Int,
) {
    companion object {
        fun load(): Config {
            val file = File(System.getenv("SMP_AUTH_CONFIG") ?: "config.properties")
            val props = Properties().apply {
                if (file.isFile) file.inputStream().use { load(it) }
            }

            fun get(key: String, env: String, default: String? = null): String {
                return System.getenv(env)
                    ?: props.getProperty(key)
                    ?: default
                    ?: error("Missing required config '$key' (env $env)")
            }

            val publicBaseUrl = get("server.publicBaseUrl", "SMP_AUTH_PUBLIC_BASE_URL", "http://localhost:8080")
            return Config(
                port = get("server.port", "SMP_AUTH_PORT", "8080").toInt(),
                publicBaseUrl = publicBaseUrl,
                redirectUri = get("oauth.redirectUri", "SMP_AUTH_REDIRECT_URI", "$publicBaseUrl/callback"),
                authorizationBaseUrl = get(
                    "datagsm.authorizationBaseUrl", "SMP_AUTH_DATAGSM_AUTH_URL",
                    "https://oauth.authorization.datagsm.kr",
                ),
                userInfoBaseUrl = get(
                    "datagsm.userInfoBaseUrl", "SMP_AUTH_DATAGSM_RESOURCE_URL",
                    "https://oauth.resource.datagsm.kr",
                ),
                datagsmClientId = get("datagsm.clientId", "SMP_AUTH_DATAGSM_CLIENT_ID"),
                datagsmClientSecret = get("datagsm.clientSecret", "SMP_AUTH_DATAGSM_CLIENT_SECRET"),
                scope = get("datagsm.scope", "SMP_AUTH_DATAGSM_SCOPE", "self:read"),
                sharedSecret = get("security.sharedSecret", "SMP_AUTH_SHARED_SECRET"),
                dbPath = get("db.path", "SMP_AUTH_DB_PATH", "smp-auth.db"),
                keyTtlSeconds = get("key.ttlSeconds", "SMP_AUTH_KEY_TTL", "300").toLong(),
                keyLength = get("key.length", "SMP_AUTH_KEY_LENGTH", "8").toInt(),
            )
        }
    }
}
