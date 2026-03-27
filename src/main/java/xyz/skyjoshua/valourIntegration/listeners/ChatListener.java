package xyz.skyjoshua.valourIntegration.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import xyz.skyjoshua.valourIntegration.ValourIntegration;
import xyz.skyjoshua.valourIntegration.helpers.ValourMessage;

import java.util.Locale;

public class ChatListener implements Listener {

    private final ValourIntegration _valourIntegration;

    public ChatListener(ValourIntegration valourIntegration) {
        _valourIntegration = valourIntegration;
    }



    @EventHandler
    public void OnMinecraftChat(AsyncChatEvent event) {

        var content = PlainTextComponentSerializer.plainText().serialize(
                GlobalTranslator.render(event.message(), Locale.ENGLISH)
        );

        var message = _valourIntegration.getConfig().getString("valourChatMessage")
                .replace("{name}", event.getPlayer().getName())
                .replace("{message}", content);


        ValourMessage.SendAsync(_valourIntegration, message).thenAccept(result -> {
            if (!result.Success) {
                _valourIntegration.LogToConsole("Error sending Valour message");
                _valourIntegration.LogToConsole(result.Message);
            }
        });
    }
}
