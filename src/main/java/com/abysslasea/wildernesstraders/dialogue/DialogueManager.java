package com.abysslasea.wildernesstraders.dialogue;

import com.abysslasea.wildernesstraders.NetworkHandler;
import com.abysslasea.wildernesstraders.entity.TraderEntity;
import com.abysslasea.wildernesstraders.network.DialoguePacket;
import com.abysslasea.wildernesstraders.network.ShopPacket;
import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.PacketDistributor;

import java.util.*;

public class DialogueManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final DialogueManager INSTANCE = new DialogueManager();

    private final Map<String, Map<Integer, DialogueNode>> serverDialogues = new HashMap<>();
    private final Map<String, Map<Integer, DialogueNode>> clientDialogues = new HashMap<>();

    private DialogueManager() {
        super(GSON, "dialoguetree");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        if (isClient()) {
            loadNodesFromResources(resources, clientDialogues);
        } else {
            loadNodesFromResources(resources, serverDialogues);
        }
    }

    private void loadNodesFromResources(Map<ResourceLocation, JsonElement> resources, Map<String, Map<Integer, DialogueNode>> target) {
        target.clear();
        resources.forEach((id, json) -> {
            try {
                String traderId = id.getPath();
                Map<Integer, DialogueNode> nodes = new HashMap<>();

                JsonObject root = json.getAsJsonObject();
                JsonObject nodesObj = root.getAsJsonObject("nodes");
                for (Map.Entry<String, JsonElement> entry : nodesObj.entrySet()) {
                    int nodeId = Integer.parseInt(entry.getKey());
                    JsonObject nodeObj = entry.getValue().getAsJsonObject();

                    String textKey = nodeObj.get("textKey").getAsString();
                    Map<Integer, String> optionKeys = new HashMap<>();
                    Map<Integer, Integer> options = new HashMap<>();

                    if (nodeObj.has("options")) {
                        JsonObject opts = nodeObj.getAsJsonObject("options");
                        for (Map.Entry<String, JsonElement> optEntry : opts.entrySet()) {
                            int idx = Integer.parseInt(optEntry.getKey());
                            JsonObject optObj = optEntry.getValue().getAsJsonObject();
                            String optTextKey = optObj.get("textKey").getAsString();

                            JsonElement nextElement = optObj.get("next");
                            int next;
                            if (nextElement.isJsonPrimitive() && nextElement.getAsJsonPrimitive().isString()) {
                                String nextStr = nextElement.getAsString();
                                if ("shop".equals(nextStr) || "s".equals(nextStr)) {
                                    next = -999;
                                } else {
                                    next = -1;
                                }
                            } else {
                                next = nextElement.getAsInt();
                            }

                            optionKeys.put(idx, optTextKey);
                            options.put(idx, next);
                        }
                    }
                    nodes.put(nodeId, new DialogueNode(textKey, optionKeys, options));
                }
                target.put(traderId, nodes);
            } catch (Exception e) {
            }
        });
    }

    public void ensureServerDialogueLoaded(net.minecraft.server.MinecraftServer server, String traderId) {
        if (!serverDialogues.containsKey(traderId)) {
            try {
                ResourceLocation dialogueLocation = new ResourceLocation("wildernesstraders", "dialoguetree/" + traderId + ".json");
                var resource = server.getResourceManager().getResourceOrThrow(dialogueLocation);
                String jsonContent = new String(resource.open().readAllBytes());

                Map<Integer, DialogueNode> nodes = new HashMap<>();
                JsonObject root = JsonParser.parseString(jsonContent).getAsJsonObject();
                JsonObject nodesObj = root.getAsJsonObject("nodes");

                for (Map.Entry<String, JsonElement> entry : nodesObj.entrySet()) {
                    int nodeId = Integer.parseInt(entry.getKey());
                    JsonObject nodeObj = entry.getValue().getAsJsonObject();

                    String textKey = nodeObj.get("textKey").getAsString();
                    Map<Integer, String> optionKeys = new HashMap<>();
                    Map<Integer, Integer> options = new HashMap<>();

                    if (nodeObj.has("options")) {
                        JsonObject opts = nodeObj.getAsJsonObject("options");
                        for (Map.Entry<String, JsonElement> optEntry : opts.entrySet()) {
                            int idx = Integer.parseInt(optEntry.getKey());
                            JsonObject optObj = optEntry.getValue().getAsJsonObject();
                            String optTextKey = optObj.get("textKey").getAsString();

                            JsonElement nextElement = optObj.get("next");
                            int next;
                            if (nextElement.isJsonPrimitive() && nextElement.getAsJsonPrimitive().isString()) {
                                String nextStr = nextElement.getAsString();
                                if ("shop".equals(nextStr) || "s".equals(nextStr)) {
                                    next = -999;
                                } else {
                                    next = -1;
                                }
                            } else {
                                next = nextElement.getAsInt();
                            }

                            optionKeys.put(idx, optTextKey);
                            options.put(idx, next);
                        }
                    }
                    nodes.put(nodeId, new DialogueNode(textKey, optionKeys, options));
                }
                serverDialogues.put(traderId, nodes);
            } catch (Exception e) {
                if (!serverDialogues.containsKey("default")) {
                    loadDefaultDialogue();
                }
            }
        }
    }

    private void loadDefaultDialogue() {
        Map<Integer, DialogueNode> defaultNodes = new HashMap<>();
        Map<Integer, String> optionKeys = new HashMap<>();
        Map<Integer, Integer> options = new HashMap<>();

        optionKeys.put(0, "trader.default.option.trade");
        optionKeys.put(1, "trader.default.option.goodbye");
        options.put(0, -999);
        options.put(1, -1);

        defaultNodes.put(0, new DialogueNode("trader.default.greeting", optionKeys, options));
        serverDialogues.put("default", defaultNodes);
    }

    private String getCleanTraderName(Entity npcEntity) {
        if (npcEntity instanceof TraderEntity traderEntity) {
            return traderEntity.getCleanDisplayName();
        }
        return "Trader";
    }

    @OnlyIn(Dist.CLIENT)
    public void updateClientNodes(Map<Integer, DialogueNode> nodes, String traderId) {
        Map<Integer, DialogueNode> traderNodes = clientDialogues.computeIfAbsent(traderId, k -> new HashMap<>());
        traderNodes.clear();
        traderNodes.putAll(nodes);
        currentTraderId = traderId;
    }

    @OnlyIn(Dist.CLIENT)
    private int currentNodeId = 0;
    @OnlyIn(Dist.CLIENT)
    private int currentEntityId = -1;
    @OnlyIn(Dist.CLIENT)
    private boolean shouldCloseDialogue = false;
    @OnlyIn(Dist.CLIENT)
    private String cachedNpcName = "";
    @OnlyIn(Dist.CLIENT)
    private String cachedNpcId = "";
    @OnlyIn(Dist.CLIENT)
    private String currentTraderId = "";

    @OnlyIn(Dist.CLIENT)
    public void loadClientProgress(int entityId, int currentNode, String traderId) {
        currentEntityId = entityId;
        currentNodeId = 0;
        shouldCloseDialogue = false;
        currentTraderId = traderId;
    }

    @OnlyIn(Dist.CLIENT)
    public void setCurrentEntityId(int entityId) {
        currentEntityId = entityId;
    }

    @OnlyIn(Dist.CLIENT)
    public void setCachedNpcInfo(String npcName, String npcId) {
        cachedNpcName = npcName;
        cachedNpcId = npcId;
    }

    @OnlyIn(Dist.CLIENT)
    public DialogueNode getCurrentNode() {
        Map<Integer, DialogueNode> traderNodes = clientDialogues.get(currentTraderId);
        if (traderNodes == null) {
            return null;
        }

        DialogueNode node = traderNodes.get(currentNodeId);
        if (node == null && traderNodes.containsKey(0)) {
            currentNodeId = 0;
            node = traderNodes.get(0);
        }

        return node;
    }

    @OnlyIn(Dist.CLIENT)
    public void setNode(int id) {
        if (id == -1) {
            shouldCloseDialogue = true;
        } else if (id == -999) {
            shouldCloseDialogue = true;
            if (currentEntityId == -1) {
                return;
            }
            NetworkHandler.CHANNEL.sendToServer(ShopPacket.openShop(currentEntityId, cachedNpcName, cachedNpcId));
        } else {
            Map<Integer, DialogueNode> traderNodes = clientDialogues.get(currentTraderId);
            if (traderNodes != null && traderNodes.containsKey(id)) {
                currentNodeId = id;
                saveCurrentNodeProgress();
                shouldCloseDialogue = false;
            } else {
                shouldCloseDialogue = true;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void saveCurrentNodeProgress() {
        if (currentEntityId != -1) {
            NetworkHandler.CHANNEL.sendToServer(DialoguePacket.updateProgress(currentEntityId, currentNodeId));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isDialogueActive() {
        return !shouldCloseDialogue;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean shouldCloseDialogue() {
        return shouldCloseDialogue;
    }

    public DialogueNode getNode(int id, String traderId) {
        if (isClient()) {
            Map<Integer, DialogueNode> traderNodes = clientDialogues.get(traderId);
            return traderNodes != null ? traderNodes.get(id) : null;
        } else {
            Map<Integer, DialogueNode> traderNodes = serverDialogues.get(traderId);
            return traderNodes != null ? traderNodes.get(id) : null;
        }
    }

    public DialogueNode getNode(int id) {
        return getNode(id, currentTraderId);
    }

    public static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    public void syncToPlayer(ServerPlayer player, Entity npcEntity, String traderId) {
        if (player.level().isClientSide() || npcEntity == null) {
            return;
        }

        ensureServerDialogueLoaded(player.server, traderId);
        Map<Integer, DialogueNode> traderNodes = serverDialogues.get(traderId);

        if (traderNodes == null) {
            ensureServerDialogueLoaded(player.server, "default");
            traderNodes = serverDialogues.get("default");
        }

        if (traderNodes == null) {
            return;
        }

        String cleanName = getCleanTraderName(npcEntity);
        DialogueProgress progress = new DialogueProgress();
        progress.currentNodeId = 0;

        DialoguePacket packet = DialoguePacket.syncNodes(traderNodes, traderId, cleanName, npcEntity.getId(), progress.currentNodeId);
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public void updateEntityProgress(Player player, Entity npcEntity, int currentNode) {
        CompoundTag entityData = npcEntity.getPersistentData();
        String playerKey = "dialogue_" + player.getUUID().toString();

        Map<Integer, DialogueNode> traderNodes = serverDialogues.get(currentTraderId);
        if (traderNodes == null) traderNodes = serverDialogues.get("default");

        int nodeToSave = (currentNode >= 0 && traderNodes != null && traderNodes.containsKey(currentNode)) ? currentNode : 0;

        CompoundTag playerData = new CompoundTag();
        playerData.putInt("currentNode", nodeToSave);
        entityData.put(playerKey, playerData);
    }

    public static class DialogueProgress {
        public int currentNodeId = 0;
    }
}