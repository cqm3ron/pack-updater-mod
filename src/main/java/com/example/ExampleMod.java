package com.example;

import net.fabricmc.api.ModInitializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExampleMod implements ModInitializer {

    private static final String DOWNLOAD_URL = "http://mcpack.cqmeron.uk/";
    private static final String PACK_NAME = "BeaconPack_Latest.zip";
    private static final String OPTIONS_FILE = "options.txt";

    @Override
    public void onInitialize() {
        System.out.println("Initializing Texture Pack Updater Mod...");
        Path resourcePacksDirPath = getResourcePacksDirectory();
        downloadLatestPack(resourcePacksDirPath);
        removeOldVersions(resourcePacksDirPath);
        applyTexturePack(resourcePacksDirPath.resolve(PACK_NAME));
    }

    private Path getResourcePacksDirectory() {
        Path modsFolder = Paths.get(System.getProperty("user.dir"));
        Path resourcePacksDir = modsFolder.resolve("resourcepacks");

        try {
            if (!Files.exists(resourcePacksDir)) {
                Files.createDirectories(resourcePacksDir);
            }
        } catch (IOException e) {
            System.err.println("Failed to create resourcepacks directory: " + e.getMessage());
            e.printStackTrace();
        }

        return resourcePacksDir;
    }

    private void downloadLatestPack(Path resourcePacksDir) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(DOWNLOAD_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);  // Set timeout for connection
            connection.setReadTimeout(5000);     // Set timeout for reading data

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Path packPath = resourcePacksDir.resolve(PACK_NAME);

                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(packPath.toFile())) {

                    byte[] buffer = new byte[4096];  // Use a larger buffer size for efficiency
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                System.out.println("Downloaded the latest texture pack successfully to " + packPath);
            } else {
                System.err.println("Failed to download the texture pack. HTTP response code: " + responseCode);
            }

        } catch (IOException e) {
            System.err.println("Failed to download the latest texture pack: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();  // Ensure the connection is closed properly
            }
        }
    }

    private void removeOldVersions(Path resourcePacksDir) {
        try {
            Files.list(resourcePacksDir)
                    .filter(path -> path.getFileName().toString().startsWith("BeaconPack") &&
                                    !path.getFileName().toString().equals(PACK_NAME))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            System.out.println("Removed old version: " + path.getFileName());
                        } catch (IOException e) {
                            System.err.println("Failed to remove old version: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            System.err.println("Error while cleaning up old texture packs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void applyTexturePack(Path packPath) {
    File optionsFile = new File(System.getProperty("user.dir"), OPTIONS_FILE);

    try {
        List<String> lines = Files.readAllLines(optionsFile.toPath());
        String resourcePacksLine = lines.stream()
                                        .filter(line -> line.startsWith("resourcePacks:"))
                                        .findFirst()
                                        .orElse("resourcePacks:[]");

        // Extract the current list of resource packs
        Pattern pattern = Pattern.compile("resourcePacks:\\[(.*?)]");
        Matcher matcher = pattern.matcher(resourcePacksLine);
        List<String> resourcePacksList = new ArrayList<>();
        
        if (matcher.find()) {
            String packs = matcher.group(1);
            if (!packs.isEmpty()) {
                for (String pack : packs.split(",")) {
                    resourcePacksList.add(pack.trim().replace("\"", ""));
                }
            }
        }
        
        // Add the new texture pack to the list
        String newPack = "file/" + packPath.getFileName().toString();
        if (!resourcePacksList.contains(newPack)) {
            resourcePacksList.add(newPack);
        }

        // Write the updated list back to options.txt
        String updatedResourcePacks = resourcePacksList.stream()
                                                       .map(pack -> "\"" + pack + "\"")
                                                       .reduce((first, second) -> first + "," + second)
                                                       .orElse("");
        String updatedLine = "resourcePacks:[" + updatedResourcePacks + "]";

        // Rewrite the entire file
        List<String> updatedLines = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("resourcePacks:")) {
                updatedLines.add(updatedLine);
            } else {
                updatedLines.add(line);
            }
        }
        Files.write(optionsFile.toPath(), updatedLines);

        System.out.println("Applied the new texture pack: " + packPath.getFileName());
    } catch (IOException e) {
        System.err.println("Failed to apply the texture pack: " + e.getMessage());
        e.printStackTrace();
    }
}
}
