package com.abysslasea.wildernesstraders.dialogue;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.Map;

public class DialogueScreen extends Screen {
    private final int entityId;
    private final String npcName;
    private final String npcId;
    private Entity cachedEntity;

    public DialogueScreen(int entityId, String npcName, String npcId) {
        super(Component.translatable("screen.wildernesstraders.npc_dialogue"));
        this.entityId = entityId;
        this.npcName = npcName;
        this.npcId = npcId;
        DialogueManager.INSTANCE.setCurrentEntityId(entityId);
        DialogueManager.INSTANCE.setCachedNpcInfo(npcName, npcId);
    }

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null && this.minecraft.level != null) {
            this.cachedEntity = this.minecraft.level.getEntity(this.entityId);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        if (DialogueManager.INSTANCE.shouldCloseDialogue()) {
            this.onClose();
            return;
        }

        this.renderBackground(gui);

        int panelW = this.width / 2;
        int panelH = this.height - 40;
        int panelX = 20;
        int panelY = 20;

        gui.fillGradient(panelX, panelY, panelX + panelW - 40, panelY + panelH, 0xFF1F1F1F, 0xFF1F1F1F);
        gui.fillGradient(panelX + 6, panelY + 6, panelX + panelW - 46, panelY + 40, 0xFF2F2F2F, 0xFF2F2F2F);

        Font font = this.font;
        gui.drawString(font, Component.translatable("screen.wildernesstraders.npc_title", npcName),
                panelX + 10, panelY + 10, 0xFFFFFF, false);

        String playerName = this.minecraft != null && this.minecraft.player != null
                ? this.minecraft.player.getName().getString() : "";

        DialogueNode node = DialogueManager.INSTANCE.getCurrentNode();
        if (node != null) {
            String body = Component.translatable(node.getTextKey()).getString()
                    .replace("%player", playerName)
                    .replace("%p", playerName);
            drawWordWrappedString(gui, font, body, panelX + 10, panelY + 50, panelW - 60, 12);

            int optStartY = panelY + 120;
            for (Map.Entry<Integer, String> entry : node.getOptionKeys().entrySet()) {
                int idx = entry.getKey();
                String label = Component.translatable(entry.getValue()).getString();
                gui.drawString(font, Component.literal(idx + ". " + label), panelX + 30, optStartY, 0xFFDCDCDC, false);
                optStartY += 16;
            }
        }

        renderEntityDisplay(gui, font, mouseX, mouseY, partialTicks);
        super.render(gui, mouseX, mouseY, partialTicks);
    }

    private void renderEntityDisplay(GuiGraphics gui, Font font, int mouseX, int mouseY, float partialTicks) {
        int rightX = this.width - (this.width / 4);
        int rightY = this.height / 2 + 20;
        int scale = Math.max(30, Math.min(this.width, this.height) / 6);

        if (this.cachedEntity instanceof LivingEntity living) {
            try {
                renderEntityInGui(living, rightX, rightY, scale, partialTicks);
            } catch (Exception e) {
                gui.drawString(font, Component.translatable("screen.wildernesstraders.render_error"),
                        rightX - 80, rightY, 0xFFFFFFFF, false);
            }
        } else {
            gui.drawString(font, Component.translatable("screen.wildernesstraders.entity_missing"),
                    rightX - 50, rightY, 0xFFFFFFFF, false);
        }
    }

    private void drawWordWrappedString(GuiGraphics gui, Font font, String text, int x, int y, int maxWidth, int lineHeight) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int curY = y;

        for (String word : words) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (font.width(test) > maxWidth && !line.isEmpty()) {
                gui.drawString(font, Component.literal(line.toString()), x, curY, 0xFFFFFFFF, false);
                line = new StringBuilder(word);
                curY += lineHeight;
            } else {
                if (!line.isEmpty()) line.append(" ");
                line.append(word);
            }
        }
        if (!line.isEmpty()) {
            gui.drawString(font, Component.literal(line.toString()), x, curY, 0xFFFFFFFF, false);
        }
    }

    private void renderEntityInGui(LivingEntity entity, int x, int y, int scale, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        PoseStack pose = new PoseStack();
        pose.pushPose();
        pose.translate(x, y, 1050.0D);
        pose.scale(scale, scale, scale);
        pose.mulPose(new Quaternionf().rotateXYZ((float) Math.PI, 0.0F, 0.0F));

        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        dispatcher.setRenderShadow(false);
        dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, pose, buffer, 15728880);
        buffer.endBatch();
        dispatcher.setRenderShadow(true);
        pose.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        DialogueNode node = DialogueManager.INSTANCE.getCurrentNode();
        if (node != null) {
            int optStartY = 140;
            int i = 0;
            for (Map.Entry<Integer, String> entry : node.getOptionKeys().entrySet()) {
                int y = optStartY + i * 16;
                if (mouseY >= y && mouseY <= y + 12) {
                    Integer next = node.getOptions().get(entry.getKey());
                    if (next != null) {
                        DialogueManager.INSTANCE.setNode(next);
                    }
                    return true;
                }
                i++;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        super.onClose();
        this.minecraft.setScreen(null);
    }
}