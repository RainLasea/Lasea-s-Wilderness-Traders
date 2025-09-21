package com.abysslasea.wildernesstraders.shop;

import com.abysslasea.wildernesstraders.WildernessTraders;
import com.abysslasea.wildernesstraders.entity.TraderEntity;
import com.abysslasea.wildernesstraders.trader.TraderEntityData;
import com.abysslasea.wildernesstraders.trader.TradeEntry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

public class ShopContainer extends AbstractContainerMenu {

    public static final int SHOP_SALE_SLOTS = 16;
    public static final int SHOP_BUY_SLOTS = 16;
    public static final int PURCHASE_SELECTION_SLOTS = 4;
    public static final int SELL_SELECTION_SLOTS = 4;
    public static final int PLAYER_INVENTORY_SLOTS = 36;

    private final Player player;
    private final int entityId;
    private final String npcName;
    private final String npcId;

    private final ItemStackHandler shopSaleInventory;
    private final ItemStackHandler shopBuyInventory;
    private final ItemStackHandler purchaseSelectionInventory;
    private final ItemStackHandler sellSelectionInventory;

    private final TraderEntityData traderData;
    private final List<TradeEntry> saleEntries;
    private final List<TradeEntry> buyEntries;

    private int playerCurrency = 0;
    private int npcMoney = 64;

    public static ShopContainer create(int windowId, Inventory playerInventory, int entityId, String npcName, String npcId) {
        try {
            return new ShopContainer(WildernessTraders.SHOP_CONTAINER_TYPE.get(), windowId, playerInventory, entityId, npcName, npcId);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static ShopContainer createWithData(int windowId, Inventory playerInventory, int entityId, String npcName, String npcId,
                                               List<TradeEntry> saleEntries, List<TradeEntry> buyEntries, int npcMoney, int playerCurrency) {
        try {
            return new ShopContainer(WildernessTraders.SHOP_CONTAINER_TYPE.get(), windowId, playerInventory,
                    entityId, npcName, npcId, saleEntries, buyEntries, npcMoney, playerCurrency);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public ShopContainer(MenuType<?> menuType, int windowId, Inventory playerInventory, int entityId, String npcName, String npcId) {
        super(menuType, windowId);
        this.player = playerInventory.player;
        this.entityId = entityId;
        this.npcName = npcName;
        this.npcId = npcId;

        this.shopSaleInventory = new ItemStackHandler(SHOP_SALE_SLOTS);
        this.shopBuyInventory = new ItemStackHandler(SHOP_BUY_SLOTS);
        this.purchaseSelectionInventory = new ItemStackHandler(PURCHASE_SELECTION_SLOTS);
        this.sellSelectionInventory = new ItemStackHandler(SELL_SELECTION_SLOTS);

        if (!player.level().isClientSide()) {
            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof TraderEntity traderEntity) {
                this.traderData = new TraderEntityData(traderEntity);
                this.saleEntries = traderData.getSaleEntries();
                this.buyEntries = traderData.getBuyEntries();
                this.npcMoney = traderData.getCurrentMoney();
            } else {
                this.traderData = null;
                this.saleEntries = new ArrayList<>();
                this.buyEntries = new ArrayList<>();
            }
        } else {
            this.traderData = null;
            this.saleEntries = new ArrayList<>();
            this.buyEntries = new ArrayList<>();
        }

        calculatePlayerCurrency();
        initializeShopDisplay();
        addSlots(playerInventory);
    }

    public ShopContainer(MenuType<?> menuType, int windowId, Inventory playerInventory, int entityId, String npcName, String npcId,
                         List<TradeEntry> saleEntries, List<TradeEntry> buyEntries, int npcMoney, int playerCurrency) {
        super(menuType, windowId);
        this.player = playerInventory.player;
        this.entityId = entityId;
        this.npcName = npcName;
        this.npcId = npcId;
        this.npcMoney = npcMoney;
        this.playerCurrency = playerCurrency;

        this.shopSaleInventory = new ItemStackHandler(SHOP_SALE_SLOTS);
        this.shopBuyInventory = new ItemStackHandler(SHOP_BUY_SLOTS);
        this.purchaseSelectionInventory = new ItemStackHandler(PURCHASE_SELECTION_SLOTS);
        this.sellSelectionInventory = new ItemStackHandler(SELL_SELECTION_SLOTS);

        this.traderData = null;
        this.saleEntries = new ArrayList<>(saleEntries);
        this.buyEntries = new ArrayList<>(buyEntries);

        initializeShopDisplay();
        addSlots(playerInventory);
    }

    public void calculatePlayerCurrency() {
        if (player.level().isClientSide()) {
            return;
        }

        playerCurrency = 0;

        if (traderData == null) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.getItem() == Items.EMERALD) {
                    playerCurrency += stack.getCount();
                }
            }
            return;
        }

        ItemStack currencyItem = traderData.getCurrencyItem();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItem(stack, currencyItem)) {
                playerCurrency += stack.getCount();
            }
        }
    }

    public boolean isBuyableItem(ItemStack stack) {
        if (traderData != null && !player.level().isClientSide()) {
            return traderData.isBuyableItem(stack);
        }
        return buyEntries.stream()
                .anyMatch(entry -> ItemStack.isSameItemSameTags(entry.getItem(), stack));
    }

    private void initializeShopDisplay() {
        for (int i = 0; i < SHOP_SALE_SLOTS && i < saleEntries.size(); i++) {
            shopSaleInventory.setStackInSlot(i, saleEntries.get(i).getItem());
        }

        for (int i = 0; i < SHOP_BUY_SLOTS && i < buyEntries.size(); i++) {
            shopBuyInventory.setStackInSlot(i, buyEntries.get(i).getItem());
        }
    }

    public List<TradeEntry> getSaleEntries() {
        return new ArrayList<>(saleEntries);
    }

    public List<TradeEntry> getBuyEntries() {
        return new ArrayList<>(buyEntries);
    }

    public void updateClientData(List<TradeEntry> newSaleEntries, List<TradeEntry> newBuyEntries, int newNpcMoney, int newPlayerCurrency) {
        if (player.level().isClientSide()) {
            this.saleEntries.clear();
            this.saleEntries.addAll(newSaleEntries);
            this.buyEntries.clear();
            this.buyEntries.addAll(newBuyEntries);
            this.npcMoney = newNpcMoney;
            this.playerCurrency = newPlayerCurrency;

            initializeShopDisplay();
            this.broadcastChanges();
        }
    }

    private void addSlots(Inventory playerInventory) {
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int index = row * 4 + col;
                addSlot(new ShopDisplaySlot(shopSaleInventory, index, 20 + col * 20, 30 + row * 20, ShopDisplaySlot.SlotType.SALE));
            }
        }

        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int index = row * 4 + col;
                addSlot(new ShopDisplaySlot(shopBuyInventory, index, 120 + col * 20, 30 + row * 20, ShopDisplaySlot.SlotType.BUY));
            }
        }

        for (int col = 0; col < 4; col++) {
            addSlot(new ShopSelectionSlot(purchaseSelectionInventory, col, 20 + col * 20, 140, ShopSelectionSlot.SlotType.PURCHASE, this));
        }

        for (int col = 0; col < 4; col++) {
            addSlot(new ShopSelectionSlot(sellSelectionInventory, col, 120 + col * 20, 140, ShopSelectionSlot.SlotType.SELL, this));
        }

        int inventoryStartX = 29;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, inventoryStartX + col * 18, 190 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, inventoryStartX + col * 18, 248));
        }
    }

    @Override
    public void clicked(int slotIndex, int dragType, ClickType clickType, Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        int totalShopSlots = SHOP_SALE_SLOTS + SHOP_BUY_SLOTS;
        int totalSelectionSlots = PURCHASE_SELECTION_SLOTS + SELL_SELECTION_SLOTS;

        if (slotIndex >= 0 && slotIndex < totalShopSlots + totalSelectionSlots) {
            handleShopSlotClick(slotIndex, clickType, player);
            return;
        }

        super.clicked(slotIndex, dragType, clickType, player);
    }

    private void handleShopSlotClick(int slotIndex, ClickType clickType, Player player) {
        if (clickType != ClickType.PICKUP) return;

        if (slotIndex < SHOP_SALE_SLOTS) {
            if (slotIndex < saleEntries.size()) {
                TradeEntry entry = saleEntries.get(slotIndex);
                addToPurchaseSelection(entry);
            }
        } else if (slotIndex < SHOP_SALE_SLOTS + SHOP_BUY_SLOTS) {
            int buyIndex = slotIndex - SHOP_SALE_SLOTS;
            if (buyIndex < buyEntries.size()) {
                TradeEntry entry = buyEntries.get(buyIndex);
                addToSellSelectionFromInventory(entry);
            }
        } else if (slotIndex < SHOP_SALE_SLOTS + SHOP_BUY_SLOTS + PURCHASE_SELECTION_SLOTS) {
            int selectionIndex = slotIndex - SHOP_SALE_SLOTS - SHOP_BUY_SLOTS;
            ItemStack stack = purchaseSelectionInventory.getStackInSlot(selectionIndex);
            if (!stack.isEmpty()) {
                removeFromPurchaseSelection(selectionIndex);
            }
        } else {
            int selectionIndex = slotIndex - SHOP_SALE_SLOTS - SHOP_BUY_SLOTS - PURCHASE_SELECTION_SLOTS;
            ItemStack stack = sellSelectionInventory.getStackInSlot(selectionIndex);
            if (!stack.isEmpty()) {
                removeFromSellSelection(selectionIndex);
            }
        }
    }

    public void executeBatchTrade() {
        if (player.level().isClientSide()) {
            return;
        }

        try {
            boolean anyTrade = false;
            calculatePlayerCurrency();

            for (int i = 0; i < PURCHASE_SELECTION_SLOTS; i++) {
                ItemStack stack = purchaseSelectionInventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    TradeEntry tradeEntry = findTradeEntry(stack, true);
                    if (tradeEntry != null) {
                        int itemsPerTrade = tradeEntry.getItem().getCount();
                        int tradeUnits = stack.getCount() / itemsPerTrade;
                        int totalPrice = tradeEntry.getPrice() * tradeUnits;

                        boolean hasStock = tradeEntry.canSell(stack.getCount());
                        boolean canAfford = playerCurrency >= totalPrice;
                        boolean traderCanSell = (traderData == null || traderData.canExecuteTrade(tradeEntry, true));

                        if (hasStock && canAfford && traderCanSell) {
                            removeCurrencyFromPlayer(totalPrice);
                            if (traderData != null) {
                                traderData.addMoney(totalPrice);
                                npcMoney = traderData.getCurrentMoney();
                            }

                            if (player.getInventory().add(stack.copy())) {
                                tradeEntry.consumeStock(stack.getCount());
                                purchaseSelectionInventory.setStackInSlot(i, ItemStack.EMPTY);
                                anyTrade = true;
                            } else {
                                addCurrencyToPlayer(totalPrice);
                                if (traderData != null) {
                                    traderData.subtractMoney(totalPrice);
                                    npcMoney = traderData.getCurrentMoney();
                                }
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < SELL_SELECTION_SLOTS; i++) {
                ItemStack stack = sellSelectionInventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    TradeEntry tradeEntry = findTradeEntry(stack, false);
                    if (tradeEntry != null) {
                        int itemsPerTrade = tradeEntry.getItem().getCount();
                        int tradeUnits = stack.getCount() / itemsPerTrade;
                        int totalPrice = tradeEntry.getPrice() * tradeUnits;

                        boolean canAfford = (traderData == null) || traderData.canExecuteTrade(tradeEntry, false);

                        if (canAfford) {
                            sellSelectionInventory.setStackInSlot(i, ItemStack.EMPTY);
                            addCurrencyToPlayer(totalPrice);
                            if (traderData != null) {
                                traderData.subtractMoney(totalPrice);
                                npcMoney = traderData.getCurrentMoney();
                            }
                            anyTrade = true;
                        }
                    }
                }
            }

            if (anyTrade) {
                calculatePlayerCurrency();
                this.broadcastChanges();
                player.getInventory().setChanged();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TradeEntry findTradeEntry(ItemStack stack, boolean isSale) {
        if (traderData != null && !player.level().isClientSide()) {
            return traderData.findTradeEntry(stack, isSale);
        }

        List<TradeEntry> entries = isSale ? saleEntries : buyEntries;
        return entries.stream()
                .filter(entry -> ItemStack.isSameItemSameTags(entry.getItem(), stack))
                .findFirst()
                .orElse(null);
    }

    private void removeCurrencyFromPlayer(int amount) {
        if (traderData == null || player.level().isClientSide()) {
            removeEmeraldsFromPlayer(amount);
            return;
        }

        ItemStack currencyItem = traderData.getCurrencyItem();
        int remaining = amount;

        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItem(stack, currencyItem)) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
        calculatePlayerCurrency();
    }

    private void addCurrencyToPlayer(int amount) {
        if (traderData == null || player.level().isClientSide()) {
            addEmeraldsToPlayer(amount);
            return;
        }

        ItemStack currencyItem = traderData.getCurrencyItem();
        ItemStack toAdd = currencyItem.copy();
        toAdd.setCount(amount);
        player.getInventory().add(toAdd);
        calculatePlayerCurrency();
    }

    private void removeEmeraldsFromPlayer(int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.EMERALD) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
        calculatePlayerCurrency();
    }

    private void addEmeraldsToPlayer(int amount) {
        ItemStack emeralds = new ItemStack(Items.EMERALD, amount);
        player.getInventory().add(emeralds);
        calculatePlayerCurrency();
    }

    private void addToPurchaseSelection(TradeEntry entry) {
        ItemStack item = entry.getItem();

        if (!entry.hasStock()) {
            return;
        }

        int itemCountPerTrade = item.getCount();
        int currentStock = entry.getCurrentStock();

        int totalInSelection = getTotalItemCountInPurchaseSelection(item);
        int remainingStock = currentStock - totalInSelection;

        if (remainingStock < itemCountPerTrade) {
            return;
        }

        for (int i = 0; i < PURCHASE_SELECTION_SLOTS; i++) {
            ItemStack existingStack = purchaseSelectionInventory.getStackInSlot(i);
            if (ItemStack.isSameItemSameTags(existingStack, item)) {
                int currentCount = existingStack.getCount();
                int maxStackSize = existingStack.getMaxStackSize();

                if (currentCount + itemCountPerTrade <= maxStackSize) {
                    existingStack.setCount(currentCount + itemCountPerTrade);
                }
                return;
            }
        }

        for (int i = 0; i < PURCHASE_SELECTION_SLOTS; i++) {
            if (purchaseSelectionInventory.getStackInSlot(i).isEmpty()) {
                ItemStack stackToAdd = item.copy();
                stackToAdd.setCount(itemCountPerTrade);
                purchaseSelectionInventory.setStackInSlot(i, stackToAdd);
                return;
            }
        }
    }

    private int getTotalItemCountInPurchaseSelection(ItemStack targetItem) {
        int total = 0;
        for (int i = 0; i < PURCHASE_SELECTION_SLOTS; i++) {
            ItemStack stack = purchaseSelectionInventory.getStackInSlot(i);
            if (ItemStack.isSameItemSameTags(stack, targetItem)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private void addToSellSelectionFromInventory(TradeEntry entry) {
        ItemStack targetStack = entry.getItem();
        int itemCountPerTrade = targetStack.getCount();

        if (!hasEnoughItemsInInventory(targetStack, itemCountPerTrade)) {
            return;
        }

        for (int i = 0; i < SELL_SELECTION_SLOTS; i++) {
            ItemStack existingStack = sellSelectionInventory.getStackInSlot(i);

            if (!existingStack.isEmpty() && ItemStack.isSameItemSameTags(existingStack, targetStack)) {
                int currentCount = existingStack.getCount();
                int maxStackSize = existingStack.getMaxStackSize();

                if (currentCount + itemCountPerTrade <= maxStackSize) {
                    ItemStack itemsToRemove = targetStack.copy();
                    itemsToRemove.setCount(itemCountPerTrade);
                    removeItemFromPlayerInventory(itemsToRemove);
                    existingStack.setCount(currentCount + itemCountPerTrade);
                }
                return;
            }
        }

        for (int i = 0; i < SELL_SELECTION_SLOTS; i++) {
            if (sellSelectionInventory.getStackInSlot(i).isEmpty()) {
                ItemStack itemsToAdd = targetStack.copy();
                itemsToAdd.setCount(itemCountPerTrade);
                removeItemFromPlayerInventory(itemsToAdd);
                sellSelectionInventory.setStackInSlot(i, itemsToAdd);
                return;
            }
        }
        this.broadcastChanges();
    }

    private boolean hasEnoughItemsInInventory(ItemStack targetStack, int requiredCount) {
        int totalCount = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inventoryStack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(inventoryStack, targetStack)) {
                totalCount += inventoryStack.getCount();
                if (totalCount >= requiredCount) {
                    return true;
                }
            }
        }
        return false;
    }

    private void removeFromPurchaseSelection(int index) {
        purchaseSelectionInventory.setStackInSlot(index, ItemStack.EMPTY);
    }

    private void removeFromSellSelection(int index) {
        ItemStack stack = sellSelectionInventory.getStackInSlot(index);
        sellSelectionInventory.setStackInSlot(index, ItemStack.EMPTY);

        if (!player.getInventory().add(stack.copy())) {
            player.drop(stack.copy(), false);
        }
    }

    private boolean hasItemInInventory(ItemStack targetStack) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack inventoryStack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(inventoryStack, targetStack) &&
                    inventoryStack.getCount() >= 1) {
                return true;
            }
        }
        return false;
    }

    private void removeItemFromPlayerInventory(ItemStack targetStack) {
        int countToRemove = targetStack.getCount();

        for (int i = 0; i < player.getInventory().getContainerSize() && countToRemove > 0; i++) {
            ItemStack inventoryStack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(inventoryStack, targetStack)) {
                int availableCount = inventoryStack.getCount();
                int removeFromThisStack = Math.min(countToRemove, availableCount);

                inventoryStack.shrink(removeFromThisStack);
                countToRemove -= removeFromThisStack;
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().getEntity(entityId) != null) {
            return player.distanceToSqr(player.level().getEntity(entityId)) <= 64.0;
        }
        return false;
    }

    public String getNpcName() {
        return npcName;
    }

    public String getNpcId() {
        return npcId;
    }

    public int getEntityId() {
        return entityId;
    }

    public int getPlayerCurrency() {
        return playerCurrency;
    }

    public int getNpcMoney() {
        return npcMoney;
    }

    public TraderEntityData getTraderData() {
        return traderData;
    }

    public static class ShopDisplaySlot extends SlotItemHandler {
        public enum SlotType { SALE, BUY }
        private final SlotType slotType;

        public ShopDisplaySlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition, SlotType slotType) {
            super(itemHandler, index, xPosition, yPosition);
            this.slotType = slotType;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public ItemStack remove(int amount) {
            return ItemStack.EMPTY;
        }

        public SlotType getSlotType() {
            return slotType;
        }
    }

    public static class ShopSelectionSlot extends SlotItemHandler {
        public enum SlotType { PURCHASE, SELL }
        private final SlotType slotType;
        private final ShopContainer container;

        public ShopSelectionSlot(ItemStackHandler itemHandler, int index, int xPosition, int yPosition, SlotType slotType, ShopContainer container) {
            super(itemHandler, index, xPosition, yPosition);
            this.slotType = slotType;
            this.container = container;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (slotType == SlotType.SELL) {
                return container.isBuyableItem(stack);
            }
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return true;
        }

        @Override
        public ItemStack remove(int amount) {
            return super.remove(amount);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (container != null) {
                container.calculatePlayerCurrency();
            }
        }

        public SlotType getSlotType() {
            return slotType;
        }
    }
}