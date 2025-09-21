package com.abysslasea.wildernesstraders.client.model;

import com.abysslasea.wildernesstraders.entity.TraderEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import software.bernie.geckolib.model.GeoModel;

public class TraderGeoModel extends GeoModel<TraderEntity> {

    private static final String DEFAULT_TRADER_NAME = "default_trader";

    @Override
    public ResourceLocation getModelResource(TraderEntity entity) {
        try {
            String traderName = getValidTraderName(entity);
            return new ResourceLocation("wildernesstraders", "geo/" + traderName + ".geo.json");
        } catch (Exception e) {
            return new ResourceLocation("wildernesstraders", "geo/" + DEFAULT_TRADER_NAME + ".geo.json");
        }
    }

    @Override
    public ResourceLocation getTextureResource(TraderEntity entity) {
        try {
            String traderName = getValidTraderName(entity);
            return new ResourceLocation("wildernesstraders", "textures/entity/" + traderName + ".png");
        } catch (Exception e) {
            return new ResourceLocation("wildernesstraders", "textures/entity/" + DEFAULT_TRADER_NAME + ".png");
        }
    }

    @Override
    public ResourceLocation getAnimationResource(TraderEntity entity) {
        try {
            String traderName = getValidTraderName(entity);
            return new ResourceLocation("wildernesstraders", "animations/" + traderName + ".animation.json");
        } catch (Exception e) {
            return new ResourceLocation("wildernesstraders", "animations/" + DEFAULT_TRADER_NAME + ".animation.json");
        }
    }

    private String getValidTraderName(TraderEntity entity) {
        try {
            if (entity == null) {
                return DEFAULT_TRADER_NAME;
            }

            String traderName = entity.getTraderName();
            if (traderName == null || traderName.isEmpty()) {
                return DEFAULT_TRADER_NAME;
            }

            return traderName;
        } catch (Exception e) {
            return DEFAULT_TRADER_NAME;
        }
    }

    public boolean hasValidResources(TraderEntity entity) {
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            if (resourceManager == null) {
                return false;
            }

            String traderName = getValidTraderName(entity);
            boolean hasAllResources = true;

            ResourceLocation geoLocation = getModelResource(entity);
            if (!resourceManager.getResource(geoLocation).isPresent()) {
                hasAllResources = false;
            }

            ResourceLocation animationLocation = getAnimationResource(entity);
            if (!resourceManager.getResource(animationLocation).isPresent()) {
                hasAllResources = false;
            }

            ResourceLocation textureLocation = getTextureResource(entity);
            if (!resourceManager.getResource(textureLocation).isPresent()) {
                hasAllResources = false;
            }

            if (hasAllResources) {
                try {
                    checkAnimationContent(entity, traderName);
                } catch (Exception e) {
                }
            }

            return hasAllResources;

        } catch (Exception e) {
            return false;
        }
    }

    private void checkAnimationContent(TraderEntity entity, String traderName) {
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            ResourceLocation animationLocation = getAnimationResource(entity);

            var resourceOptional = resourceManager.getResource(animationLocation);
            if (!resourceOptional.isPresent()) {
                return;
            }

            try (var inputStream = resourceOptional.get().open()) {
                String content = new String(inputStream.readAllBytes());

                boolean hasIdle = content.contains("\"idle\"") || content.contains("'idle'");
                boolean hasWalk = content.contains("\"walk\"") || content.contains("'walk'");
                boolean hasTalk = content.contains("\"talk\"") || content.contains("'talk'");

                if (entity != null) {
                    entity.setAnimationAvailability(hasIdle, hasWalk, hasTalk);
                }

            } catch (Exception e) {
                if (entity != null) {
                    entity.setAnimationAvailability(false, false, false);
                }
            }
        } catch (Exception e) {
        }
    }

    public ResourceLocation getDefaultModelResource() {
        return new ResourceLocation("wildernesstraders", "geo/" + DEFAULT_TRADER_NAME + ".geo.json");
    }

    public ResourceLocation getDefaultTextureResource() {
        return new ResourceLocation("wildernesstraders", "textures/entity/" + DEFAULT_TRADER_NAME + ".png");
    }

    public ResourceLocation getDefaultAnimationResource() {
        return new ResourceLocation("wildernesstraders", "animations/" + DEFAULT_TRADER_NAME + ".animation.json");
    }
}