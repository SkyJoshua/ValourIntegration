package xyz.skyjoshua.valourIntegration.listeners;

import io.papermc.paper.advancement.AdvancementDisplay;
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

        AdvancementDisplay display = event.getAdvancement().getDisplay();
        if (display == null) return;
        if (!display.doesAnnounceToChat()) return;

        var title = PlainTextComponentSerializer.plainText().serialize(
                GlobalTranslator.render(display.title(), Locale.ENGLISH)
        );

        var message = _valourIntegration.getConfig().getString("advancementMessage")
                .replace("{name}", event.getPlayer().getName())
                .replace("{advancement}", title);

        _valourIntegration.SendValourMessage(message).thenAccept(result -> {
            if (!result.Success) {
                _valourIntegration.LogToConsole("Error sending Valour message");
                _valourIntegration.LogToConsole(result.Message);
            }
        });
    }
}
