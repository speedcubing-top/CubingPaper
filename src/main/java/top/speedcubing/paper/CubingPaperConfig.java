package top.speedcubing.paper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class CubingPaperConfig {
    //CubingPaper - Configurations
    public static String[] restartArgument;
    public static boolean fixGhostBlock = true;
    public static boolean cleanLogs = true;
    public static boolean commandOP = false;
    public static boolean disablePlayerDataSaving = false;
    public static boolean disableOpsJson = false;
    //FlamePaper
    public static int bookMaxPages = 5;
    public static boolean adaptativeChunkGC = true;
    public static boolean allowMapDecorations = false;
    public static JsonObject config;

    public static void init() {
        try {
            if (!new File("cubingpaper.json").exists()) {
                InputStream file = CubingPaperConfig.class.getClassLoader().getResourceAsStream("configurations/cubingpaper.json");
                Files.copy(file, Paths.get("cubingpaper.json"), StandardCopyOption.REPLACE_EXISTING);
            }
            config = new JsonParser().parse(new FileReader("cubingpaper.json")).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void update() {
        try {
            FileWriter fileWriter = new FileWriter("cubingpaper.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(config, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
