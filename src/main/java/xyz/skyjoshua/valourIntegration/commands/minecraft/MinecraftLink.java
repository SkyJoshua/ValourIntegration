package xyz.skyjoshua.valourIntegration.commands.minecraft;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import xyz.skyjoshua.valourIntegration.ValourIntegration;
import org.apache.commons.lang3.RandomUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MinecraftLink implements CommandExecutor {

    private final ValourIntegration _valourIntegration;

    public MinecraftLink(ValourIntegration valourIntegration) {
        _valourIntegration = valourIntegration;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed as a player.");
            return true;
        }

        Player player = (Player) sender;

        var rand = RandomUtils.nextInt();
        var code = sender.getName() + "-" + rand;

        _valourIntegration.AddLinkCode(code, player.getUniqueId());

        Component msg = LegacyComponentSerializer.legacyAmpersand().deserialize(
                _valourIntegration.getConfig().getString("minceraftLinkMessage")
                        .replace("{prefix}", _valourIntegration.getConfig().getString("prefix"))
                        .replace("{code}", code))
                .clickEvent(ClickEvent.copyToClipboard(code))
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy code!")));

        player.sendMessage(msg);

        return true;
    }
}
