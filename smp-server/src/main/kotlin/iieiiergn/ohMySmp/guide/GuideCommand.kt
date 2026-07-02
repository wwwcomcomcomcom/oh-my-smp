package iieiiergn.ohMySmp.guide

import iieiiergn.ohMySmp.config.PluginConfig
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/** `/guide` — config.yml에 반영된 현재 서버 규칙 안내서를 펼친다. */
class GuideCommand(config: PluginConfig, nametagActive: Boolean) : CommandExecutor {

    private val book: ItemStack = GuideBook.build(config, nametagActive)

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
        sender.openBook(book)
        return true
    }
}
