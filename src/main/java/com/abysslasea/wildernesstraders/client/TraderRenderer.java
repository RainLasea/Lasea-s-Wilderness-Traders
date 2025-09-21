package com.abysslasea.wildernesstraders.client;

import com.abysslasea.wildernesstraders.entity.TraderEntity;
import com.abysslasea.wildernesstraders.client.model.TraderGeoModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class TraderRenderer extends MobRenderer<TraderEntity, VillagerModel<TraderEntity>> {

    private static final Map<String, ResourceCheckResult> RESOURCE_CACHE = new ConcurrentHashMap<>();
    private static final String DEFAULT_TRADER_NAME = "default_trader";
    private static final boolean GECKOLIB_AVAILABLE = isGeckoLibAvailable();
    private static final ResourceLocation VANILLA_VILLAGER_TEXTURE = new ResourceLocation("textures/entity/villager/villager.png");

    private final TraderGeoModel geoModel;
    private Object geckoRenderer;

    public TraderRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.geoModel = GECKOLIB_AVAILABLE ? new TraderGeoModel() : null;

        if (GECKOLIB_AVAILABLE) {
            try {
                initializeGeckoRenderer(context);
            } catch (Exception e) {
                // Silently fall back to villager model
            }
        }
    }

    private static boolean isGeckoLibAvailable() {
        try {
            Class.forName("software.bernie.geckolib.renderer.GeoEntityRenderer");
            Class.forName("software.bernie.geckolib.model.GeoModel");
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        }
    }

    private void initializeGeckoRenderer(EntityRendererProvider.Context context) {
        if (!GECKOLIB_AVAILABLE) return;

        try {
            Class<?> geoRendererClass = Class.forName("software.bernie.geckolib.renderer.GeoEntityRenderer");
            geckoRenderer = createGeckoRendererInstance(context, geoRendererClass);
        } catch (Exception e) {
            // Silently fall back
        }
    }

    private Object createGeckoRendererInstance(EntityRendererProvider.Context context, Class<?> geoRendererClass) {
        return null;
    }

    private static class ResourceCheckResult {
        final boolean hasGeckoLibResources;
        final long checkTime;

        ResourceCheckResult(boolean hasGeckoLib) {
            this.hasGeckoLibResources = hasGeckoLib;
            this.checkTime = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - checkTime > 30000;
        }
    }

    @Override
    public void render(TraderEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {

        String traderName = getValidTraderName(entity);
        ResourceCheckResult resources = getResourceCheckResult(traderName);

        if (GECKOLIB_AVAILABLE && resources.hasGeckoLibResources && geckoRenderer != null) {
            renderWithGeckoLib(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        } else {
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }

    private ResourceCheckResult getResourceCheckResult(String traderName) {
        ResourceCheckResult cached = RESOURCE_CACHE.get(traderName);
        if (cached != null && !cached.isExpired()) {
            return cached;
        }

        ResourceCheckResult result = checkResources(traderName);
        RESOURCE_CACHE.put(traderName, result);
        return result;
    }

    private ResourceCheckResult checkResources(String traderName) {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        boolean hasGeckoLibResources = false;

        if (GECKOLIB_AVAILABLE) {
            hasGeckoLibResources = checkGeckoLibResourcesInOrder(resourceManager, traderName);
        }

        return new ResourceCheckResult(hasGeckoLibResources);
    }

    private boolean checkGeckoLibResourcesInOrder(ResourceManager resourceManager, String traderName) {
        ResourceLocation geoLocation = getGeoLocation(traderName);
        if (!resourceManager.getResource(geoLocation).isPresent()) {
            return false;
        }

        ResourceLocation animationLocation = getAnimationLocation(traderName);
        if (!resourceManager.getResource(animationLocation).isPresent()) {
            return false;
        }

        ResourceLocation textureLocation = getGeckoLibTextureLocation(traderName);
        return resourceManager.getResource(textureLocation).isPresent();
    }

    private void renderWithGeckoLib(TraderEntity entity, float entityYaw, float partialTicks,
                                    PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        try {
            if (geckoRenderer != null && geoModel != null) {
                super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            } else {
                super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
            }
        } catch (Exception e) {
            super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(TraderEntity trader) {
        String traderName = getValidTraderName(trader);
        ResourceCheckResult resources = getResourceCheckResult(traderName);

        if (GECKOLIB_AVAILABLE && resources.hasGeckoLibResources) {
            return getGeckoLibTextureLocation(traderName);
        } else {
            return VANILLA_VILLAGER_TEXTURE;
        }
    }

    private String getValidTraderName(TraderEntity trader) {
        String traderName = trader.getTraderName();
        return (traderName == null || traderName.isEmpty()) ? DEFAULT_TRADER_NAME : traderName;
    }

    public ResourceLocation getGeoLocation(String traderName) {
        return new ResourceLocation("wildernesstraders", "geo/" + traderName + ".geo.json");
    }

    public ResourceLocation getAnimationLocation(String traderName) {
        return new ResourceLocation("wildernesstraders", "animations/" + traderName + ".animation.json");
    }

    private ResourceLocation getGeckoLibTextureLocation(String traderName) {
        return new ResourceLocation("wildernesstraders", "textures/entity/" + traderName + ".png");
    }

    public static void clearResourceCache() {
        RESOURCE_CACHE.clear();
    }

    public static void invalidateCache(String traderName) {
        RESOURCE_CACHE.remove(traderName);
    }

    public static boolean isGeckoLibSupported() {
        return GECKOLIB_AVAILABLE;
    }
}