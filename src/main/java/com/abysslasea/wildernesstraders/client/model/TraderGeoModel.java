package com.abysslasea.wildernesstraders.client.model;

import com.abysslasea.wildernesstraders.entity.TraderEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import software.bernie.geckolib.model.GeoModel;

public class TraderGeoModel extends GeoModel<TraderEntity> {

    private static final String DEFAULT_TRADER_NAME = "default_trader";

    @Override
    public ResourceLocation getModelResource(TraderEntity entity) {
        String traderName = getValidTraderName(entity);
        return new ResourceLocation("wildernesstraders", "geo/" + traderName + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(TraderEntity entity) {
        String traderName = getValidTraderName(entity);
        return new ResourceLocation("wildernesstraders", "textures/entity/" + traderName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(TraderEntity entity) {
        String traderName = getValidTraderName(entity);
        return new ResourceLocation("wildernesstraders", "animations/" + traderName + ".animation.json");
    }

    private String getValidTraderName(TraderEntity entity) {
        String traderName = entity.getTraderName();
        return (traderName == null || traderName.isEmpty()) ? DEFAULT_TRADER_NAME : traderName;
    }

    public boolean hasValidResources(TraderEntity entity) {
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();

            ResourceLocation geoLocation = getModelResource(entity);
            if (!resourceManager.getResource(geoLocation).isPresent()) {
                return false;
            }

            ResourceLocation animationLocation = getAnimationResource(entity);
            if (!resourceManager.getResource(animationLocation).isPresent()) {
                return false;
            }

            ResourceLocation textureLocation = getTextureResource(entity);
            return resourceManager.getResource(textureLocation).isPresent();

        } catch (Exception e) {
            return false;
        }
    }
}