package top.speedcubing.paper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.spigotmc.RestartCommand;

public class PluginUpdater {

    public static String getFileNameFromUrl(String url, HttpURLConnection connection) {
        try {
            String disp = connection.getHeaderField("Content-Disposition");

            if (disp != null && disp.contains("filename=")) {
                return disp.substring(disp.indexOf("filename=") + 9).replaceAll("\"", "");
            }

            return url.substring(url.lastIndexOf('/') + 1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void init() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<Void>> futures = new CopyOnWriteArrayList<>();

        System.out.println("[PluginUpdater] initializing...");
        try {
            JsonObject cfg = CubingPaperConfig.config.getAsJsonObject("plugins");
            AtomicBoolean restart = new AtomicBoolean(false);
            for (Map.Entry<String, JsonElement> e : cfg.entrySet()) {
                String url = e.getKey();
                JsonObject obj = e.getValue().getAsJsonObject();
                Future<Void> f = executorService.submit(() -> {
                    try {
                        Logger.getLogger("[PluginUpdater] verifying \"" + url + "\"");
                        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                        connection.addRequestProperty("User-Agent", "Mozilla/4.0");

                        String newName = getFileNameFromUrl(url, connection);
                        String newHash = connection.getHeaderField("Content-SHA256");

                        String oldHash = "null";
                        if (obj.has("hash")) {
                            oldHash = obj.get("hash").getAsString();
                        }

                        String oldName = "null";
                        if (obj.has("file")) {
                            oldName = obj.get("file").getAsString();
                        }

                        File oldFile = new File("plugins/" + oldName);
                        File newfile = new File("plugins/" + newName);

                        boolean update = false;
                        if (oldHash.equals(newHash)) {
                            if (!oldName.equals(newName)) {
                                if (oldFile.exists()) {
                                    oldFile.renameTo(newfile);
                                    System.out.println("[PluginUpdater] \"" + oldName + "\" rename to " + newfile);
                                } else {
                                    System.out.println("[PluginUpdater] \"" + oldName + "\" not found.");
                                    update = true;
                                }
                            } else {
                                if (oldFile.exists()) {
                                    System.out.println("[PluginUpdater] \"" + newName + "\" Identical.");
                                } else {
                                    System.out.println("[PluginUpdater] \"" + oldName + "\" not found.");
                                    update = true;
                                }
                            }
                        } else {
                            System.out.println("[PluginUpdater] \"" + newName + "\" diff " + oldHash + " <-> " + newHash);
                            if (!oldName.equals(newName))
                                oldFile.delete();
                            update = true;
                        }

                        if (update) {
                            oldFile.delete();
                            System.out.println("[PluginUpdater] installing \"" + url + "\"");
                            Files.copy(connection.getInputStream(), newfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            System.out.println("[PluginUpdater] installation of \"" + url + "\" is done");
                            connection.getInputStream().close();
                        }
                        connection.disconnect();

                        obj.addProperty("file", newName);
                        obj.addProperty("hash", newHash);

                        restart.set(restart.get() || update);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return null;
                });
                futures.add(f);
            }

            for (Future<Void> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            executorService.shutdown();
            CubingPaperConfig.update();

            if (restart.get())
                RestartCommand.restart();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
