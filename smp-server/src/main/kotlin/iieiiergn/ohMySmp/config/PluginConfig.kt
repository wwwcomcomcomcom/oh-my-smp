package iieiiergn.ohMySmp.config

import org.bukkit.configuration.file.FileConfiguration

/**
 * config.yml 값을 타입드 프로퍼티로 노출하는 래퍼.
 * onEnable 시점에 한 번 읽어 보관한다.
 */
class PluginConfig(config: FileConfiguration) {

    // border
    val borderWorld: String = config.getString("border.world", "world")!!
    val borderRadius: Double = config.getDouble("border.radius", 5000.0)
    val borderCenterX: Double = config.getDouble("border.center-x", 0.0)
    val borderCenterZ: Double = config.getDouble("border.center-z", 0.0)

    // death
    val naturalDropChance: Double = config.getDouble("death.natural-drop-chance", 0.3)

    // combat
    val combatDurationSeconds: Long = config.getLong("combat.duration-seconds", 15L)

    // respawn
    val randomOnFirstJoin: Boolean = config.getBoolean("respawn.random-on-first-join", true)
    val respawnMaxAttempts: Int = config.getInt("respawn.max-attempts", 50)

    // nametag (SmpAuth 학번 이름표)
    val nametagEnabled: Boolean = config.getBoolean("nametag.enabled", true)

    // dragon
    val dragonMaxHealth: Double = config.getDouble("dragon.max-health", 1000.0)
    val dragonImmuneExplosion: Boolean = config.getBoolean("dragon.immune-explosion", true)
    val dragonImmuneProjectile: Boolean = config.getBoolean("dragon.immune-projectile", true)
    val dragonRegenAmount: Double = config.getDouble("dragon.regen-amount", 1.0)
    val dragonRegenIntervalTicks: Long = config.getLong("dragon.regen-interval-ticks", 20L)
}
