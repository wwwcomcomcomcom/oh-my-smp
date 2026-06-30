package iieiiergn.ohMySmp

import iieiiergn.ohMySmp.border.BorderManager
import iieiiergn.ohMySmp.combat.CombatDisplay
import iieiiergn.ohMySmp.combat.CombatListener
import iieiiergn.ohMySmp.combat.CombatManager
import iieiiergn.ohMySmp.config.PluginConfig
import iieiiergn.ohMySmp.dragon.DragonListener
import iieiiergn.ohMySmp.dragon.DragonManager
import iieiiergn.ohMySmp.listener.DeathListener
import iieiiergn.ohMySmp.listener.RespawnListener
import org.bukkit.plugin.java.JavaPlugin

class OhMySmp : JavaPlugin() {

    private lateinit var dragonManager: DragonManager
    private lateinit var combatDisplay: CombatDisplay

    override fun onEnable() {
        saveDefaultConfig()
        val pluginConfig = PluginConfig(config)

        // 1. 전역 보더 적용
        BorderManager(pluginConfig, logger).apply()

        // 2 & 4. Combat + 사망 처리
        val combatManager = CombatManager(pluginConfig)

        // Combat 액션바 카운트다운 표시
        combatDisplay = CombatDisplay(this, combatManager)
        combatDisplay.start()

        // 5. 엔더드래곤 강화
        dragonManager = DragonManager(this, pluginConfig)
        dragonManager.buffExisting()
        dragonManager.startRegenTask()

        // 리스너 등록
        val pm = server.pluginManager
        pm.registerEvents(CombatListener(combatManager), this)
        pm.registerEvents(DeathListener(pluginConfig, combatManager), this)
        pm.registerEvents(RespawnListener(this, pluginConfig), this)
        pm.registerEvents(DragonListener(pluginConfig, dragonManager), this)

        logger.info("oh-my-smp 활성화 완료.")
    }

    override fun onDisable() {
        if (::dragonManager.isInitialized) {
            dragonManager.stop()
        }
        if (::combatDisplay.isInitialized) {
            combatDisplay.stop()
        }
    }
}
