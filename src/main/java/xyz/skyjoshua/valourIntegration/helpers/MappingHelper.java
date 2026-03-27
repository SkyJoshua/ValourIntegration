package xyz.skyjoshua.valourIntegration.helpers;

import com.google.gson.reflect.TypeToken;
import xyz.skyjoshua.valourIntegration.ValourIntegration;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MappingHelper {
    public static void SaveData(ValourIntegration plugin) {
        try (FileWriter writer = new FileWriter("linkData.json")) {
            writer.write(plugin.Gson.toJson(plugin.UUIDToValourMap));
            plugin.LogToConsole("Saved updated Valour Integration link data.");
        } catch (Exception ex) {
            plugin.LogToConsole("Critical error saving Valour Integration link data!");
            plugin.LogToConsole(ex.getMessage());
        }
    }

    public static void LoadData(ValourIntegration plugin) {
        if (!new File("linkData.json").exists()) {
            plugin.LogToConsole("No Valour Integration link data found. Skipping load...");
            return;
        }
        try (FileReader reader = new FileReader("linkData.json")) {
            Type type = new TypeToken<ConcurrentHashMap<UUID, Long>>(){}.getType();
            plugin.UUIDToValourMap = plugin.Gson.fromJson(reader, type);
            plugin.LogToConsole("Loaded Valour Integration link data.");
        } catch (Exception ex) {
            plugin.LogToConsole("Critical error loading Valour link data!");
            plugin.LogToConsole(ex.getMessage());
        }
    }
}
