package iieiiergn.ohMySmp.tpa

import iieiiergn.ohMySmp.combat.CombatManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

/**
 * `/tpa <플레이어>` — 대상에게 텔레포트 요청을 보낸다(수락 시 요청자가 대상 위치로 이동).
 * `/tpa accept|deny` — 받은 요청을 수락/거절. `/tpa help` — 사용법.
 *
 * 컴뱃 상태에서는 요청 전송·수락 모두 불가하며, 수락 시점에 요청자가 컴뱃이면
 * 텔레포트가 취소된다(컴뱃로그 회피 방지).
 */
class TpaCommand(
    private val combat: CombatManager,
    private val tpa: TpaManager,
) : CommandExecutor, TabCompleter {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage(Component.text("플레이어만 사용할 수 있습니다.", NamedTextColor.RED))
            return true
        }
        if (args.isEmpty() || args[0].equals("help", ignoreCase = true)) {
            sendHelp(sender, label)
            return true
        }
        return when (args[0].lowercase()) {
            "accept" -> handleAccept(sender)
            "deny" -> handleDeny(sender)
            else -> handleRequest(sender, args[0])
        }
    }

    /** `/tpa <플레이어>` — 대상에게 요청 전송. */
    private fun handleRequest(sender: Player, targetName: String): Boolean {
        if (combat.isTagged(sender.uniqueId)) {
            sender.sendMessage(Component.text("컴뱃 중에는 TPA를 사용할 수 없습니다.", NamedTextColor.RED))
            return true
        }

        val target = Bukkit.getPlayerExact(targetName)
        if (target == null) {
            sender.sendMessage(
                Component.text("'$targetName' 플레이어를 찾을 수 없습니다(온라인이어야 합니다).", NamedTextColor.RED)
            )
            return true
        }
        if (target.uniqueId == sender.uniqueId) {
            sender.sendMessage(Component.text("자신에게는 요청할 수 없습니다.", NamedTextColor.RED))
            return true
        }

        tpa.request(sender.uniqueId, target.uniqueId)

        sender.sendMessage(
            Component.text("${target.name} 님에게 텔레포트 요청을 보냈습니다. (${tpa.expireSeconds}초 내 응답)", NamedTextColor.GREEN)
        )
        target.sendMessage(
            Component.text("${sender.name} 님이 당신에게 텔레포트를 요청했습니다.", NamedTextColor.AQUA)
        )
        target.sendMessage(
            Component.text("수락: /tpa accept  ·  거절: /tpa deny  (${tpa.expireSeconds}초 내)", NamedTextColor.GRAY)
        )
        return true
    }

    /** `/tpa accept` — 받은 요청 수락 후 요청자를 내 위치로 이동. */
    private fun handleAccept(sender: Player): Boolean {
        if (combat.isTagged(sender.uniqueId)) {
            sender.sendMessage(Component.text("컴뱃 중에는 TPA를 사용할 수 없습니다.", NamedTextColor.RED))
            return true
        }

        val requesterId = tpa.consume(sender.uniqueId)
        if (requesterId == null) {
            sender.sendMessage(Component.text("수락할 텔레포트 요청이 없습니다.", NamedTextColor.RED))
            return true
        }

        val requester = Bukkit.getPlayer(requesterId)
        if (requester == null) {
            sender.sendMessage(Component.text("요청자가 오프라인이라 텔레포트할 수 없습니다.", NamedTextColor.RED))
            return true
        }
        if (combat.isTagged(requester.uniqueId)) {
            sender.sendMessage(Component.text("${requester.name} 님이 컴뱃 중이라 텔레포트가 취소되었습니다.", NamedTextColor.RED))
            requester.sendMessage(Component.text("컴뱃 중이라 텔레포트가 취소되었습니다.", NamedTextColor.RED))
            return true
        }

        requester.teleport(sender.location)
        sender.sendMessage(Component.text("${requester.name} 님의 텔레포트 요청을 수락했습니다.", NamedTextColor.GREEN))
        requester.sendMessage(Component.text("${sender.name} 님이 요청을 수락했습니다. 텔레포트합니다.", NamedTextColor.GREEN))
        return true
    }

    /** `/tpa deny` — 받은 요청 거절. */
    private fun handleDeny(sender: Player): Boolean {
        val requesterId = tpa.consume(sender.uniqueId)
        if (requesterId == null) {
            sender.sendMessage(Component.text("거절할 텔레포트 요청이 없습니다.", NamedTextColor.RED))
            return true
        }

        sender.sendMessage(Component.text("텔레포트 요청을 거절했습니다.", NamedTextColor.YELLOW))
        Bukkit.getPlayer(requesterId)?.sendMessage(
            Component.text("${sender.name} 님이 텔레포트 요청을 거절했습니다.", NamedTextColor.YELLOW)
        )
        return true
    }

    private fun sendHelp(sender: Player, label: String) {
        sender.sendMessage(Component.text("── TPA 사용법 ──", NamedTextColor.AQUA))
        sender.sendMessage(help("/$label <플레이어>", "대상에게 텔레포트 요청을 보냅니다(수락 시 내가 대상에게 이동)."))
        sender.sendMessage(help("/$label accept", "받은 요청을 수락합니다."))
        sender.sendMessage(help("/$label deny", "받은 요청을 거절합니다."))
        sender.sendMessage(help("/$label help", "이 도움말을 표시합니다."))
        sender.sendMessage(
            Component.text("※ 요청은 ${tpa.expireSeconds}초 후 만료되며, 컴뱃 중에는 사용할 수 없습니다.", NamedTextColor.GRAY)
        )
    }

    private fun help(usage: String, desc: String): Component =
        Component.text(usage, NamedTextColor.YELLOW)
            .append(Component.text(" - $desc", NamedTextColor.GRAY))

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>,
    ): List<String> {
        if (args.size != 1) return emptyList()
        val prefix = args[0].lowercase()
        val candidates = mutableListOf("accept", "deny", "help")
        Bukkit.getOnlinePlayers().mapTo(candidates) { it.name }
        return candidates.filter { it.lowercase().startsWith(prefix) }
    }
}
