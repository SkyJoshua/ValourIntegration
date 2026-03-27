package xyz.skyjoshua.valourIntegration.commands.valour;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import xyz.skyjoshua.valourIntegration.ValourIntegration;
import xyz.skyjoshua.valourIntegration.helpers.MappingHelper;
import xyz.skyjoshua.valourIntegration.helpers.ValourMessage;
import xyz.skyjoshua.valourIntegration.models.PlanetMessage;

import static xyz.skyjoshua.valourIntegration.helpers.UserHelper.GetUserAsync;

public class ValourLink {

    public static boolean Execute(ValourIntegration plugin, PlanetMessage message) {
        var split = message.content.split(" ");
        if (split.length < 2) {
            ValourMessage.ReplyAsync(plugin, "Failed! Include your code.", message.id);
            return true;
        }

        if (!plugin._codeToUUID.containsKey(split[1])) {
            ValourMessage.ReplyAsync(plugin, "Failed! Code not found.", message.id);
            return true;
        }

        var uuid = plugin._codeToUUID.remove(split[1]);
        plugin.UUIDToValourMap.put(uuid, message.authorUserId);
        GetUserAsync(plugin, message.authorUserId);
        OfflinePlayer player = Bukkit.getPlayer(uuid) != null ? Bukkit.getPlayer(uuid) : Bukkit.getOfflinePlayer(uuid);

        var msg = plugin.getConfig().getString("valourLinkMessage")
                .replace("{name}", player.getName())
                .replace("{uuid}", uuid.toString());

        ValourMessage.ReplyAsync(plugin, msg, message.id);
        MappingHelper.SaveData(plugin);
        return true;
    }
}
