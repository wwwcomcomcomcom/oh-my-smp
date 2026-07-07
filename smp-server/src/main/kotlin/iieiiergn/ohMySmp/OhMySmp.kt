package iieiiergn.ohMySmp

import iieiiergn.ohMySmp.border.BorderManager
import iieiiergn.ohMySmp.combat.CombatDisplay
import iieiiergn.ohMySmp.combat.CombatListener
import iieiiergn.ohMySmp.combat.CombatManager
import iieiiergn.ohMySmp.config.PluginConfig
import iieiiergn.ohMySmp.dragon.DragonListener
import iieiiergn.ohMySmp.dragon.DragonManager
import iieiiergn.ohMySmp.guide.GuideBroadcaster
import iieiiergn.ohMySmp.guide.GuideCommand
import iieiiergn.ohMySmp.listener.DeathListener
import iieiiergn.ohMySmp.listener.RespawnListener
import iieiiergn.ohMySmp.nametag.NametagListener
import iieiiergn.ohMySmp.nametag.NametagManager
import iieiiergn.ohMySmp.nametag.StudentInfoCommand
import iieiiergn.ohMySmp.tpa.TpaCommand
import iieiiergn.ohMySmp.tpa.TpaManager
import org.bukkit.plugin.java.JavaPlugin

class OhMySmp : JavaPlugin() {

    private lateinit var dragonManager: DragonManager
    private lateinit var combatDisplay: CombatDisplay
    private lateinit var guideBroadcaster: GuideBroadcaster
    private var nametagManager: NametagManager? = null

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

        // 6. SmpAuth 연동: 학생 이름표 + /student-info (SmpAuth 플러그인이 있을 때만)
        if (pm.getPlugin("SmpAuth") != null) {
            if (pluginConfig.nametagEnabled) {
                val manager = NametagManager()
                nametagManager = manager
                pm.registerEvents(NametagListener(manager), this)
            }
            getCommand("student-info")?.let {
                val executor = StudentInfoCommand()
                it.setExecutor(executor)
                it.tabCompleter = executor
            }
        } else {
            logger.warning("SmpAuth 플러그인이 없어 학생 이름표/명령어 기능을 건너뜁니다.")
        }

        // 7. 서버 규칙 안내서
        getCommand("guide")?.setExecutor(GuideCommand(pluginConfig, nametagManager != null))
        guideBroadcaster = GuideBroadcaster(this, pluginConfig)
        guideBroadcaster.start()

        // 8. TPA: 컴뱃 중에는 사용 불가
        val tpaManager = TpaManager()
        getCommand("tpa")?.let {
            val executor = TpaCommand(combatManager, tpaManager)
            it.setExecutor(executor)
            it.tabCompleter = executor
        }

        logger.info("oh-my-smp 활성화 완료.")
    }

    override fun onDisable() {
        if (::dragonManager.isInitialized) {
            dragonManager.stop()
        }
        if (::combatDisplay.isInitialized) {
            combatDisplay.stop()
        }
        if (::guideBroadcaster.isInitialized) {
            guideBroadcaster.stop()
        }
        nametagManager?.clearAll()
    }
}
