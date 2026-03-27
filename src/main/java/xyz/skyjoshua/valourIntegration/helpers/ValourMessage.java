package xyz.skyjoshua.valourIntegration.helpers;

import xyz.skyjoshua.valourIntegration.ValourIntegration;
import xyz.skyjoshua.valourIntegration.models.PlanetMessage;
import xyz.skyjoshua.valourIntegration.models.TaskResult;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ValourMessage {

    public static CompletableFuture<TaskResult> SendAsync(ValourIntegration plugin, String content) {

        PlanetMessage message = new PlanetMessage();
        message.planetId = plugin.PlanetId;
        message.channelId = plugin.ChannelId;
        message.authorUserId = plugin._user.get("id").getAsLong();
        message.authorMemberId = plugin._member.get("id").getAsLong();
        message.fingerprint = UUID.randomUUID().toString();
        message.content = content;

        return MessageAsync(plugin, message);
    }

    public static CompletableFuture<TaskResult> ReplyAsync(ValourIntegration plugin, String content, long replyId) {

        PlanetMessage message = new PlanetMessage();
        message.planetId = plugin.PlanetId;
        message.channelId = plugin.ChannelId;
        message.authorUserId = plugin._user.get("id").getAsLong();
        message.authorMemberId = plugin._member.get("id").getAsLong();
        message.fingerprint = UUID.randomUUID().toString();
        message.content = content;
        message.replyToId = replyId;

        return MessageAsync(plugin, message);
    }

    private static CompletableFuture<TaskResult> MessageAsync(ValourIntegration plugin, PlanetMessage message) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(plugin.BaseUrl + "messages"))
                    .headers(plugin.BaseHeaders)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(plugin.Gson.toJson(message)))
                    .build();

            return plugin.http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply((result) -> {
                        var res = new TaskResult();
                        res.Message = result.body();

                        if (result.statusCode() == 200) {
                            res.Success = true;
                        }

                        return res;
                    });

        } catch (Exception ex) {
            plugin.LogToConsole("Error sending message to Valour");
            plugin.LogToConsole(ex.getMessage());

            var result = new TaskResult();
            result.Message = "Error sending message to Valour";
            return CompletableFuture.supplyAsync(() -> { return result; });

        }
    }
}
