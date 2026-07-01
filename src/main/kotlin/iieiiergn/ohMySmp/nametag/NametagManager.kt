package iieiiergn.ohMySmp.nametag

import iieiiergn.smpAuth.common.StudentData
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team

/**
 * 인증 서버에서 받아온 학생 실명을 플레이어 머리 위 이름표 앞(prefix)에 덧붙인다.
 *
 * 메인 스코어보드의 팀 prefix 를 사용하므로 별도 패킷 없이 모든 플레이어에게 표시된다.
 * 플레이어마다 전용 팀(`oms_<uuid앞12자리>`)을 만들어 관리한다.
 */
class NametagManager {

    private val scoreboard: Scoreboard = Bukkit.getScoreboardManager().mainScoreboard

    /** data 의 학생 실명을 player 이름표에 적용한다. 학생이 아니면 아무 것도 하지 않는다. */
    fun apply(player: Player, data: StudentData) {
        val name = studentName(data) ?: return
        val team = teamFor(player)
        // 이름표는 [실명] [MC이름] 형태가 되도록 뒤에 공백을 둔다.
        team.prefix(
            Component.text(name, NamedTextColor.AQUA).append(Component.text(" "))
        )
        if (!team.hasEntry(player.name)) {
            team.addEntry(player.name)
        }
    }

    /** player 전용 이름표 팀을 제거한다(퇴장 시). */
    fun clear(player: Player) {
        scoreboard.getTeam(teamName(player))?.unregister()
    }

    /** 이 매니저가 등록한 모든 이름표 팀을 정리한다(플러그인 비활성화 시). */
    fun clearAll() {
        for (team in scoreboard.teams.toList()) {
            if (team.name.startsWith(TEAM_PREFIX)) {
                team.unregister()
            }
        }
    }

    private fun teamFor(player: Player): Team {
        val name = teamName(player)
        return scoreboard.getTeam(name) ?: scoreboard.registerNewTeam(name)
    }

    // 팀 이름은 최대 16자 → "oms_"(4) + uuid 앞 12자리.
    private fun teamName(player: Player): String =
        TEAM_PREFIX + player.uniqueId.toString().replace("-", "").substring(0, 12)

    /** 학생 계정이면 실명, 아니면 null. */
    private fun studentName(data: StudentData): String? =
        if (data.isStudent) data.name() else null

    companion object {
        private const val TEAM_PREFIX = "oms_"
    }
}
