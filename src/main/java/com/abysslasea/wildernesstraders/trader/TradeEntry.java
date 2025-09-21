package com.abysslasea.wildernesstraders.trader;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TradeEntry {

    public enum TradeType {
        SELL,  // 商人卖给玩家
        BUY    // 商人从玩家买入
    }

    private final TradeType type;
    private final ItemStack item;
    private final int price;
    private final int maxUses;
    private final int weight;
    private final int maxStock;

    private int currentStock;
    private int usedCount;

    public TradeEntry(TradeType type, ItemStack item, int price, int maxUses, int weight) {
        this(type, item, price, maxUses, weight, item.getCount());
    }

    public TradeEntry(TradeType type, ItemStack item, int price, int maxUses, int weight, int maxStock) {
        this.type = type;
        this.item = item.copy();
        this.price = Math.max(0, price);
        this.maxUses = Math.max(1, maxUses);
        this.weight = Math.max(1, weight);
        this.maxStock = Math.max(0, maxStock);
        this.currentStock = this.maxStock;
        this.usedCount = 0;
    }

    // 基本getter方法
    public TradeType getType() {
        return type;
    }

    public ItemStack getItem() {
        return item.copy();
    }

    public int getPrice() {
        return price;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getWeight() {
        return weight;
    }

    public int getMaxStock() {
        return maxStock;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public int getUsedCount() {
        return usedCount;
    }

    // 新增：设置当前库存的方法（用于网络同步）
    public void setCurrentStock(int stock) {
        this.currentStock = Math.max(0, Math.min(stock, maxStock));
    }

    // 新增：设置使用次数的方法（用于网络同步）
    public void setUsedCount(int count) {
        this.usedCount = Math.max(0, Math.min(count, maxUses));
    }

    // 类型检查方法
    public boolean isSell() {
        return type == TradeType.SELL;
    }

    public boolean isBuy() {
        return type == TradeType.BUY;
    }

    // 库存相关方法
    public boolean hasStock() {
        if (type == TradeType.BUY) {
            return true; // 买入交易不需要库存限制
        }
        return currentStock > 0;
    }

    public boolean canSell(int amount) {
        if (type == TradeType.BUY) {
            return true; // 买入交易不需要库存检查
        }
        return currentStock >= amount;
    }

    public void consumeStock(int amount) {
        if (type == TradeType.SELL) {
            currentStock = Math.max(0, currentStock - amount);
        }
        usedCount++;
    }

    public void resetStock() {
        currentStock = maxStock;
        usedCount = 0;
    }

    // 交易状态检查
    public boolean isDisabled() {
        return usedCount >= maxUses;
    }

    public boolean canTrade() {
        return !isDisabled() && hasStock();
    }

    // NBT序列化
    public CompoundTag toNBT() {
        try {
            CompoundTag tag = new CompoundTag();

            tag.putString("Type", type.name());
            tag.putInt("Price", price);
            tag.putInt("MaxUses", maxUses);
            tag.putInt("Weight", weight);
            tag.putInt("MaxStock", maxStock);
            tag.putInt("CurrentStock", currentStock);
            tag.putInt("UsedCount", usedCount);

            // 序列化物品
            CompoundTag itemTag = new CompoundTag();
            item.save(itemTag);
            tag.put("Item", itemTag);

            return tag;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // NBT反序列化
    public static TradeEntry fromNBT(CompoundTag tag) {
        try {
            String typeStr = tag.getString("Type");
            TradeType type = TradeType.valueOf(typeStr);

            int price = tag.getInt("Price");
            int maxUses = tag.getInt("MaxUses");
            int weight = tag.getInt("Weight");
            int maxStock = tag.getInt("MaxStock");
            int currentStock = tag.getInt("CurrentStock");
            int usedCount = tag.getInt("UsedCount");

            // 反序列化物品
            CompoundTag itemTag = tag.getCompound("Item");
            ItemStack item = ItemStack.of(itemTag);

            if (item.isEmpty()) {
                item = new ItemStack(Items.BARRIER); // 安全默认值
            }

            TradeEntry entry = new TradeEntry(type, item, price, maxUses, weight, maxStock);
            entry.currentStock = currentStock;
            entry.usedCount = usedCount;

            return entry;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 创建简化的交易条目
    public static TradeEntry createSellEntry(ItemStack item, int price) {
        return new TradeEntry(TradeType.SELL, item, price, 5, 50);
    }

    public static TradeEntry createBuyEntry(ItemStack item, int price) {
        return new TradeEntry(TradeType.BUY, item, price, 5, 50, 0);
    }

    public static TradeEntry createSellEntry(ItemStack item, int price, int stock) {
        return new TradeEntry(TradeType.SELL, item, price, 5, 50, stock);
    }

    public static TradeEntry createBuyEntry(ItemStack item, int price, int maxUses) {
        return new TradeEntry(TradeType.BUY, item, price, maxUses, 50, 0);
    }

    // 新增：创建带有完整数据的交易条目（用于网络同步）
    public static TradeEntry createWithCurrentStock(TradeType type, ItemStack item, int price, int maxUses, int weight, int maxStock, int currentStock, int usedCount) {
        TradeEntry entry = new TradeEntry(type, item, price, maxUses, weight, maxStock);
        entry.setCurrentStock(currentStock);
        entry.setUsedCount(usedCount);
        return entry;
    }

    @Override
    public String toString() {
        return String.format("TradeEntry{type=%s, item=%s, price=%d, stock=%d/%d, uses=%d/%d}",
                type, item.getDisplayName().getString(), price, currentStock, maxStock, usedCount, maxUses);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TradeEntry that = (TradeEntry) obj;
        return type == that.type &&
                price == that.price &&
                ItemStack.isSameItemSameTags(item, that.item);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + item.getItem().hashCode();
        result = 31 * result + price;
        return result;
    }
}