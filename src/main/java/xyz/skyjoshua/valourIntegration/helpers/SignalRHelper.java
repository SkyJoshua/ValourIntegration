package xyz.skyjoshua.valourIntegration.helpers;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import org.bukkit.Bukkit;
import xyz.skyjoshua.valourIntegration.ValourIntegration;
import xyz.skyjoshua.valourIntegration.models.ChannelTypingUpdate;
import xyz.skyjoshua.valourIntegration.models.ChannelWatchingUpdate;
import xyz.skyjoshua.valourIntegration.models.PlanetMessage;
import xyz.skyjoshua.valourIntegration.models.TaskResult;

public class SignalRHelper {

    public static void Setup(ValourIntegration plugin, HubConnection[] signalRRef, boolean[] hasConnected) {
        HubConnection signalR = HubConnectionBuilder.create(plugin._hubUrl)
                .withHeader("authorization", plugin.ValourAuth)
                .withHeader("x-server-select", plugin._primaryNode)
                .build();
        
        signalRRef[0] = signalR;
        
        signalR.onClosed((ex) -> {
            plugin.LogToConsole("Valour SignalR connection closed. Reconnecting in 5 seconds...");
            if (ex != null) plugin.LogToConsole(ex.getMessage());

            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                try {
                    Reconnect(plugin, signalRRef, hasConnected);
                } catch (Exception e) {
                    plugin.LogToConsole("Failed to reconnect: " + e.getMessage());
                }
            }, 100L);
        });

        signalR.on("Relay", (msg) -> plugin.OnValourMessage(msg), PlanetMessage.class);
        signalR.on("Channel-Watching-Update", (update) -> {}, ChannelWatchingUpdate.class);
        signalR.on("Channel-CurrentlyTyping-Update", (update) -> {}, ChannelTypingUpdate.class);

        Connect(plugin, signalRRef, hasConnected);
    }

    public static void Connect(ValourIntegration plugin, HubConnection[] signalRRef, boolean[] hasConnected) {
        HubConnection signalR = signalRRef[0];

        while (true) {
            var startError = signalR.start().blockingGet();
            if (startError == null) break;

            plugin.LogToConsole("SignalR connected failed: "+startError.getMessage());
            plugin.LogToConsole("Retrying in 5 seconds...");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {}
        }

        var authResult = signalR.invoke(TaskResult.class, "Authorize", plugin.ValourAuth).blockingGet();
        plugin.LogToConsole(authResult.Message);

        var joinResult = ConnectToChannel(plugin, signalRRef, plugin.ChannelId);
        if (!joinResult.Success) {
            plugin.LogToConsole("Failed to join channel: " + joinResult.Message);
        } else {
            plugin.LogToConsole("Rejoined channel " + plugin.ChannelId);
        }

        if (hasConnected[0]) {
            ValourMessage.SendAsync(plugin, plugin.getConfig().getString("signalRReconnect"));
        }

        hasConnected[0] = true;
    }

    public static void Reconnect(ValourIntegration plugin, HubConnection[] signalRRef, boolean[] hasConnected) {
        try {
            signalRRef[0].stop().blockingAwait();
        } catch (Exception ingored) {
            Connect(plugin, signalRRef, hasConnected);
        }
    }

    public static TaskResult ConnectToChannel(ValourIntegration plugin, HubConnection[] signalRRef, long channelId) {
        return signalRRef[0].invoke(TaskResult.class, "JoinChannel", channelId).blockingGet();
    }
}
