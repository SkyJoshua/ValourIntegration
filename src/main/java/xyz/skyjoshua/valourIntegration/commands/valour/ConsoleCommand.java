package xyz.skyjoshua.valourIntegration.commands.valour;

import org.bukkit.Bukkit;
import xyz.skyjoshua.valourIntegration.ValourIntegration;
import xyz.skyjoshua.valourIntegration.helpers.MemberHelper;
import xyz.skyjoshua.valourIntegration.helpers.ValourMessage;
import xyz.skyjoshua.valourIntegration.models.PlanetMessage;
import xyz.skyjoshua.valourIntegration.models.PlanetPermissions;

import java.util.Arrays;
import java.util.Objects;

public class ConsoleCommand {

    public static void Execute(ValourIntegration plugin, PlanetMessage message) {
        Objects.requireNonNull(MemberHelper.GetMemberAsync(plugin, message.authorMemberId, message.planetId))
                .thenCompose(member -> member.HasPermission(plugin, PlanetPermissions.Manage))
                .thenAccept(hasPerm -> {
                    if (!hasPerm) {
                        String msg = plugin.getConfig().getString("noPerm")
                                .replace("{ping}", "«@m-" + message.authorMemberId + "»");
                        ValourMessage.ReplyAsync(plugin, msg, message.id);
                        return;
                    }

                    var args = message.content.split(" ");
                    if (args.length < 2) {
                        ValourMessage.ReplyAsync(plugin, "Please enter a command to send.", message.id);
                        return;
                    }

                    String cmd = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                    String msg = plugin.getConfig().getString("consoleCommand")
                            .replace("{ping}", "«@m-" + message.authorMemberId + "»")
                            .replace("{command}", cmd);

                    ValourMessage.ReplyAsync(plugin, msg, message.id);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    });
                });
    }
}
