package iieiiergn.ohMySmp.spawn

import org.bukkit.HeightMap
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Tag
import org.bukkit.World
import kotlin.random.Random

/**
 * 보더 내부에서 "밟을 수 있는" 랜덤 안전 위치를 찾는다.
 * 바다(수면)·공중·용암·잎 등 unsteppable한 표면은 거부한다.
 *
 * Bukkit/Paper에는 랜덤 안전 스폰을 한 번에 주는 유틸이 없으므로
 * World#getHighestBlockAt(HeightMap) + 표면 블록 검사로 직접 구현한다.
 */
object SafeLocationFinder {

    // 표면이 이 블록이면 거부(서 있을 수 없거나 위험).
    private val UNSAFE_SURFACE = setOf(
        Material.WATER,
        Material.LAVA,
        Material.MAGMA_BLOCK,
        Material.CACTUS,
        Material.FIRE,
        Material.POWDER_SNOW,
    )

    fun find(
        world: World,
        centerX: Double,
        centerZ: Double,
        radius: Double,
        maxAttempts: Int,
    ): Location? {
        repeat(maxAttempts) {
            val x = (centerX + Random.nextDouble(-radius, radius)).toInt()
            val z = (centerZ + Random.nextDouble(-radius, radius)).toInt()

            val surface = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES)
            val type = surface.type

            // 발을 디딜 표면이 고형이고 위험/물/공중이 아니어야 한다.
            if (!type.isSolid) return@repeat
            if (type in UNSAFE_SURFACE) return@repeat
            if (Tag.LEAVES.isTagged(type)) return@repeat

            // 머리 공간 2칸이 비어 있어야 한다(질식/끼임 방지).
            val above = surface.getRelative(0, 1, 0)
            val aboveHead = surface.getRelative(0, 2, 0)
            if (!above.isPassable || !aboveHead.isPassable) return@repeat

            return surface.location.add(0.5, 1.0, 0.5)
        }
        return null
    }
}
