package com.abysslasea.wildernesstraders.client;

import com.abysslasea.wildernesstraders.entity.TraderEntity;
import com.abysslasea.wildernesstraders.client.model.TraderGeoModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@OnlyIn(Dist.CLIENT)
public class TraderRenderer extends GeoEntityRenderer<TraderEntity> {

    public TraderRenderer(EntityRendererProvider.Context context) {
        super(context, new TraderGeoModel());
    }

    @Override
    public ResourceLocation getTextureLocation(TraderEntity entity) {
        return this.model.getTextureResource(entity);
    }
}