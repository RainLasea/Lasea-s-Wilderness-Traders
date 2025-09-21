package com.abysslasea.wildernesstraders.network;

import com.abysslasea.wildernesstraders.dialogue.DialogueManager;
import com.abysslasea.wildernesstraders.dialogue.DialogueNode;
import com.abysslasea.wildernesstraders.dialogue.DialogueScreen;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class DialoguePacket {

    public enum Type {
        OPEN_GUI,
        SYNC_NODES,
        UPDATE_PROGRESS
    }

    private final Type type;
    private final int entityId;
    private final String npcName;
    private final String npcId;
    private final Map<Integer, DialogueNode> nodes;
    private final int currentNode;

    public DialoguePacket(Type type, int entityId, String npcName, String npcId) {
        this.type = type;
        this.entityId = entityId;
        this.npcName = npcName;
        this.npcId = npcId;
        this.nodes = new HashMap<>();
        this.currentNode = 0;
    }

    public DialoguePacket(Type type, Map<Integer, DialogueNode> nodes, String traderId, String npcName, int entityId, int currentNode) {
        this.type = type;
        this.entityId = entityId;
        this.npcName = npcName;
        this.npcId = traderId;
        this.nodes = nodes;
        this.currentNode = currentNode;
    }

    public DialoguePacket(Type type, int entityId, int currentNode) {
        this.type = type;
        this.entityId = entityId;
        this.currentNode = currentNode;
        this.npcName = "";
        this.npcId = "";
        this.nodes = new HashMap<>();
    }

    public static void encode(DialoguePacket pkt, FriendlyByteBuf buf) {
        buf.writeEnum(pkt.type);
        buf.writeInt(pkt.entityId);
        buf.writeUtf(pkt.npcName);
        buf.writeUtf(pkt.npcId);
        buf.writeInt(pkt.currentNode);

        if (pkt.type == Type.SYNC_NODES) {
            JsonObject root = new JsonObject();
            pkt.nodes.forEach((id, node) -> {
                JsonObject nodeObj = new JsonObject();
                nodeObj.addProperty("textKey", node.getTextKey());

                JsonObject opts = new JsonObject();
                node.getOptionKeys().forEach((idx, textKey) -> {
                    JsonObject optObj = new JsonObject();
                    optObj.addProperty("textKey", textKey);
                    optObj.addProperty("next", node.getOptions().get(idx));
                    opts.add(String.valueOf(idx), optObj);
                });
                nodeObj.add("options", opts);
                root.add(String.valueOf(id), nodeObj);
            });
            String jsonString = root.toString();
            buf.writeUtf(jsonString);
        } else {
            buf.writeUtf("");
        }
    }

    public static DialoguePacket decode(FriendlyByteBuf buf) {
        Type type = buf.readEnum(Type.class);
        int entityId = buf.readInt();
        String npcName = buf.readUtf();
        String npcId = buf.readUtf();
        int currentNode = buf.readInt();
        String json = buf.readUtf();

        if (type == Type.SYNC_NODES && !json.isEmpty()) {
            Map<Integer, DialogueNode> nodes = new HashMap<>();
            try {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                    int id = Integer.parseInt(entry.getKey());
                    JsonObject nodeObj = entry.getValue().getAsJsonObject();
                    String textKey = nodeObj.get("textKey").getAsString();

                    Map<Integer, String> optionKeys = new HashMap<>();
                    Map<Integer, Integer> options = new HashMap<>();

                    if (nodeObj.has("options")) {
                        JsonObject opts = nodeObj.getAsJsonObject("options");
                        for (Map.Entry<String, JsonElement> optEntry : opts.entrySet()) {
                            int idx = Integer.parseInt(optEntry.getKey());
                            JsonObject optObj = optEntry.getValue().getAsJsonObject();
                            optionKeys.put(idx, optObj.get("textKey").getAsString());
                            int nextValue = optObj.get("next").getAsInt();
                            options.put(idx, nextValue);
                        }
                    }
                    nodes.put(id, new DialogueNode(textKey, optionKeys, options));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new DialoguePacket(type, nodes, npcId, npcName, entityId, currentNode);
        }

        return new DialoguePacket(type, entityId, npcName, npcId);
    }

    public static void handle(DialoguePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                switch (pkt.type) {
                    case OPEN_GUI:
                        Minecraft mc = Minecraft.getInstance();
                        mc.setScreen(new DialogueScreen(pkt.entityId, pkt.npcName, pkt.npcId));
                        break;

                    case SYNC_NODES:
                        if (DialogueManager.isClient()) {
                            DialogueManager.INSTANCE.updateClientNodes(pkt.nodes, pkt.npcId);
                            if (pkt.entityId != -1) {
                                DialogueManager.INSTANCE.loadClientProgress(pkt.entityId, pkt.currentNode, pkt.npcId);
                            }
                            DialogueManager.INSTANCE.setCachedNpcInfo(pkt.npcName, pkt.npcId);
                            DialogueManager.INSTANCE.setCurrentEntityId(pkt.entityId);
                        }
                        break;
                }
            });
        } else if (context.getDirection().getReceptionSide() == LogicalSide.SERVER) {
            context.enqueueWork(() -> {
                if (pkt.type == Type.UPDATE_PROGRESS) {
                    ServerPlayer player = context.getSender();
                    if (player != null) {
                        Entity npcEntity = player.level().getEntity(pkt.entityId);
                        if (npcEntity != null) {
                            DialogueManager.INSTANCE.updateEntityProgress(player, npcEntity, pkt.currentNode);
                        }
                    }
                }
            });
        }

        context.setPacketHandled(true);
    }

    public static DialoguePacket openGui(int entityId, String npcName, String npcId) {
        return new DialoguePacket(Type.OPEN_GUI, entityId, npcName, npcId);
    }

    public static DialoguePacket syncNodes(Map<Integer, DialogueNode> nodes, String traderId, String npcName, int entityId, int currentNode) {
        return new DialoguePacket(Type.SYNC_NODES, nodes, traderId, npcName, entityId, currentNode);
    }

    public static DialoguePacket updateProgress(int entityId, int currentNode) {
        return new DialoguePacket(Type.UPDATE_PROGRESS, entityId, currentNode);
    }
}