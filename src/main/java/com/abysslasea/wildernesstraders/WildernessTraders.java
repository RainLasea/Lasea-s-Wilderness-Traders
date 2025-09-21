package com.abysslasea.wildernesstraders;

import com.abysslasea.wildernesstraders.client.TraderRenderer;
import com.abysslasea.wildernesstraders.dialogue.DialogueManager;
import com.abysslasea.wildernesstraders.entity.ModEntities;
import com.abysslasea.wildernesstraders.entity.TraderEntity;
import com.abysslasea.wildernesstraders.shop.ShopContainer;
import com.abysslasea.wildernesstraders.shop.ShopContainerScreen;
import com.abysslasea.wildernesstraders.trader.TradeEntry;
import com.abysslasea.wildernesstraders.trader.TraderResourceManager;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Mod(WildernessTraders.MODID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WildernessTraders {
    public static final String MODID = "wildernesstraders";
    private static final Logger LOGGER = LoggerFactory.getLogger(WildernessTraders.class);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    public static final RegistryObject<MenuType<ShopContainer>> SHOP_CONTAINER_TYPE = MENU_TYPES.register("shop",
            () -> IForgeMenuType.create((windowId, inv, data) -> {
                int entityId = data.readInt();
                String npcName = data.readUtf();
                String npcId = data.readUtf();

                List<TradeEntry> saleEntries = new ArrayList<>();
                List<TradeEntry> buyEntries = new ArrayList<>();
                int npcMoney = 64;
                int playerCurrency = 0;

                try {
                    int saleCount = data.readInt();
                    for (int i = 0; i < saleCount; i++) {
                        ItemStack item = data.readItem();
                        int price = data.readInt();
                        int currentStock = data.readInt();
                        int maxStock = data.readInt();

                        TradeEntry entry = TradeEntry.createWithCurrentStock(
                                TradeEntry.TradeType.SELL, item, price, 5, 50, maxStock, currentStock, 0);
                        saleEntries.add(entry);
                    }

                    int buyCount = data.readInt();
                    for (int i = 0; i < buyCount; i++) {
                        ItemStack item = data.readItem();
                        int price = data.readInt();

                        TradeEntry entry = new TradeEntry(TradeEntry.TradeType.BUY, item, price, 5, 50, 0);
                        buyEntries.add(entry);
                    }

                    npcMoney = data.readInt();
                    playerCurrency = data.readInt();

                } catch (Exception e) {
                    LOGGER.error("Failed to read shop data", e);
                }

                return ShopContainer.createWithData(windowId, inv, entityId, npcName, npcId, saleEntries, buyEntries, npcMoney, playerCurrency);
            }));

    public WildernessTraders(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        MENU_TYPES.register(modEventBus);
        ModEntities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::setupEntityAttributes);

        NetworkHandler.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
            modEventBus.addListener(this::registerEntityRenderers);
        }

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Wilderness Traders mod loading...");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 强制初始化配置
        event.enqueueWork(() -> {
            try {
                LOGGER.info("Initializing trader names config...");
                // 强制触发 TraderNamesConfig 的初始化
                TraderNamesConfig config = TraderNamesConfig.INSTANCE;
                LOGGER.info("Trader names config initialized with {} names", config.getNameCount());

                // 打印配置文件路径用于调试
                LOGGER.info("Config should be located at: {}",
                        net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("trader_names.json"));

            } catch (Exception e) {
                LOGGER.error("Failed to initialize trader names config", e);
            }
        });
    }

    private void setupEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.TRADER.get(), TraderEntity.createAttributes().build());
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                MenuScreens.register(SHOP_CONTAINER_TYPE.get(), ShopContainerScreen::new);
            } catch (Exception e) {
                LOGGER.error("Failed to register menu screen", e);
            }
        });
    }

    private void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TRADER.get(), TraderRenderer::new);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(DialogueManager.INSTANCE);
        event.addListener(TraderResourceManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TraderCommands.register(event.getDispatcher());
    }
}