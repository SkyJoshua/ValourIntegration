package xyz.skyjoshua.valourIntegration.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import xyz.skyjoshua.valourIntegration.ValourIntegration;

public class ChatListener implements Listener {

    private final ValourIntegration _valourIntegration;

    public ChatListener(ValourIntegration valourIntegration) {
        _valourIntegration = valourIntegration;
    }

    @EventHandler
    public void OnMinecraftChat(AsyncPlayerChatEvent event) {
        var message = "<" + event.getPlayer().getPlayerListName() + "> " + event.getMessage();

        try {
            var task = _valourIntegration.SendValourMessage(message);
            var result = task.get();
            if (!result.Success) {
                _valourIntegration.LogToConsole("Error sending Valour message");
                _valourIntegration.LogToConsole(result.Message);
            }
        } catch (Exception ex) {
            _valourIntegration.LogToConsole("Error sending Valour message");
            _valourIntegration.LogToConsole(ex.getMessage());
        }
    }
}
