package com.abysslasea.wildernesstraders;

import com.google.gson.*;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TraderConfig {
    private static final String CONFIG_FILENAME = "wilderness_traders.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final TraderConfig INSTANCE = new TraderConfig();

    private final Path configPath;
    private List<String> traderNames = new ArrayList<>();
    private boolean disableBuiltinResources = false;

    private TraderConfig() {
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
            initializeDefaults();
        }
    }

    private void createDefaultConfig() throws IOException {
        initializeDefaults();

        JsonObject root = new JsonObject();

        root.addProperty("disable_builtin_resources", disableBuiltinResources);

        JsonArray nameArray = new JsonArray();
        for (String name : traderNames) {
            nameArray.add(name);
        }
        root.add("trader_names", nameArray);

        Files.createDirectories(configPath.getParent());
        Files.write(configPath, GSON.toJson(root).getBytes(StandardCharsets.UTF_8));
    }

    private void loadConfig() throws IOException {
        String content = Files.readString(configPath, StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        disableBuiltinResources = root.has("disable_builtin_resources")
                ? root.get("disable_builtin_resources").getAsBoolean()
                : false;

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

    private void initializeDefaults() {
        initializeDefaultNames();
        disableBuiltinResources = false;
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

    public boolean isBuiltinResourcesDisabled() {
        return disableBuiltinResources;
    }

    public void setBuiltinResourcesDisabled(boolean disabled) {
        this.disableBuiltinResources = disabled;
    }

    public void saveConfig() {
        try {
            createDefaultConfig();
        } catch (IOException e) {
        }
    }

    public void reload() {
        try {
            loadConfig();
        } catch (Exception e) {
        }
    }
}