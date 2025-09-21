package com.abysslasea.wildernesstraders.trader;

import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.*;

public class TraderResourceManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final TraderResourceManager INSTANCE = new TraderResourceManager();

    private final Map<String, TraderData> traders = new HashMap<>();
    private final Map<String, TradePool> tradePools = new HashMap<>();

    private TraderResourceManager() {
        super(GSON, "traders");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        traders.clear();
        tradePools.clear();

        loadTraders(resources);
        loadTradePools(manager);
    }

    private void loadTraders(Map<ResourceLocation, JsonElement> resources) {
        resources.forEach((location, json) -> {
            try {
                String traderId = location.getPath();
                TraderData traderData = parseTraderData(traderId, json.getAsJsonObject());
                if (traderData != null) {
                    traders.put(traderId, traderData);
                }
            } catch (Exception e) {
            }
        });
    }

    private void loadTradePools(ResourceManager resourceManager) {
        try {
            var tradePoolResources = resourceManager.listResources("traderpool",
                    path -> path.getPath().endsWith(".json"));

            for (var entry : tradePoolResources.entrySet()) {
                try {
                    String content = new String(entry.getValue().open().readAllBytes());
                    JsonElement json = JsonParser.parseString(content);
                    String traderId = entry.getKey().getPath().replace("traderpool/", "").replace(".json", "");

                    TradePool tradePool = parseTradePool(traderId, json.getAsJsonObject());
                    if (tradePool != null) {
                        tradePools.put(traderId, tradePool);
                    }
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }
    }

    private TraderData parseTraderData(String traderId, JsonObject json) {
        try {
            String profession = json.has("profession") ?
                    json.get("profession").getAsString() : traderId;

            ItemStack currencyItem = new ItemStack(net.minecraft.world.item.Items.EMERALD);
            if (json.has("currency")) {
                JsonObject currencyObj = json.getAsJsonObject("currency");
                if (currencyObj.has("item")) {
                    String itemId = currencyObj.get("item").getAsString();
                    var item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
                    if (item != null) {
                        currencyItem = new ItemStack(item);
                    }
                }
            }

            int initialMoney = json.has("initial_money") ?
                    json.get("initial_money").getAsInt() : 100;

            int restockDays = json.has("restock_days") ?
                    json.get("restock_days").getAsInt() : 3;

            return new TraderData(profession, currencyItem, initialMoney, restockDays);

        } catch (Exception e) {
            return null;
        }
    }

    private TradePool parseTradePool(String traderId, JsonObject json) {
        try {
            TradePool pool = new TradePool(traderId);

            if (json.has("currency")) {
                JsonObject currencyObj = json.getAsJsonObject("currency");
                if (currencyObj.has("item")) {
                    String itemId = currencyObj.get("item").getAsString();
                    var item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
                    if (item != null) {
                        pool.setCurrencyOverride(new ItemStack(item));
                    }
                }
            }

            if (json.has("initial_money")) {
                pool.setInitialMoneyOverride(json.get("initial_money").getAsInt());
            }

            if (json.has("restock_days")) {
                pool.setRestockDaysOverride(json.get("restock_days").getAsInt());
            }

            if (json.has("trades")) {
                JsonArray tradesArray = json.getAsJsonArray("trades");
                for (JsonElement tradeElement : tradesArray) {
                    JsonObject tradeObj = tradeElement.getAsJsonObject();
                    TradeEntry entry = parseTradeEntry(tradeObj);
                    if (entry != null) {
                        pool.addTrade(entry);
                    }
                }
            }

            return pool;
        } catch (Exception e) {
            return null;
        }
    }

    private TradeEntry parseTradeEntry(JsonObject tradeObj) {
        try {
            String typeStr = tradeObj.get("type").getAsString().toUpperCase();
            TradeEntry.TradeType type = TradeEntry.TradeType.valueOf(typeStr);

            int weight = tradeObj.has("weight") ? tradeObj.get("weight").getAsInt() : 50;
            int maxUses = tradeObj.has("max_uses") ? tradeObj.get("max_uses").getAsInt() : 5;

            ItemStack item = parseItemStack(tradeObj.getAsJsonObject("item"));
            int price = tradeObj.get("price").getAsInt();

            int maxStock = 0;
            if (type == TradeEntry.TradeType.SELL && tradeObj.has("stock")) {
                maxStock = tradeObj.get("stock").getAsInt();
            } else if (type == TradeEntry.TradeType.SELL) {
                maxStock = item.getCount();
            }

            return new TradeEntry(type, item, price, maxUses, weight, maxStock);

        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack parseItemStack(JsonObject itemObj) {
        try {
            String itemId = itemObj.get("id").getAsString();
            int count = itemObj.has("count") ? itemObj.get("count").getAsInt() : 1;

            var item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
            if (item == null) {
                return ItemStack.EMPTY;
            }

            ItemStack stack = new ItemStack(item, count);

            if (itemObj.has("nbt")) {
                try {
                    String nbtString = itemObj.get("nbt").toString();
                    CompoundTag nbt = TagParser.parseTag(nbtString);
                    stack.setTag(nbt);
                } catch (Exception e) {
                }
            }

            return stack;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public TraderData getTrader(String traderId) {
        return traders.get(traderId);
    }

    public TraderData getDefaultTrader() {
        return new TraderData(
                "default_trader",
                new ItemStack(net.minecraft.world.item.Items.EMERALD),
                100,
                3
        );
    }

    public boolean hasTrader(String traderId) {
        return traders.containsKey(traderId);
    }

    public Map<String, TraderData> getAllTraders() {
        return new HashMap<>(traders);
    }

    public TradePool getTradePool(String traderId) {
        return tradePools.get(traderId);
    }

    public boolean hasTradePool(String traderId) {
        return tradePools.containsKey(traderId);
    }

    public List<TradeEntry> getRandomizedTrades(String traderId, int maxCount, Random random) {
        TradePool pool = tradePools.get(traderId);
        if (pool == null) {
            return new ArrayList<>();
        }

        List<TradeEntry> allTrades = pool.getTrades();
        if (allTrades.size() <= maxCount) {
            return new ArrayList<>(allTrades);
        }

        return weightedRandomSelection(allTrades, maxCount, random);
    }

    private List<TradeEntry> weightedRandomSelection(List<TradeEntry> trades, int count, Random random) {
        List<TradeEntry> result = new ArrayList<>();
        List<TradeEntry> remaining = new ArrayList<>(trades);

        for (int i = 0; i < count && !remaining.isEmpty(); i++) {
            int totalWeight = remaining.stream().mapToInt(TradeEntry::getWeight).sum();
            int randomWeight = random.nextInt(totalWeight);

            int currentWeight = 0;
            TradeEntry selected = null;

            for (TradeEntry trade : remaining) {
                currentWeight += trade.getWeight();
                if (currentWeight > randomWeight) {
                    selected = trade;
                    break;
                }
            }

            if (selected != null) {
                result.add(selected);
                remaining.remove(selected);
            }
        }

        return result;
    }

    public Set<String> getLoadedTraderTypes() {
        return new HashSet<>(traders.keySet());
    }

    public Set<String> getLoadedTradePools() {
        return new HashSet<>(tradePools.keySet());
    }

    public static class TradePool {
        private final String traderId;
        private final List<TradeEntry> trades = new ArrayList<>();
        private ItemStack currencyOverride = null;
        private Integer initialMoneyOverride = null;
        private Integer restockDaysOverride = null;

        public TradePool(String traderId) {
            this.traderId = traderId;
        }

        public void addTrade(TradeEntry trade) {
            trades.add(trade);
        }

        public List<TradeEntry> getTrades() {
            return new ArrayList<>(trades);
        }

        public String getTraderId() {
            return traderId;
        }

        public void setCurrencyOverride(ItemStack currency) {
            this.currencyOverride = currency;
        }

        public void setInitialMoneyOverride(int money) {
            this.initialMoneyOverride = money;
        }

        public void setRestockDaysOverride(int days) {
            this.restockDaysOverride = days;
        }

        public ItemStack getCurrencyOverride() {
            return currencyOverride != null ? currencyOverride.copy() : null;
        }

        public Integer getInitialMoneyOverride() {
            return initialMoneyOverride;
        }

        public Integer getRestockDaysOverride() {
            return restockDaysOverride;
        }
    }
}