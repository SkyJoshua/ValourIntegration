package xyz.skyjoshua.valourIntegration.listeners;

import de.myzelyam.api.vanish.PlayerHideEvent;
import de.myzelyam.api.vanish.PlayerShowEvent;
import de.myzelyam.api.vanish.VanishAPI;
import de.myzelyam.api.vanish.VanishTargetChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import xyz.skyjoshua.valourIntegration.ValourIntegration;

public class VanishListener implements Listener {

    private final ValourIntegration _valourIntegration;

    public VanishListener(ValourIntegration valourIntegration) {
        _valourIntegration = valourIntegration;
    }

    @EventHandler
    public void OnVanish(PlayerHideEvent event) {

        if (!event.isSilent()) {
            var message = _valourIntegration.getConfig().getString("leaveMessage")
                    .replace("{name}", event.getPlayer().getName());

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

    @EventHandler
    public void OnAppear(PlayerShowEvent event) {

        if (!event.isSilent()) {
            var message = _valourIntegration.getConfig().getString("joinMessage")
                    .replace("{name}", event.getPlayer().getName());

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
}
