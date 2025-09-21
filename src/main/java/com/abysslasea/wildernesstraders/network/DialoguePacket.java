package com.abysslasea.wildernesstraders.network;

import com.abysslasea.wildernesstraders.dialogue.DialogueManager;
import com.abysslasea.wildernesstraders.dialogue.DialogueNode;
import com.abysslasea.wildernesstraders.dialogue.DialogueScreen;
import com.abysslasea.wildernesstraders.entity.TraderEntity;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class DialoguePacket {

    public enum Type {
        OPEN_GUI,
        SYNC_NODES,
        UPDATE_PROGRESS,
        TRIGGER_ANIMATION
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
        this.npcName = npcName != null ? npcName : "";
        this.npcId = npcId != null ? npcId : "";
        this.nodes = new HashMap<>();
        this.currentNode = 0;
    }

    public DialoguePacket(Type type, Map<Integer, DialogueNode> nodes, String traderId, String npcName, int entityId, int currentNode) {
        this.type = type;
        this.entityId = entityId;
        this.npcName = npcName != null ? npcName : "";
        this.npcId = traderId != null ? traderId : "";
        this.nodes = nodes != null ? nodes : new HashMap<>();
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

    public DialoguePacket(Type type, int entityId) {
        this.type = type;
        this.entityId = entityId;
        this.npcName = "";
        this.npcId = "";
        this.nodes = new HashMap<>();
        this.currentNode = 0;
    }

    public static void encode(DialoguePacket pkt, FriendlyByteBuf buf) {
        try {
            buf.writeEnum(pkt.type);
            buf.writeInt(pkt.entityId);
            buf.writeUtf(pkt.npcName);
            buf.writeUtf(pkt.npcId);
            buf.writeInt(pkt.currentNode);

            if (pkt.type == Type.SYNC_NODES && pkt.nodes != null && !pkt.nodes.isEmpty()) {
                try {
                    JsonObject root = new JsonObject();
                    pkt.nodes.forEach((id, node) -> {
                        try {
                            JsonObject nodeObj = new JsonObject();
                            nodeObj.addProperty("textKey", node.getTextKey());

                            JsonObject opts = new JsonObject();
                            if (node.getOptionKeys() != null) {
                                node.getOptionKeys().forEach((idx, textKey) -> {
                                    try {
                                        JsonObject optObj = new JsonObject();
                                        optObj.addProperty("textKey", textKey);
                                        Integer nextValue = node.getOptions() != null ? node.getOptions().get(idx) : -1;
                                        optObj.addProperty("next", nextValue != null ? nextValue : -1);
                                        opts.add(String.valueOf(idx), optObj);
                                    } catch (Exception e) {
                                    }
                                });
                            }
                            nodeObj.add("options", opts);
                            root.add(String.valueOf(id), nodeObj);
                        } catch (Exception e) {
                        }
                    });
                    String jsonString = root.toString();
                    buf.writeUtf(jsonString);
                } catch (Exception e) {
                    buf.writeUtf("");
                }
            } else {
                buf.writeUtf("");
            }
        } catch (Exception e) {
        }
    }

    public static DialoguePacket decode(FriendlyByteBuf buf) {
        try {
            Type type = buf.readEnum(Type.class);
            int entityId = buf.readInt();
            String npcName = buf.readUtf();
            String npcId = buf.readUtf();
            int currentNode = buf.readInt();
            String json = buf.readUtf();

            if (type == Type.SYNC_NODES && json != null && !json.isEmpty()) {
                Map<Integer, DialogueNode> nodes = new HashMap<>();
                try {
                    JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                        try {
                            int id = Integer.parseInt(entry.getKey());
                            JsonObject nodeObj = entry.getValue().getAsJsonObject();

                            if (!nodeObj.has("textKey")) {
                                continue;
                            }

                            String textKey = nodeObj.get("textKey").getAsString();

                            Map<Integer, String> optionKeys = new HashMap<>();
                            Map<Integer, Integer> options = new HashMap<>();

                            if (nodeObj.has("options")) {
                                JsonObject opts = nodeObj.getAsJsonObject("options");
                                for (Map.Entry<String, JsonElement> optEntry : opts.entrySet()) {
                                    try {
                                        int idx = Integer.parseInt(optEntry.getKey());
                                        JsonObject optObj = optEntry.getValue().getAsJsonObject();

                                        if (!optObj.has("textKey") || !optObj.has("next")) {
                                            continue;
                                        }

                                        optionKeys.put(idx, optObj.get("textKey").getAsString());
                                        int nextValue = optObj.get("next").getAsInt();
                                        options.put(idx, nextValue);
                                    } catch (Exception e) {
                                    }
                                }
                            }
                            nodes.put(id, new DialogueNode(textKey, optionKeys, options));
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                }
                return new DialoguePacket(type, nodes, npcId, npcName, entityId, currentNode);
            }

            if (type == Type.TRIGGER_ANIMATION) {
                return new DialoguePacket(type, entityId);
            }

            return new DialoguePacket(type, entityId, npcName, npcId);
        } catch (Exception e) {
            return new DialoguePacket(Type.OPEN_GUI, -1, "", "");
        }
    }

    public static void handle(DialoguePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        try {
            if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                context.enqueueWork(() -> {
                    try {
                        switch (pkt.type) {
                            case OPEN_GUI:
                                try {
                                    Minecraft mc = Minecraft.getInstance();
                                    mc.setScreen(new DialogueScreen(pkt.entityId, pkt.npcName, pkt.npcId));
                                } catch (Exception e) {
                                }
                                break;

                            case SYNC_NODES:
                                try {
                                    if (DialogueManager.isClient()) {
                                        DialogueManager.INSTANCE.updateClientNodes(pkt.nodes, pkt.npcId);
                                        if (pkt.entityId != -1) {
                                            DialogueManager.INSTANCE.loadClientProgress(pkt.entityId, pkt.currentNode, pkt.npcId);
                                        }
                                        DialogueManager.INSTANCE.setCachedNpcInfo(pkt.npcName, pkt.npcId);
                                        DialogueManager.INSTANCE.setCurrentEntityId(pkt.entityId);
                                    }
                                } catch (Exception e) {
                                }
                                break;

                            case TRIGGER_ANIMATION:
                                try {
                                    if (pkt.entityId != -1) {
                                        Entity entity = Minecraft.getInstance().level.getEntity(pkt.entityId);
                                        if (entity instanceof TraderEntity trader) {
                                            trader.triggerTalkAnimation();
                                        }
                                    }
                                } catch (Exception e) {
                                }
                                break;
                        }
                    } catch (Exception e) {
                    }
                });
            } else if (context.getDirection().getReceptionSide() == LogicalSide.SERVER) {
                context.enqueueWork(() -> {
                    try {
                        if (pkt.type == Type.UPDATE_PROGRESS) {
                            ServerPlayer player = context.getSender();
                            if (player != null) {
                                Entity npcEntity = player.level().getEntity(pkt.entityId);
                                if (npcEntity != null) {
                                    DialogueManager.INSTANCE.updateEntityProgress(player, npcEntity, pkt.currentNode);
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                });
            }
        } catch (Exception e) {
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

    public static DialoguePacket triggerAnimation(int entityId) {
        return new DialoguePacket(Type.TRIGGER_ANIMATION, entityId);
    }
}