package com.abysslasea.wildernesstraders.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "wildernesstraders");

    public static final RegistryObject<EntityType<TraderEntity>> TRADER = ENTITY_TYPES.register("trader",
            () -> EntityType.Builder.of(TraderEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .build("trader"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}