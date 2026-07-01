package iieiiergn.ohMySmp.nametag

import iieiiergn.smpAuth.common.StudentData
import iieiiergn.smpAuth.paperlib.SmpAuth
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

/**
 * `/student-info <player>` — 대상 온라인 플레이어의 SmpAuth 학생 정보를 요청자에게 표시한다.
 * 별도 권한이 없어 모든 유저가 사용할 수 있다.
 */
class StudentInfoCommand : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (args.size != 1) {
            sender.sendMessage(Component.text("사용법: /$label <플레이어>", NamedTextColor.YELLOW))
            return true
        }

        val target = Bukkit.getPlayerExact(args[0])
        if (target == null) {
            sender.sendMessage(
                Component.text("'${args[0]}' 플레이어를 찾을 수 없습니다(온라인이어야 합니다).", NamedTextColor.RED)
            )
            return true
        }

        val data = SmpAuth.get(target).orElse(null)
        if (data == null || !data.isStudent) {
            sender.sendMessage(Component.text("${target.name} 님의 학생 정보가 없습니다.", NamedTextColor.RED))
            return true
        }

        sender.sendMessage(Component.text("── ${target.name} 학생 정보 ──", NamedTextColor.AQUA))
        sender.sendMessage(line("이름", data.name()))
        sender.sendMessage(line("학번", studentId(data) ?: "-"))
        sender.sendMessage(line("학년/반/번호", "${data.grade()}학년 ${data.classNum()}반 ${data.number()}번"))
        data.major()?.let { sender.sendMessage(line("전공", it)) }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): List<String> {
        if (args.size != 1) return emptyList()
        val prefix = args[0].lowercase()
        return Bukkit.getOnlinePlayers()
            .map { it.name }
            .filter { it.lowercase().startsWith(prefix) }
    }

    /** grade·classNum(각 한 자리) + number(두 자리 패딩). 예) 2학년 3반 4번 → "2304". */
    private fun studentId(data: StudentData): String? {
        val grade = data.grade() ?: return null
        val classNum = data.classNum() ?: return null
        val number = data.number() ?: return null
        return "%d%d%02d".format(grade, classNum, number)
    }

    private fun line(label: String, value: String): Component =
        Component.text("$label: ", NamedTextColor.GRAY)
            .append(Component.text(value, NamedTextColor.WHITE))
}
