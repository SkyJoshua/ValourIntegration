package xyz.skyjoshua.valourIntegration.models;

import com.google.gson.JsonParser;
import xyz.skyjoshua.valourIntegration.ValourIntegration;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class PlanetMember {
    public long id;
    public User User;
    public long userId;
    public long planetId;
    public String nickname;
    public String memberAvatar;
    public PlanetRoleMembership roleMembership;

    public CompletableFuture<Boolean> HasPermission(ValourIntegration plugin, long permission) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(plugin.BaseUrl + "planets/"+planetId+"/roles"))
                    .headers(plugin.BaseHeaders)
                    .GET()
                    .build();

            return plugin.http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply((response) -> {
                        var roles = JsonParser.parseString(response.body()).getAsJsonArray();
                        long combined = 0L;

                        for (var roleElement : roles) {
                            var role = roleElement.getAsJsonObject();
                            int flagBitIndex = role.get("flagBitIndex").getAsInt();

                            if (roleMembership.hasRole(flagBitIndex)) {
                                if (role.get("isAdmin").getAsBoolean()) return true;
                                combined |= role.get("permissions").getAsLong();
                            }
                        }
                        return combined == -1L || (combined & permission) == permission;
                    });
        } catch (Exception ex) {
            plugin.LogToConsole("Error fetching roles: " + ex.getMessage());
            return CompletableFuture.completedFuture(false);
        }
    }
}
