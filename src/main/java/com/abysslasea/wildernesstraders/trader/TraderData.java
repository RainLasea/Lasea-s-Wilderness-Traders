package com.abysslasea.wildernesstraders.trader;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class TraderData {
    private final String profession;
    private final ItemStack currencyItem;
    private final int initialMoney;
    private final int restockDays;

    public TraderData(String profession, ItemStack currencyItem, int initialMoney, int restockDays) {
        this.profession = profession;
        this.currencyItem = currencyItem != null ? currencyItem : new ItemStack(Items.EMERALD);
        this.initialMoney = Math.max(0, initialMoney);
        this.restockDays = Math.max(1, restockDays);
    }

    public String getProfession() {
        return profession;
    }

    public String getName() {
        return profession;
    }

    public ItemStack getCurrencyItem() {
        return currencyItem.copy();
    }

    public int getInitialMoney() {
        return initialMoney;
    }

    public int getRestockDays() {
        return restockDays;
    }

    public long getRestockTicks() {
        return restockDays * 24000L; // MC days to ticks
    }

    @Override
    public String toString() {
        return String.format("TraderData{profession='%s', currency=%s, money=%d, restock=%d days}",
                profession, currencyItem.getDisplayName().getString(), initialMoney, restockDays);
    }
}