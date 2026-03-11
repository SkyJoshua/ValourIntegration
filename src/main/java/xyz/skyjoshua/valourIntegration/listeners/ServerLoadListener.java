package xyz.skyjoshua.valourIntegration.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import xyz.skyjoshua.valourIntegration.ValourIntegration;

public class ServerLoadListener implements Listener {

    private final ValourIntegration _valourIntegration;

    public ServerLoadListener(ValourIntegration valourIntegration) {
        _valourIntegration = valourIntegration;
    }

    @EventHandler
    public void OnServerLoad(ServerLoadEvent event) {
        var message = _valourIntegration.getConfig().getString("serverStart");

        _valourIntegration.SendValourMessage(message).thenAccept(result -> {
            if (!result.Success) {
                _valourIntegration.LogToConsole("Error sending Valour message");
                _valourIntegration.LogToConsole(result.Message);
            }
        });
    }
}
