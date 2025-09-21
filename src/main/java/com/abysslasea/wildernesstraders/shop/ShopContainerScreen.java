package com.abysslasea.wildernesstraders.shop;

import com.abysslasea.wildernesstraders.NetworkHandler;
import com.abysslasea.wildernesstraders.network.ShopPacket;
import com.abysslasea.wildernesstraders.trader.TradeEntry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class ShopContainerScreen extends AbstractContainerScreen<ShopContainer> {

    private static final ResourceLocation INVENTORY_TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/inventory.png");
    private static final ResourceLocation SHOP_BACKGROUND_TEXTURE = new ResourceLocation("wildernesstraders", "textures/gui/shop_background.png");
    private static final int GUI_WIDTH = 220;
    private static final int GUI_HEIGHT = 270;

    private Button goodbyeButton;
    private Button tradeButton;

    public ShopContainerScreen(ShopContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, Component.translatable("screen.wildernesstraders.shop_title", container.getNpcName()));
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 6;
        this.inventoryLabelY = 178;

        int leftX = this.leftPos + 30;
        int bottomY = this.topPos + 165;
        this.goodbyeButton = Button.builder(
                Component.translatable("gui.wildernesstraders.shop.goodbye"),
                button -> this.onClose()
        ).bounds(leftX, bottomY, 60, 20).build();
        this.addRenderableWidget(goodbyeButton);

        int rightX = this.leftPos + this.imageWidth - 90;
        this.tradeButton = Button.builder(
                Component.translatable("gui.wildernesstraders.shop.trade"),
                button -> {
                    NetworkHandler.CHANNEL.sendToServer(ShopPacket.executeTrade());
                }
        ).bounds(rightX, bottomY, 60, 20).build();
        this.addRenderableWidget(tradeButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderShopTooltips(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        renderCustomShopBackground(guiGraphics, x, y);
        renderVanillaInventoryBackground(guiGraphics, x, y);
    }

    private void renderCustomShopBackground(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, SHOP_BACKGROUND_TEXTURE);

        guiGraphics.blit(SHOP_BACKGROUND_TEXTURE, x, y, 0, 0, this.imageWidth, 180, 256, 256);

        Component saleTitle = Component.translatable("gui.wildernesstraders.shop.for_sale");
        guiGraphics.drawString(this.font, saleTitle, x + 20, y + 20, 0xFFFFFFFF, false);

        Component buyTitle = Component.translatable("gui.wildernesstraders.shop.buying");
        guiGraphics.drawString(this.font, buyTitle, x + 120, y + 20, 0xFFFFFFFF, false);

        Component purchaseSelectionTitle = Component.translatable("gui.wildernesstraders.shop.select_to_buy");
        guiGraphics.drawString(this.font, purchaseSelectionTitle, x + 20, y + 125, 0xFFFFFFFF, false);

        Component sellSelectionTitle = Component.translatable("gui.wildernesstraders.shop.select_to_sell");
        guiGraphics.drawString(this.font, sellSelectionTitle, x + 120, y + 125, 0xFFFFFFFF, false);

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int slotX = x + 19 + col * 20;
                int slotY = y + 29 + row * 20;
                drawSlotBorder(guiGraphics, slotX, slotY);
            }
        }

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int slotX = x + 119 + col * 20;
                int slotY = y + 29 + row * 20;
                drawSlotBorder(guiGraphics, slotX, slotY);
            }
        }

        for (int col = 0; col < 4; col++) {
            int slotX = x + 19 + col * 20;
            int slotY = y + 139;
            drawSlotBorder(guiGraphics, slotX, slotY);
        }

        for (int col = 0; col < 4; col++) {
            int slotX = x + 119 + col * 20;
            int slotY = y + 139;
            drawSlotBorder(guiGraphics, slotX, slotY);
        }

        renderCurrencyInfo(guiGraphics, x, y);
    }

    private void renderVanillaInventoryBackground(GuiGraphics guiGraphics, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, INVENTORY_TEXTURE);

        int inventoryStartX = 29;
        int inventoryY = y + 188;

        guiGraphics.blit(INVENTORY_TEXTURE, x + inventoryStartX - 1, inventoryY, 7, 83, 162, 54);
        guiGraphics.blit(INVENTORY_TEXTURE, x + inventoryStartX - 1, inventoryY + 58, 7, 141, 162, 18);
    }

    private void drawSlotBorder(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 1, 0xFF8B8B8B);
        guiGraphics.fill(x, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + 1, y + 18, 0xFF8B8B8B);
        guiGraphics.fill(x + 17, y, x + 18, y + 18, 0xFFFFFFFF);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF373737);
    }

    private void renderCurrencyInfo(GuiGraphics guiGraphics, int x, int y) {
        Component playerCurrency = Component.translatable("gui.wildernesstraders.shop.player_currency",
                menu.getPlayerCurrency());
        guiGraphics.drawString(this.font, playerCurrency, x + 5, y + 110, 0x00AA00, false);

        Component npcMoney = Component.translatable("gui.wildernesstraders.shop.npc_money",
                menu.getNpcName(), menu.getNpcMoney());
        int npcMoneyWidth = this.font.width(npcMoney);
        guiGraphics.drawString(this.font, npcMoney, x + this.imageWidth - npcMoneyWidth - 5, y + 110, 0x00AA00, false);
    }

    private void renderShopTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        List<TradeEntry> saleEntries = menu.getSaleEntries();
        List<TradeEntry> buyEntries = menu.getBuyEntries();

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int slotIndex = row * 4 + col;
                int slotX = x + 19 + col * 20;
                int slotY = y + 29 + row * 20;

                if (mouseX >= slotX && mouseX < slotX + 18 &&
                        mouseY >= slotY && mouseY < slotY + 18) {

                    if (slotIndex < saleEntries.size()) {
                        TradeEntry entry = saleEntries.get(slotIndex);
                        renderTradeEntryTooltip(guiGraphics, mouseX, mouseY, entry, true);
                    }
                }
            }
        }

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int slotIndex = row * 4 + col;
                int slotX = x + 119 + col * 20;
                int slotY = y + 29 + row * 20;

                if (mouseX >= slotX && mouseX < slotX + 18 &&
                        mouseY >= slotY && mouseY < slotY + 18) {

                    if (slotIndex < buyEntries.size()) {
                        TradeEntry entry = buyEntries.get(slotIndex);
                        renderTradeEntryTooltip(guiGraphics, mouseX, mouseY, entry, false);
                    }
                }
            }
        }

        renderSelectionTooltips(guiGraphics, mouseX, mouseY, x, y);
    }

    private void renderSelectionTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        for (int col = 0; col < 4; col++) {
            int slotX = x + 19 + col * 20;
            int slotY = y + 139;

            if (mouseX >= slotX && mouseX < slotX + 18 &&
                    mouseY >= slotY && mouseY < slotY + 18) {

                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("gui.wildernesstraders.shop.click_to_remove_purchase"));
                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
        }

        for (int col = 0; col < 4; col++) {
            int slotX = x + 119 + col * 20;
            int slotY = y + 139;

            if (mouseX >= slotX && mouseX < slotX + 18 &&
                    mouseY >= slotY && mouseY < slotY + 18) {

                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("gui.wildernesstraders.shop.click_to_remove_sale"));
                guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
            }
        }
    }

    private void renderTradeEntryTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, TradeEntry entry, boolean isSale) {
        List<Component> tooltip = new ArrayList<>();

        tooltip.add(entry.getItem().getDisplayName());

        if (isSale) {
            tooltip.add(Component.translatable("gui.wildernesstraders.shop.price.buy", entry.getPrice()));
            if (entry.hasStock()) {
                tooltip.add(Component.translatable("gui.wildernesstraders.shop.stock", entry.getCurrentStock()));
            } else {
                tooltip.add(Component.translatable("gui.wildernesstraders.shop.out_of_stock"));
            }
            tooltip.add(Component.translatable("gui.wildernesstraders.shop.click_to_add_purchase"));
        } else {
            tooltip.add(Component.translatable("gui.wildernesstraders.shop.price.sell", entry.getPrice()));
            tooltip.add(Component.translatable("gui.wildernesstraders.shop.click_to_add_sale"));
        }

        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            int slotIndex = this.hoveredSlot.index;
            int totalShopSlots = ShopContainer.SHOP_SALE_SLOTS + ShopContainer.SHOP_BUY_SLOTS +
                    ShopContainer.PURCHASE_SELECTION_SLOTS + ShopContainer.SELL_SELECTION_SLOTS;

            if (slotIndex >= totalShopSlots) {
                super.renderTooltip(guiGraphics, x, y);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        super.onClose();
        this.minecraft.setScreen(null);
    }
}