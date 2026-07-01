package iieiiergn.smpAuth.sample;

import iieiiergn.smpAuth.common.StudentData;
import iieiiergn.smpAuth.paperlib.AuthDataLoadedEvent;
import iieiiergn.smpAuth.paperlib.SmpAuth;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Demonstrates consuming SmpAuth on a content server:
 *  - greet players by name on join (reactive, via {@link AuthDataLoadedEvent})
 *  - {@code /whoami} prints the player's DataGSM info (polling {@link SmpAuth#get})
 *  - {@code /seniors} is gated to grade-3 students
 */
public final class SamplePlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onAuthLoaded(AuthDataLoadedEvent event) {
        Player player = event.getPlayer();
        if (event.isLinked()) {
            StudentData s = event.getData();
            player.sendMessage(Component.text(
                    "환영합니다, " + s.name() + " (" + s.grade() + "학년 " + s.classNum() + "반)!",
                    NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("DataGSM 계정이 연결되지 않았습니다.", NamedTextColor.GRAY));
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("플레이어만 사용할 수 있습니다.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "whoami" -> {
                Optional<StudentData> data = SmpAuth.get(player);
                if (data.isEmpty()) {
                    player.sendMessage(Component.text("아직 인증 정보가 없습니다.", NamedTextColor.YELLOW));
                    return true;
                }
                StudentData s = data.get();
                player.sendMessage(Component.text("이름: " + s.name(), NamedTextColor.AQUA));
                player.sendMessage(Component.text("학번: " + s.studentNumber()
                        + " (" + s.grade() + "-" + s.classNum() + "-" + s.number() + ")", NamedTextColor.AQUA));
                player.sendMessage(Component.text("전공: " + s.major(), NamedTextColor.AQUA));
                if (s.githubId() != null) {
                    player.sendMessage(Component.text("GitHub: " + s.githubId(), NamedTextColor.AQUA));
                }
                return true;
            }
            case "seniors" -> {
                Optional<StudentData> data = SmpAuth.get(player);
                if (data.isPresent() && Integer.valueOf(3).equals(data.get().grade())) {
                    player.sendMessage(Component.text("3학년 전용 구역에 오신 것을 환영합니다!", NamedTextColor.GOLD));
                } else {
                    player.sendMessage(Component.text("3학년만 입장할 수 있습니다.", NamedTextColor.RED));
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
