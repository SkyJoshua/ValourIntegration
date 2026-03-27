package xyz.skyjoshua.valourIntegration.helpers;

import xyz.skyjoshua.valourIntegration.ValourIntegration;
import xyz.skyjoshua.valourIntegration.models.User;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class UserHelper {

    public static Future<User> GetUserAsync(ValourIntegration plugin, long userId) {
        var cached = GetCachedUser(plugin, userId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(plugin.BaseUrl + "users/"+userId))
                    .headers(plugin.BaseHeaders)
                    .GET()
                    .build();

            return plugin.http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply((result) -> {
                        var user = plugin.Gson.fromJson(result.body(), User.class);
                        SetCachedUser(plugin, user);
                        return user;
                    });
        } catch (Exception ex) {
            plugin.LogToConsole("Failed to fetch user "+userId);
            plugin.LogToConsole(ex.getMessage());
            return null;
        }
    }

    public static User GetCachedUser(ValourIntegration plugin, long userId) {
        return plugin.UserIdMap.getOrDefault(String.valueOf(userId), null);
    }
    public static void SetCachedUser(ValourIntegration plugin, User user) {
        plugin.UserIdMap.put(String.valueOf(user.id), user);
    }
}