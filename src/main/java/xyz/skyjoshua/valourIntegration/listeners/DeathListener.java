package xyz.skyjoshua.valourIntegration.listeners;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import xyz.skyjoshua.valourIntegration.ValourIntegration;

public class DeathListener implements Listener {

    private final ValourIntegration _valourIntegration;

    public DeathListener(ValourIntegration valourIntegration) {
        _valourIntegration = valourIntegration;
    }

    @EventHandler
    public void OnPlayerDeath(PlayerDeathEvent event) {

        var death = PlainTextComponentSerializer.plainText().serialize(event.deathMessage());

        var message = _valourIntegration.getConfig().getString("deathMessage")
                .replace("{name}", event.getPlayer().getName())
                .replace("{reason}", death.replace(event.getPlayer().getName() + " ", ""));

        _valourIntegration.SendValourMessage(message).thenAccept(result -> {
            if (!result.Success) {
                _valourIntegration.LogToConsole("Error sending Valour message");
                _valourIntegration.LogToConsole(result.Message);
            }
        });
    }
}
