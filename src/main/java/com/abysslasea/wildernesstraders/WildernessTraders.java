package com.abysslasea.wildernesstraders;

import com.abysslasea.wildernesstraders.dialogue.DialogueManager;
import com.abysslasea.wildernesstraders.entity.ModEntities;
import com.abysslasea.wildernesstraders.entity.TraderEntity;
import com.abysslasea.wildernesstraders.shop.ShopContainer;
import com.abysslasea.wildernesstraders.shop.ShopContainerScreen;
import com.abysslasea.wildernesstraders.trader.TradeEntry;
import com.abysslasea.wildernesstraders.trader.TraderResourceManager;
import com.abysslasea.wildernesstraders.worldgen.ModStructureData;
import com.abysslasea.wildernesstraders.worldgen.ModStructures;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.data.event.GatherDataEvent;
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
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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

        ModStructures.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        ModEntities.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::setupEntityAttributes);
        modEventBus.addListener(this::gatherData);

        NetworkHandler.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::clientSetup);
        }

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            TraderConfig config = TraderConfig.INSTANCE;
        });
    }

    private void setupEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.TRADER.get(), TraderEntity.createAttributes().build());
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        MenuScreens.register(SHOP_CONTAINER_TYPE.get(), ShopContainerScreen::new);
    }

    private void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(
                packOutput, lookupProvider, Set.of(MODID)));
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

    public static class DatapackBuiltinEntriesProvider extends net.minecraftforge.common.data.DatapackBuiltinEntriesProvider {
        public DatapackBuiltinEntriesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, Set<String> modIds) {
            super(output, provider,
                    new RegistrySetBuilder()
                            .add(Registries.STRUCTURE, ModStructureData::bootstrap)
                            .add(Registries.STRUCTURE_SET, ModStructureData::bootstrapStructureSet),
                    modIds);
        }
    }
}