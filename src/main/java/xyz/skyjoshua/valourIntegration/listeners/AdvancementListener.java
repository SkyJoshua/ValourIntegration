package xyz.skyjoshua.valourIntegration.listeners;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import xyz.skyjoshua.valourIntegration.ValourIntegration;

import java.util.Locale;

public class AdvancementListener implements Listener {

    private final ValourIntegration _valourIntegration;

    public AdvancementListener(ValourIntegration valourIntegration) {
        _valourIntegration = valourIntegration;
    }

    @EventHandler
    public void OnAdvancementGet(PlayerAdvancementDoneEvent event) {

        var title = PlainTextComponentSerializer.plainText().serialize(
                GlobalTranslator.render(event.getAdvancement().getDisplay().title(), Locale.ENGLISH)
        );

        var message = _valourIntegration.getConfig().getString("advancementMessage")
                .replace("{name}", event.getPlayer().getName())
                .replace("{advancement}", title);

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
