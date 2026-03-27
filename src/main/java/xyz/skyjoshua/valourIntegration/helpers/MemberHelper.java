package xyz.skyjoshua.valourIntegration.helpers;

import com.google.gson.JsonParser;
import xyz.skyjoshua.valourIntegration.ValourIntegration;
import xyz.skyjoshua.valourIntegration.models.PlanetMember;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class MemberHelper {

    public static CompletableFuture<PlanetMember> GetMemberAsync(ValourIntegration plugin, long memberId, long planetId) {
        var cached = GetCachedMember(plugin, memberId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        try {
            HttpRequest request = HttpRequest.newBuilder() // /api/members/byuser/{planetId}/{userId}
                    .uri (new URI(plugin.BaseUrl + "members/"+memberId))
                    .headers(plugin.BaseHeaders)
                    .GET()
                    .build();

            return plugin.http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply((result) -> {
                        var member = plugin.Gson.fromJson(result.body(), PlanetMember.class);
                        SetCachedMember(plugin, member);
                        return member;
                    });
        } catch (Exception ex) {
            plugin.LogToConsole("Failed to fetch member "+memberId);
            plugin.LogToConsole(ex.getMessage());
            return null;
        }
    }

    public static PlanetMember GetCachedMember(ValourIntegration plugin, long memberId) {
        return plugin.MemberIdMap.getOrDefault(String.valueOf(memberId), null);
    }

    public static void SetCachedMember(ValourIntegration plugin, PlanetMember member) {
        plugin.MemberIdMap.put(String.valueOf(member.id), member);
    }



}
