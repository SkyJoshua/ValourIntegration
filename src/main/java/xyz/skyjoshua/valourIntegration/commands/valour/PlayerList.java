package xyz.skyjoshua.valourIntegration.commands.valour;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import xyz.skyjoshua.valourIntegration.ValourIntegration;
import xyz.skyjoshua.valourIntegration.helpers.ValourMessage;
import xyz.skyjoshua.valourIntegration.models.PlanetMessage;

import java.util.List;

public class PlayerList {

    public static void Execute(ValourIntegration plugin, PlanetMessage message) {
        List<String> visiblePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(p -> !plugin.isVanished(p))
                .map(Player::getName)
                .toList();

        String msg = plugin.getConfig().getString("listCommand")
                .replace("{ping}", "«@m-" + message.authorMemberId + "»")
                .replace("{playercount}", ""+visiblePlayers.size())
                .replace("{maxplayers}", ""+Bukkit.getMaxPlayers())
                .replace("{players}", String.join(", ", visiblePlayers));

        ValourMessage.ReplyAsync(plugin, msg, message.id
        );
    }
}
