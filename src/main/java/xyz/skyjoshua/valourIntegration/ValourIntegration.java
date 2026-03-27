package xyz.skyjoshua.valourIntegration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.signalr.HubConnection;
import de.myzelyam.api.vanish.VanishAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import xyz.skyjoshua.valourIntegration.commands.minecraft.MinecraftLink;
import xyz.skyjoshua.valourIntegration.commands.valour.ConsoleCommand;
import xyz.skyjoshua.valourIntegration.commands.valour.PlayerList;
import xyz.skyjoshua.valourIntegration.commands.valour.ValourLink;
import xyz.skyjoshua.valourIntegration.helpers.MappingHelper;
import xyz.skyjoshua.valourIntegration.helpers.SignalRHelper;
import xyz.skyjoshua.valourIntegration.helpers.ValourMessage;
import xyz.skyjoshua.valourIntegration.listeners.*;
import xyz.skyjoshua.valourIntegration.models.*;
import static xyz.skyjoshua.valourIntegration.helpers.UserHelper.GetUserAsync;

import java.io.Console;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;



public final class ValourIntegration extends JavaPlugin {

    public HubConnection[] _signalR = new HubConnection[1];
    public boolean[] _hasConnected = new boolean[]{false};
    private FileConfiguration _config;
    public HttpClient http;
    public final Gson Gson = new GsonBuilder().create();
    public final String BaseUrl = "https://api.valour.gg/api/";
    public final String _hubUrl = "https://api.valour.gg/hubs/core";

    public String _primaryNode;
    public JsonObject _user;
    public JsonObject _member;

    public String[] BaseHeaders;

    public boolean hasVanish;

    public @Nullable String ValourAuth;
    public long ChannelId;
    public long PlanetId;

    public ConcurrentHashMap<UUID, Long> UUIDToValourMap = new ConcurrentHashMap<UUID, Long>();

    public ConcurrentHashMap<String, User> UserIdMap = new ConcurrentHashMap<String, User>();
    public ConcurrentHashMap<String, PlanetMember> MemberIdMap = new ConcurrentHashMap<String, PlanetMember>();

    public ConcurrentHashMap<String, UUID> _codeToUUID = new ConcurrentHashMap<String, UUID>();

    public void AddLinkCode(String code, UUID playerUUID) {
        _codeToUUID.put(code, playerUUID);
    }

    public void LogToConsole(String message) {
        getLogger().info(message);
    }

    @Override
    public void onLoad() {
        LogToConsole("ValourIntegration has been loaded");
        MappingHelper.LoadData(this);
    }

    @Override
    public void onEnable() {

        LogToConsole("Setting up config.");
        SetupConfig();
        LogToConsole("Attempting to login.");
        SetupHttp();

        LogToConsole("ValourIntegration has been enabled.");
        LogToConsole("Connecting to SignalR...");
        SignalRHelper.Setup(this, _signalR, _hasConnected);

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

        SetupCommands();;
    }

    @Override
    public void onDisable() {
        LogToConsole("ValourIntegration has been disabled.");
        ServerStopMessage();
    }

    private void SetupCommands() {
        this.getCommand("link").setExecutor(new MinecraftLink(this));
    }

    public void ServerStopMessage() {
        var message = getConfig().getString("serverStop");

        ValourMessage.SendAsync(this, message).thenAccept(result -> {
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

    public boolean isVanished(Player player) {
        if (hasVanish) {
            return VanishAPI.getInvisiblePlayers().contains(player.getUniqueId());
        }
        return false;
    }

    public void OnValourMessage(PlanetMessage message) {
        try {
            if (message.authorUserId == _user.get("id").getAsLong()) return;
            var user = GetUserAsync(this, message.authorUserId).get();
            if (user.bot) return;

            String prefix = _config.getString("commandPrefix");

            if (message.content.startsWith(prefix)) {
                String withoutPrefix = message.content.substring(prefix.length());
                String[] parts = withoutPrefix.split(" ");
                if (parts.length == 0) return;
                String command = parts[0].toLowerCase();

                switch (command) {
                    case "ip":
                        String ip = _config.getString("ipCommand")
                                .replace("{ping}", "«@m-" + message.authorMemberId + "»");
                        ValourMessage.ReplyAsync(this, ip, message.id);
                        break;

                    case "source":
                    case "src":
                        String src = _config.getString("sourceCommand")
                                .replace("{ping}", "«@m-" + message.authorMemberId + "»");
                        ValourMessage.ReplyAsync(this, src, message.id);
                        break;

                    case "list":
                    case "plist":
                    case "players":
                        PlayerList.Execute(this, message);
                        break;

                    case "cc":
                        ConsoleCommand.Execute(this, message);
                        break;

                    case "link":
                        ValourLink.Execute(this, message);
                        break;

                    default:
                        String defmsg = _config.getString("valourInvalidCommand")
                                .replace("{cmd}", command);
                        ValourMessage.ReplyAsync(this, defmsg, message.id);
                        break;
                }
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
}
