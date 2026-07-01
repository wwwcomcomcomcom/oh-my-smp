package iieiiergn.ohMySmp.border

import iieiiergn.ohMySmp.config.PluginConfig
import org.bukkit.Bukkit
import java.util.logging.Logger

/**
 * 서버 시작 시 설정된 반지름에 맞춰 월드 보더를 적용한다.
 */
class BorderManager(private val config: PluginConfig, private val logger: Logger) {

    fun apply() {
        val world = Bukkit.getWorld(config.borderWorld)
        if (world == null) {
            logger.warning("border: '${config.borderWorld}' 월드를 찾을 수 없어 보더를 적용하지 못했습니다.")
            return
        }

        val border = world.worldBorder
        border.setCenter(config.borderCenterX, config.borderCenterZ)
        // 월드 보더 크기는 "지름" 단위이므로 반지름 * 2.
        border.size = config.borderRadius * 2.0

        logger.info(
            "border: '${world.name}' 월드에 보더 적용 " +
                "(center=${config.borderCenterX},${config.borderCenterZ}, radius=${config.borderRadius})"
        )
    }
}
