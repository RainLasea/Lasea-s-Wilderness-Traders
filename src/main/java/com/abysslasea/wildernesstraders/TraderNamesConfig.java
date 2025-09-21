package com.abysslasea.wildernesstraders;

import com.google.gson.*;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TraderNamesConfig {
    private static final String CONFIG_FILENAME = "trader_names.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final TraderNamesConfig INSTANCE = new TraderNamesConfig();

    private final Path configPath;
    private List<String> traderNames = new ArrayList<>();

    private TraderNamesConfig() {
        this.configPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILENAME);
        loadOrCreateConfig();
    }

    private void loadOrCreateConfig() {
        try {
            if (!Files.exists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
            }

            if (!Files.exists(configPath)) {
                createDefaultConfig();
            } else {
                loadConfig();
            }
        } catch (Exception e) {
            initializeDefaultNames();
        }
    }

    private void createDefaultConfig() throws IOException {
        initializeDefaultNames();

        StringBuilder configContent = new StringBuilder();
        configContent.append("{\n");
        configContent.append("  \"_comment\": [\n");
        configContent.append("    \"This file configures the name pool for all trader entities.\",\n");
        configContent.append("    \"All traders will randomly select names from this list.\",\n");
        configContent.append("    \"Names are randomly assigned based on the trader's spawn position.\",\n");
        configContent.append("    \"Each trader will keep the same name once assigned.\",\n");
        configContent.append("    \"\",\n");
        configContent.append("    \"You can add, remove, or modify names as you like.\",\n");
        configContent.append("    \"This file will NOT be overwritten by mod updates.\",\n");
        configContent.append("    \"To reset to defaults, delete this file and restart the game.\",\n");
        configContent.append("    \"\",\n");
        configContent.append("    \"Tips:\",\n");
        configContent.append("    \"- Names should be simple and suitable for merchants\",\n");
        configContent.append("    \"- Avoid special characters that might cause display issues\",\n");
        configContent.append("    \"- The more names you add, the more variety you'll see in-game\"\n");
        configContent.append("  ],\n");
        configContent.append("  \"trader_names\": ");

        JsonArray nameArray = new JsonArray();
        for (String name : traderNames) {
            nameArray.add(name);
        }

        configContent.append(GSON.toJson(nameArray));
        configContent.append("\n}");

        Files.createDirectories(configPath.getParent());
        Files.write(configPath, configContent.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void loadConfig() throws IOException {
        String content = Files.readString(configPath, StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        traderNames.clear();

        if (root.has("trader_names")) {
            JsonArray nameArray = root.getAsJsonArray("trader_names");
            for (JsonElement nameElement : nameArray) {
                String name = nameElement.getAsString().trim();
                if (!name.isEmpty()) {
                    traderNames.add(name);
                }
            }
        }

        if (traderNames.isEmpty()) {
            initializeDefaultNames();
        }
    }

    private void initializeDefaultNames() {
        traderNames = new ArrayList<>(Arrays.asList(
                "Alex", "Sam", "Jordan", "Casey", "Taylor", "Morgan", "Avery", "Riley",
                "Blake", "Quinn", "Sage", "River", "Rowan", "Lane", "Reed", "Grey",
                "Marcus", "Elena", "Tobias", "Vera", "Finn", "Cora", "Owen", "Ruby",
                "Jasper", "Iris", "Silas", "Luna", "Atlas", "Nova", "Felix", "Zara",
                "Stone", "Clay", "Flint", "Sage", "Moss", "Pine", "Oak", "Ash",
                "Brook", "Vale", "Hill", "Dale", "Glen", "Moor", "Heath", "Birch",
                "Smith", "Mason", "Cooper", "Turner", "Miller", "Baker", "Weaver", "Dyer",
                "Tanner", "Potter", "Carver", "Wright", "Hunter", "Fisher", "Trader", "Merchant",
                "Gold", "Silver", "Copper", "Iron", "Steel", "Bronze", "Gem", "Pearl",
                "Amber", "Jade", "Opal", "Onyx", "Garnet", "Topaz", "Quartz", "Crystal",
                "Ace", "Bo", "Cal", "Drew", "Eli", "Fox", "Guy", "Hal",
                "Ian", "Jay", "Kit", "Lee", "Max", "Nix", "Oz", "Rex"
        ));
    }

    public String generateName(long seed) {
        if (traderNames.isEmpty()) {
            initializeDefaultNames();
        }

        Random random = new Random(seed);
        String selectedName = traderNames.get(random.nextInt(traderNames.size()));
        return selectedName;
    }

    public List<String> getAllNames() {
        return new ArrayList<>(traderNames);
    }

    public int getNameCount() {
        return traderNames.size();
    }

    public boolean isEmpty() {
        return traderNames.isEmpty();
    }

    public void reload() {
        try {
            loadConfig();
        } catch (Exception e) {
        }
    }
}