package com.abysslasea.wildernesstraders.trader;

import com.abysslasea.wildernesstraders.entity.TraderEntity;
import com.abysslasea.wildernesstraders.entity.TraderWorldData;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TraderEntityData {
    private final TraderEntity entity;
    private final TraderData traderData;
    private final TraderWorldData worldData;

    public TraderEntityData(TraderEntity entity) {
        this.entity = entity;
        this.worldData = TraderWorldData.get(entity.level());

        this.worldData.registerTrader(entity);
        this.worldData.checkAndUpdateTrader(entity);

        String traderName = entity.getTraderName();
        this.traderData = TraderResourceManager.INSTANCE.getTrader(traderName);
        if (this.traderData == null) {
            throw new IllegalStateException("No trader data found for: " + traderName);
        }
    }

    public int getCurrentMoney() {
        return entity.getCurrentMoney();
    }

    public void setCurrentMoney(int amount) {
        entity.setCurrentMoney(amount);
    }

    public void addMoney(int amount) {
        entity.addMoney(amount);
    }

    public void subtractMoney(int amount) {
        entity.subtractMoney(amount);
    }

    public ItemStack getCurrencyItem() {
        TraderResourceManager.TradePool pool = TraderResourceManager.INSTANCE.getTradePool(entity.getTraderName());
        if (pool != null && pool.getCurrencyOverride() != null) {
            return pool.getCurrencyOverride();
        }

        return traderData.getCurrencyItem();
    }

    public List<TradeEntry> getCurrentTrades() {
        String traderUUID = entity.getTraderUUID();
        return worldData.getTraderTrades(traderUUID);
    }

    public List<TradeEntry> getSaleEntries() {
        List<TradeEntry> saleEntries = new ArrayList<>();
        for (TradeEntry trade : getCurrentTrades()) {
            if (trade.isSell()) {
                saleEntries.add(trade);
            }
        }
        return saleEntries;
    }

    public List<TradeEntry> getBuyEntries() {
        List<TradeEntry> buyEntries = new ArrayList<>();
        for (TradeEntry trade : getCurrentTrades()) {
            if (trade.isBuy()) {
                buyEntries.add(trade);
            }
        }
        return buyEntries;
    }

    public long getTimeUntilRestock() {
        String traderUUID = entity.getTraderUUID();
        long currentTime = entity.level().getGameTime();
        return worldData.getTimeUntilRefresh(traderUUID, currentTime);
    }

    public int getDaysUntilRestock() {
        String traderUUID = entity.getTraderUUID();
        long currentTime = entity.level().getGameTime();
        return worldData.getDaysUntilRefresh(traderUUID, currentTime);
    }

    public void forceRestock() {
        worldData.forceRefreshTrader(entity);
    }

    public boolean canExecuteTrade(TradeEntry trade, boolean isPlayerBuying) {
        if (isPlayerBuying) {
            return true;
        } else {
            return getCurrentMoney() >= trade.getPrice();
        }
    }

    public TradeEntry findTradeEntry(ItemStack stack, boolean isSale) {
        List<TradeEntry> entries = isSale ? getSaleEntries() : getBuyEntries();
        return entries.stream()
                .filter(entry -> ItemStack.isSameItemSameTags(entry.getItem(), stack))
                .findFirst()
                .orElse(null);
    }

    public boolean isBuyableItem(ItemStack stack) {
        return getBuyEntries().stream()
                .anyMatch(entry -> ItemStack.isSameItemSameTags(entry.getItem(), stack));
    }

    public TraderData getTraderData() {
        return traderData;
    }

    public String getTraderId() {
        return entity.getTraderName();
    }

    public TraderEntity getEntity() {
        return entity;
    }

    public TraderWorldData getWorldData() {
        return worldData;
    }
}