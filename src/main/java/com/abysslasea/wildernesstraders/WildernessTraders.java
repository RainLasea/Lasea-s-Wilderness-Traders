package com.abysslasea.wildernesstraders;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(WildernessTraders.MODID)
public class WildernessTraders {
    public static final String MODID = "wildernesstraders";

    public WildernessTraders(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
    }
}