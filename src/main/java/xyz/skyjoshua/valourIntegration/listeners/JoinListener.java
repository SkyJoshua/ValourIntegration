package xyz.skyjoshua.valourIntegration.listeners;

import de.myzelyam.api.vanish.VanishAPI;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import xyz.skyjoshua.valourIntegration.ValourIntegration;
import xyz.skyjoshua.valourIntegration.helpers.ValourMessage;


public class JoinListener implements Listener {

    private final ValourIntegration _valourIntegration;

    public JoinListener(ValourIntegration valourIntegration) {
        _valourIntegration = valourIntegration;
    }

    @EventHandler
    public void OnPlayerJoin(PlayerJoinEvent event) {
        var message = _valourIntegration.getConfig().getString("joinMessage")
                .replace("{name}", event.getPlayer().getName());

        if (Bukkit.getPluginManager().getPlugin("PremiumVanish") != null) {
            if (VanishAPI.getInvisiblePlayers().contains(event.getPlayer().getUniqueId())) {
                return;
            }
        }

        ValourMessage.SendAsync(_valourIntegration, message).thenAccept(result -> {
            if (!result.Success) {
                _valourIntegration.LogToConsole("Error sending Valour message");
                _valourIntegration.LogToConsole(result.Message);
            }
        });
    }
}
