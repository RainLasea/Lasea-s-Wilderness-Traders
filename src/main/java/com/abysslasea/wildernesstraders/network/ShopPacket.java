package com.abysslasea.wildernesstraders.network;

import com.abysslasea.wildernesstraders.NetworkHandler;
import com.abysslasea.wildernesstraders.shop.ShopContainer;
import com.abysslasea.wildernesstraders.trader.TradeEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ShopPacket {

    public enum Type {
        OPEN_SHOP,
        EXECUTE_TRADE,
        SYNC_DATA
    }

    private final Type type;
    private final int entityId;
    private final String npcName;
    private final String npcId;
    private final List<TradeEntry> saleEntries;
    private final List<TradeEntry> buyEntries;
    private final int npcMoney;
    private final int playerCurrency;

    public ShopPacket(Type type, int entityId, String npcName, String npcId) {
        this.type = type;
        this.entityId = entityId;
        this.npcName = npcName;
        this.npcId = npcId;
        this.saleEntries = new ArrayList<>();
        this.buyEntries = new ArrayList<>();
        this.npcMoney = 0;
        this.playerCurrency = 0;
    }

    public ShopPacket(Type type, int entityId, String npcName, String npcId,
                      List<TradeEntry> saleEntries, List<TradeEntry> buyEntries,
                      int npcMoney, int playerCurrency) {
        this.type = type;
        this.entityId = entityId;
        this.npcName = npcName;
        this.npcId = npcId;
        this.saleEntries = new ArrayList<>(saleEntries);
        this.buyEntries = new ArrayList<>(buyEntries);
        this.npcMoney = npcMoney;
        this.playerCurrency = playerCurrency;
    }

    public static void encode(ShopPacket pkt, FriendlyByteBuf buf) {
        buf.writeEnum(pkt.type);
        buf.writeInt(pkt.entityId);
        buf.writeUtf(pkt.npcName);
        buf.writeUtf(pkt.npcId);

        buf.writeInt(pkt.saleEntries.size());
        for (TradeEntry entry : pkt.saleEntries) {
            buf.writeItem(entry.getItem());
            buf.writeInt(entry.getPrice());
            buf.writeInt(entry.getCurrentStock());
            buf.writeInt(entry.getMaxStock());
            buf.writeInt(entry.getUsedCount());
            buf.writeInt(entry.getMaxUses());
            buf.writeInt(entry.getWeight());
        }

        buf.writeInt(pkt.buyEntries.size());
        for (TradeEntry entry : pkt.buyEntries) {
            buf.writeItem(entry.getItem());
            buf.writeInt(entry.getPrice());
            buf.writeInt(entry.getUsedCount());
            buf.writeInt(entry.getMaxUses());
            buf.writeInt(entry.getWeight());
        }

        buf.writeInt(pkt.npcMoney);
        buf.writeInt(pkt.playerCurrency);
    }

    public static ShopPacket decode(FriendlyByteBuf buf) {
        Type type = buf.readEnum(Type.class);
        int entityId = buf.readInt();
        String npcName = buf.readUtf();
        String npcId = buf.readUtf();

        int saleCount = buf.readInt();
        List<TradeEntry> saleEntries = new ArrayList<>();
        for (int i = 0; i < saleCount; i++) {
            ItemStack item = buf.readItem();
            int price = buf.readInt();
            int currentStock = buf.readInt();
            int maxStock = buf.readInt();
            int usedCount = buf.readInt();
            int maxUses = buf.readInt();
            int weight = buf.readInt();

            TradeEntry entry = TradeEntry.createWithCurrentStock(
                    TradeEntry.TradeType.SELL, item, price, maxUses, weight, maxStock, currentStock, usedCount);
            saleEntries.add(entry);
        }

        int buyCount = buf.readInt();
        List<TradeEntry> buyEntries = new ArrayList<>();
        for (int i = 0; i < buyCount; i++) {
            ItemStack item = buf.readItem();
            int price = buf.readInt();
            int usedCount = buf.readInt();
            int maxUses = buf.readInt();
            int weight = buf.readInt();

            TradeEntry entry = TradeEntry.createWithCurrentStock(
                    TradeEntry.TradeType.BUY, item, price, maxUses, weight, 0, 0, usedCount);
            buyEntries.add(entry);
        }

        int npcMoney = buf.readInt();
        int playerCurrency = buf.readInt();

        return new ShopPacket(type, entityId, npcName, npcId, saleEntries, buyEntries, npcMoney, playerCurrency);
    }

    public static void handle(ShopPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        if (context.getDirection().getReceptionSide() == LogicalSide.CLIENT) {
            context.enqueueWork(() -> {
                if (pkt.type == Type.SYNC_DATA) {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.screen instanceof com.abysslasea.wildernesstraders.shop.ShopContainerScreen) {
                        com.abysslasea.wildernesstraders.shop.ShopContainerScreen screen =
                                (com.abysslasea.wildernesstraders.shop.ShopContainerScreen) mc.screen;
                        ShopContainer container = screen.getMenu();
                        container.updateClientData(pkt.saleEntries, pkt.buyEntries, pkt.npcMoney, pkt.playerCurrency);
                    }
                }
            });
        } else if (context.getDirection().getReceptionSide() == LogicalSide.SERVER) {
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;

                switch (pkt.type) {
                    case OPEN_SHOP:
                        handleOpenShop(pkt, player);
                        break;

                    case EXECUTE_TRADE:
                        handleExecuteTrade(player);
                        break;
                }
            });
        }

        context.setPacketHandled(true);
    }

    private static void handleOpenShop(ShopPacket pkt, ServerPlayer player) {
        Entity entity = player.level().getEntity(pkt.entityId);
        if (entity == null) return;

        List<TradeEntry> saleEntries = new ArrayList<>();
        List<TradeEntry> buyEntries = new ArrayList<>();
        int npcMoney = 64;
        int playerCurrency = 0;

        if (entity instanceof com.abysslasea.wildernesstraders.entity.TraderEntity traderEntity) {
            try {
                com.abysslasea.wildernesstraders.trader.TraderEntityData traderData =
                        new com.abysslasea.wildernesstraders.trader.TraderEntityData(traderEntity);
                saleEntries = traderData.getSaleEntries();
                buyEntries = traderData.getBuyEntries();
                npcMoney = traderData.getCurrentMoney();

                ItemStack currencyItem = traderData.getCurrencyItem();
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (ItemStack.isSameItem(stack, currencyItem)) {
                        playerCurrency += stack.getCount();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("screen.wildernesstraders.shop_title", pkt.npcName);
            }

            @Override
            public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
                return ShopContainer.create(windowId, playerInventory, pkt.entityId, pkt.npcName, pkt.npcId);
            }
        };

        final List<TradeEntry> finalSaleEntries = saleEntries;
        final List<TradeEntry> finalBuyEntries = buyEntries;
        final int finalNpcMoney = npcMoney;
        final int finalPlayerCurrency = playerCurrency;

        try {
            NetworkHooks.openScreen(player, menuProvider, buf -> {
                buf.writeInt(pkt.entityId);
                buf.writeUtf(pkt.npcName);
                buf.writeUtf(pkt.npcId);

                buf.writeInt(finalSaleEntries.size());
                for (TradeEntry entry : finalSaleEntries) {
                    buf.writeItem(entry.getItem());
                    buf.writeInt(entry.getPrice());
                    buf.writeInt(entry.getCurrentStock());
                    buf.writeInt(entry.getMaxStock());
                }

                buf.writeInt(finalBuyEntries.size());
                for (TradeEntry entry : finalBuyEntries) {
                    buf.writeItem(entry.getItem());
                    buf.writeInt(entry.getPrice());
                }

                buf.writeInt(finalNpcMoney);
                buf.writeInt(finalPlayerCurrency);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleExecuteTrade(ServerPlayer player) {
        if (player.containerMenu instanceof ShopContainer shopContainer) {
            try {
                shopContainer.executeBatchTrade();

                ShopPacket syncPacket = ShopPacket.syncData(
                        shopContainer.getEntityId(),
                        shopContainer.getNpcName(),
                        shopContainer.getNpcId(),
                        shopContainer.getSaleEntries(),
                        shopContainer.getBuyEntries(),
                        shopContainer.getNpcMoney(),
                        shopContainer.getPlayerCurrency()
                );

                NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), syncPacket);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static ShopPacket openShop(int entityId, String npcName, String npcId) {
        return new ShopPacket(Type.OPEN_SHOP, entityId, npcName, npcId);
    }

    public static ShopPacket executeTrade() {
        return new ShopPacket(Type.EXECUTE_TRADE, -1, "", "");
    }

    public static ShopPacket syncData(int entityId, String npcName, String npcId,
                                      List<TradeEntry> saleEntries, List<TradeEntry> buyEntries,
                                      int npcMoney, int playerCurrency) {
        return new ShopPacket(Type.SYNC_DATA, entityId, npcName, npcId, saleEntries, buyEntries, npcMoney, playerCurrency);
    }
}