package xyz.skyjoshua.valourIntegration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import de.myzelyam.api.vanish.VanishAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import xyz.skyjoshua.valourIntegration.listeners.*;
import xyz.skyjoshua.valourIntegration.models.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public final class ValourIntegration extends JavaPlugin {

    private HubConnection _signalR;
    private FileConfiguration _config;
    public HttpClient http;
    public final Gson Gson = new GsonBuilder().create();
    public final String BaseUrl = "https://api.valour.gg/api/";
    public final String _hubUrl = "https://api.valour.gg/hubs/core";

    private String _primaryNode;
    private JsonObject _user;
    private JsonObject _member;

    private String[] BaseHeaders;

    public boolean hasVanish;


    public @Nullable String ValourAuth;
    public long ChannelId;
    public long PlanetId;

    public HashMap<String, User> UserIdMap = new HashMap<String, User>();

    private User GetCachedUser(long userId) {
        return UserIdMap.getOrDefault(String.valueOf(userId), null);
    }

    private void SetCachedUser(User user) {
        UserIdMap.put(String.valueOf(user.id), user);
    }

    public void LogToConsole(String message) {
        getLogger().info(message);
    }

    @Override
    public void onLoad() {
        LogToConsole("ValourIntegration has been loaded");
    }

    @Override
    public void onEnable() {

        LogToConsole("Setting up config.");
        SetupConfig();
        LogToConsole("Attempting to login.");
        SetupHttp();

        LogToConsole("ValourIntegration has been enabled.");
        LogToConsole("Connecting to SignalR...");
        SetupSignalR();

        if (Bukkit.getPluginManager().getPlugin("SuperVanish") != null) {
            hasVanish = true;
        }

        if (Bukkit.getPluginManager().getPlugin("PremiumVanish") != null) {
            hasVanish = true;
        }

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new LeaveListener(this), this);
        getServer().getPluginManager().registerEvents(new AdvancementListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new ServerLoadListener(this), this);

        // SuperVanish/PremiumVanish Support
        if (hasVanish) {
            getServer().getPluginManager().registerEvents(new VanishListener(this), this);
        }

    }

    @Override
    public void onDisable() {
        LogToConsole("ValourIntegration has been disabled.");
        ServerStopMessage();
    }

    public void ServerStopMessage() {
        var message = getConfig().getString("serverStop");

        SendValourMessage(message).thenAccept(result -> {
            if (!result.Success) {
                LogToConsole("Error sending Valour message");
                LogToConsole(result.Message);
            }
        });
    }

    private void SetupConfig(){
        saveDefaultConfig();
        _config = getConfig();

        ValourAuth = _config.getString("botToken");
        ChannelId = _config.getLong("channelId");
        PlanetId = _config.getLong("planetId");
    }

    public CompletableFuture<TaskResult> SendValourMessage(String content) {

        PlanetMessage message = new PlanetMessage();
        message.planetId = PlanetId;
        message.channelId = ChannelId;
        message.authorUserId = _user.get("id").getAsLong();
        message.authorMemberId = _member.get("id").getAsLong();
        message.fingerprint = UUID.randomUUID().toString();
        message.content = content;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(BaseUrl + "messages"))
                    .headers(BaseHeaders)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(Gson.toJson(message)))
                    .build();

            return http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply((result) -> {
                        var res = new TaskResult();
                        res.Message = result.body();

                        if (result.statusCode() == 200) {
                            res.Success = true;
                        }

                        return res;
                    });

        } catch (Exception ex) {
            LogToConsole("Error sending message to Valour");
            LogToConsole(ex.getMessage());

            var result = new TaskResult();
            result.Message = "Error sending message to Valour";
            return CompletableFuture.supplyAsync(() -> { return result; });

        }
    }

    private void SetupHttp(){
        http = HttpClient.newBuilder().build();

        try {
            HttpRequest userRequest = HttpRequest.newBuilder()
                    .uri(new URI(BaseUrl + "users/me"))
                    .setHeader("authorization", ValourAuth)
                    .GET()
                    .build();

            var userResponse = http.send(userRequest, HttpResponse.BodyHandlers.ofString());
            if (userResponse.statusCode() != 200) {
                LogToConsole("Failed to get user, check token.");
            } else {
                _user = JsonParser.parseString(userResponse.body()).getAsJsonObject();
                String name = _user.get("nameAndTag").getAsString();
                LogToConsole("Logged in as: " + name);
            }

            HttpRequest nodeRequest = HttpRequest.newBuilder()
                    .uri(new URI(BaseUrl + "node/name"))
                    .setHeader("authorization", ValourAuth)
                    .GET()
                    .build();

            var nodeResponse = http.send(nodeRequest, HttpResponse.BodyHandlers.ofString());
            if (nodeResponse.statusCode() != 200) {
                LogToConsole("Failed to get primary node. Falling back on 'emma', please note this may cause issues.");
                _primaryNode = "emma";
            } else {
                _primaryNode = nodeResponse.body();
                LogToConsole("Primary node found: " + _primaryNode);
            }

            BaseHeaders = new String[]{
                    "authorization", ValourAuth,
                    "x-server-select", _primaryNode
            };

            HttpRequest memberRequest = HttpRequest.newBuilder()
                    .uri(new URI(BaseUrl + "members/me/" + PlanetId))
                    .headers(BaseHeaders)
                    .GET()
                    .build();

            var memberResponse = http.send(memberRequest, HttpResponse.BodyHandlers.ofString());
            if (memberResponse.statusCode() != 200) {
                LogToConsole("Failed to get member: " + memberResponse.statusCode());
            } else {
                _member = JsonParser.parseString(memberResponse.body()).getAsJsonObject();
                LogToConsole("Found member in planet. (ID: "+_member.get("id").getAsLong()+")");
            }
        } catch (Exception ex){
            LogToConsole("Error building auth request.");
            LogToConsole(ex.getMessage());
        }
    }

    private void SetupSignalR() {
        _signalR = HubConnectionBuilder.create(_hubUrl)
                .withHeader("authorization", ValourAuth)
                .withHeader("x-server-select", _primaryNode)
                .build();

        _signalR.onClosed((ex) -> {
            LogToConsole("Valour SignalR connection closed. Reconnecting in 5 seconds...");
            if (ex != null) LogToConsole(ex.getMessage());

            Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
                try {
                    reconnectSignalR();
                } catch (Exception e) {
                    LogToConsole("Failed to reconnect: " + e.getMessage());
                }
            }, 100L);
        });

        _signalR.on("Relay", this::OnValourMessage, PlanetMessage.class);

        _signalR.on("Channel-Watching-Update", (update) -> {
        }, ChannelWatchingUpdate.class);

        _signalR.on("Channel-CurrentlyTyping-Update", (update) -> {
        }, ChannelTypingUpdate.class);
    }

    private boolean _hasConnected = false;

    private void connectSignalR() {
        var startError = _signalR.start().blockingGet();
        if (startError != null) {
            LogToConsole("SignalR connection error: " + startError.getMessage());
            LogToConsole("Retrying in 5 seconds...");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {}
        }

        var authResult = _signalR.invoke(TaskResult.class, "authorize", ValourAuth).blockingGet();
        LogToConsole(authResult.Message);

        var joinResult = ConnectToChannel(ChannelId);
        if (!joinResult.Success) {
            LogToConsole("Failed to join channel: " + joinResult.Message);
        } else {
            LogToConsole("Rejoined channel " + ChannelId);
        }

        if (_hasConnected) {
            SendValourMessage(_config.getString("signalRReconnect"));
        }
    }

    private void reconnectSignalR() {
        try {
            _signalR.stop().blockingAwait();;
        } catch (Exception ignored) {
            connectSignalR();
        }
    }

    private boolean isVanished(Player player) {
        if (hasVanish) {
            return VanishAPI.getInvisiblePlayers().contains(player.getUniqueId());
        }
        return false;
    }

    private void OnValourMessage(PlanetMessage message) {
        try {
            if (message.authorUserId == _user.get("id").getAsLong()) return;

            var user = GetUserAsync(message.authorUserId).get();

            if (user.bot) return;

            if (message.content.startsWith("v/ip")) {
                SendValourMessage(_config.getString("ipCommand")
                        .replace("{ping}", "«@m-" + message.authorMemberId + "»")
                );
                return;
            }

            if (message.content.startsWith("v/source")) {
                SendValourMessage(_config.getString("sourceCommand")
                        .replace("{ping}", "«@m-" + message.authorMemberId + "»")
                );
                return;
            }

            if (message.content.startsWith("v/list")) {
                List<String> visiblePlayers = Bukkit.getOnlinePlayers().stream()
                        .filter(p -> !isVanished(p))
                        .map(Player::getName)
                        .collect(Collectors.toList());

                SendValourMessage(_config.getString("listCommand")
                        .replace("{ping}", "«@m-" + message.authorMemberId + "»")
                        .replace("{playercount}", ""+visiblePlayers.size())
                        .replace("{maxplayers}", ""+Bukkit.getMaxPlayers())
                        .replace("{players}", String.join(", ", visiblePlayers))
                );
                return;
            }

            Component msg = LegacyComponentSerializer.legacyAmpersand().deserialize(
                    _config.getString("minecraftChatMessage")
                            .replace("{name}", user.name)
                            .replace("{message}", message.content)
            );

            Bukkit.broadcast(msg);
        } catch (Exception ex) {
            LogToConsole("Error fetching user " + message.authorUserId);
            LogToConsole(ex.getMessage());
        }
    }

    public Future<User> GetUserAsync(long userId) {
        var cached = GetCachedUser(userId);
        if (cached != null) {
            return CompletableFuture.supplyAsync(() -> { return cached; });
        }

        try {
            HttpRequest authRequest = HttpRequest.newBuilder()
                    .uri(new URI(BaseUrl + "users/" + userId))
                    .headers(BaseHeaders)
                    .GET()
                    .build();

            return http.sendAsync(authRequest, HttpResponse.BodyHandlers.ofString())
                    .thenApply((result) -> {
                        var user = Gson.fromJson(result.body(), User.class);
                        SetCachedUser(user);
                        return user;
                    });
        } catch (Exception ex) {
            LogToConsole("Failed to fetch user " + userId);
            LogToConsole(ex.getMessage());
            return null;
        }
    }

    private TaskResult ConnectToChannel(long channelId) {
        var result = _signalR.invoke(TaskResult.class, "JoinChannel", channelId);
        return result.blockingGet();
    }
}
