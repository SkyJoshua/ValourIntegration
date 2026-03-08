package xyz.skyjoshua.valourIntegration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import xyz.skyjoshua.valourIntegration.listeners.ChatListener;
import xyz.skyjoshua.valourIntegration.models.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

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

        var connectResult = ConnectToChannel(ChannelId);
        if (!connectResult.Success) {
            LogToConsole("Failed to connect to channel " + ChannelId);
            LogToConsole("Chat cannot be received from Valour!");
            LogToConsole(connectResult.Message);
        } else {
            LogToConsole("Connected to channel " + ChannelId);
        }

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    @Override
    public void onDisable() {
        LogToConsole("ValourIntegration has been disabled.");
    }

    private void SetupConfig(){
        _config = this.getConfig();
//        _config.addDefault("valourEmail", "user@email.com");
//        _config.addDefault("valourPassword", "password");
        _config.addDefault("botToken", "bot-your-bot-token-here");
        _config.addDefault("channelId", 0);
        _config.addDefault("planetId", 0);
        _config.options().copyDefaults(true);

        ValourAuth = _config.getString("botToken");
        ChannelId = _config.getLong("channelId");
        PlanetId = _config.getLong("planetId");

        saveConfig();
    }

    public Future<TaskResult> SendValourMessage(String content) {

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
            LogToConsole("Valour SignalR connection closed. Reconnecting...");
            var restartError = _signalR.start().blockingGet();
            if (restartError != null) {
                LogToConsole(restartError.getMessage());
            }
        });

        var startError = _signalR.start().blockingGet();
        if (startError != null) {
            LogToConsole(startError.getMessage());
        }
        
        var authTask = _signalR.invoke(TaskResult.class, "Authorize", ValourAuth);
        var authResult = authTask.blockingGet();
        LogToConsole(authResult.Message);

        _signalR.on("Relay", this::OnValourMessage, PlanetMessage.class);

        _signalR.on("Channel-Watching-Update", (update) -> {
        }, ChannelWatchingUpdate.class);

        _signalR.on("Channel-CurrentlyTyping-Update", (update) -> {
        }, ChannelTypingUpdate.class);
    }

    private void OnValourMessage(PlanetMessage message) {
        try {
            if (message.authorUserId == _user.get("id").getAsLong()) {
                return;
            }

            var user = GetUserAsync(message.authorUserId).get();

            var broadcast = ChatColor.AQUA + "Valour " + ChatColor.WHITE + "<" + user.name + "> " + message.content;

//            LogToConsole("Valour: " + message.content);
            Bukkit.broadcastMessage(broadcast);
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
