package com.abysslasea.wildernesstraders.entity;

import com.abysslasea.wildernesstraders.trader.TradeEntry;
import com.abysslasea.wildernesstraders.trader.TraderData;
import com.abysslasea.wildernesstraders.trader.TraderResourceManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class TraderWorldData extends SavedData {
    private static final String DATA_NAME = "wildernesstraders_data";

    private final Map<String, TraderRefreshData> traderRefreshData = new HashMap<>();
    private final Map<String, List<TradeEntry>> traderTrades = new HashMap<>();

    public TraderWorldData() {
        super();
    }

    public static TraderWorldData get(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getDataStorage().computeIfAbsent(
                    TraderWorldData::load,
                    TraderWorldData::new,
                    DATA_NAME
            );
        }
        throw new IllegalArgumentException("TraderWorldData can only be used on server side");
    }

    public static TraderWorldData load(CompoundTag nbt) {
        TraderWorldData data = new TraderWorldData();

        if (nbt.contains("RefreshData", Tag.TAG_COMPOUND)) {
            CompoundTag refreshTag = nbt.getCompound("RefreshData");
            for (String traderUUID : refreshTag.getAllKeys()) {
                CompoundTag traderTag = refreshTag.getCompound(traderUUID);
                TraderRefreshData refreshData = TraderRefreshData.fromNBT(traderTag);
                data.traderRefreshData.put(traderUUID, refreshData);
            }
        }

        if (nbt.contains("TradeData", Tag.TAG_COMPOUND)) {
            CompoundTag tradeTag = nbt.getCompound("TradeData");
            for (String traderUUID : tradeTag.getAllKeys()) {
                ListTag tradeList = tradeTag.getList(traderUUID, Tag.TAG_COMPOUND);
                List<TradeEntry> trades = new ArrayList<>();
                for (int i = 0; i < tradeList.size(); i++) {
                    CompoundTag entryTag = tradeList.getCompound(i);
                    TradeEntry entry = TradeEntry.fromNBT(entryTag);
                    if (entry != null) {
                        trades.add(entry);
                    }
                }
                data.traderTrades.put(traderUUID, trades);
            }
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        CompoundTag refreshTag = new CompoundTag();
        for (Map.Entry<String, TraderRefreshData> entry : traderRefreshData.entrySet()) {
            refreshTag.put(entry.getKey(), entry.getValue().toNBT());
        }
        compound.put("RefreshData", refreshTag);

        CompoundTag tradeTag = new CompoundTag();
        for (Map.Entry<String, List<TradeEntry>> entry : traderTrades.entrySet()) {
            ListTag tradeList = new ListTag();
            for (TradeEntry trade : entry.getValue()) {
                CompoundTag entryTag = trade.toNBT();
                if (entryTag != null) {
                    tradeList.add(entryTag);
                }
            }
            tradeTag.put(entry.getKey(), tradeList);
        }
        compound.put("TradeData", tradeTag);

        return compound;
    }

    public void registerTrader(TraderEntity trader) {
        String traderUUID = trader.getTraderUUID();
        String traderName = trader.getTraderName();

        if (!traderRefreshData.containsKey(traderUUID)) {
            TraderData traderData = TraderResourceManager.INSTANCE.getTrader(traderName);
            if (traderData == null) {
                traderData = TraderResourceManager.INSTANCE.getDefaultTrader();
            }

            long currentTime = trader.level().getGameTime();
            long nextRefreshTime = currentTime + traderData.getRestockTicks();
            TraderRefreshData refreshData = new TraderRefreshData(
                    traderName,
                    currentTime,
                    nextRefreshTime,
                    generateRandomSeed(trader)
            );

            traderRefreshData.put(traderUUID, refreshData);
            generateTradesForTrader(traderUUID, traderName, refreshData.randomSeed);

            this.setDirty();
        }
    }

    public void checkAndUpdateTrader(TraderEntity trader) {
        String traderUUID = trader.getTraderUUID();
        TraderRefreshData refreshData = traderRefreshData.get(traderUUID);

        if (refreshData == null) {
            registerTrader(trader);
            return;
        }

        long currentTime = trader.level().getGameTime();

        if (currentTime >= refreshData.nextRefreshTime) {
            refreshTrader(trader, refreshData);
        }
    }

    private void refreshTrader(TraderEntity trader, TraderRefreshData refreshData) {
        String traderUUID = trader.getTraderUUID();
        String traderName = trader.getTraderName();

        TraderData traderData = TraderResourceManager.INSTANCE.getTrader(traderName);
        if (traderData == null) {
            traderData = TraderResourceManager.INSTANCE.getDefaultTrader();
        }

        TraderResourceManager.TradePool pool = TraderResourceManager.INSTANCE.getTradePool(traderName);
        int resetMoney = (pool != null && pool.getInitialMoneyOverride() != null) ?
                pool.getInitialMoneyOverride() :
                traderData.getInitialMoney();

        trader.setCurrentMoney(resetMoney);

        List<TradeEntry> trades = traderTrades.get(traderUUID);
        if (trades != null) {
            for (TradeEntry trade : trades) {
                if (trade.isSell()) {
                    trade.resetStock();
                }
            }
        }

        long currentTime = trader.level().getGameTime();
        int restockDays = (pool != null && pool.getRestockDaysOverride() != null) ?
                pool.getRestockDaysOverride() :
                traderData.getRestockDays();

        refreshData.lastRefreshTime = currentTime;
        refreshData.nextRefreshTime = currentTime + (restockDays * 24000L);

        this.setDirty();
    }

    private void generateTradesForTrader(String traderUUID, String traderName, long seed) {
        Random random = new Random(seed);
        List<TradeEntry> trades = TraderResourceManager.INSTANCE.getRandomizedTrades(traderName, 16, random);
        traderTrades.put(traderUUID, trades);
    }

    private long generateRandomSeed(TraderEntity trader) {
        return trader.level().getGameTime() + trader.getId() + trader.getTraderName().hashCode();
    }

    public List<TradeEntry> getTraderTrades(String traderUUID) {
        return traderTrades.getOrDefault(traderUUID, new ArrayList<>());
    }

    public long getNextRefreshTime(String traderUUID) {
        TraderRefreshData data = traderRefreshData.get(traderUUID);
        return data != null ? data.nextRefreshTime : 0L;
    }

    public long getTimeUntilRefresh(String traderUUID, long currentTime) {
        TraderRefreshData data = traderRefreshData.get(traderUUID);
        if (data == null) return 0L;
        return Math.max(0L, data.nextRefreshTime - currentTime);
    }

    public int getDaysUntilRefresh(String traderUUID, long currentTime) {
        long ticks = getTimeUntilRefresh(traderUUID, currentTime);
        return (int) Math.ceil(ticks / 24000.0);
    }

    public void forceRefreshTrader(TraderEntity trader) {
        String traderUUID = trader.getTraderUUID();
        TraderRefreshData refreshData = traderRefreshData.get(traderUUID);
        if (refreshData != null) {
            refreshData.nextRefreshTime = trader.level().getGameTime();
            refreshTrader(trader, refreshData);
        }
    }

    public static class TraderRefreshData {
        public String traderName;
        public long lastRefreshTime;
        public long nextRefreshTime;
        public long randomSeed;

        public TraderRefreshData(String traderName, long lastRefreshTime, long nextRefreshTime, long randomSeed) {
            this.traderName = traderName;
            this.lastRefreshTime = lastRefreshTime;
            this.nextRefreshTime = nextRefreshTime;
            this.randomSeed = randomSeed;
        }

        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putString("TraderName", traderName);
            tag.putLong("LastRefreshTime", lastRefreshTime);
            tag.putLong("NextRefreshTime", nextRefreshTime);
            tag.putLong("RandomSeed", randomSeed);
            return tag;
        }

        public static TraderRefreshData fromNBT(CompoundTag tag) {
            return new TraderRefreshData(
                    tag.getString("TraderName"),
                    tag.getLong("LastRefreshTime"),
                    tag.getLong("NextRefreshTime"),
                    tag.getLong("RandomSeed")
            );
        }
    }
}