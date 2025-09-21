package com.abysslasea.wildernesstraders;

import com.abysslasea.wildernesstraders.network.DialoguePacket;
import com.abysslasea.wildernesstraders.network.ShopPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    public static void init() {
        CHANNEL = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(WildernessTraders.MODID, "main"))
                .clientAcceptedVersions(PROTOCOL::equals)
                .serverAcceptedVersions(PROTOCOL::equals)
                .networkProtocolVersion(() -> PROTOCOL)
                .simpleChannel();

        int id = 0;
        CHANNEL.registerMessage(id++, DialoguePacket.class,
                DialoguePacket::encode, DialoguePacket::decode, DialoguePacket::handle);
        CHANNEL.registerMessage(id++, ShopPacket.class,
                ShopPacket::encode, ShopPacket::decode, ShopPacket::handle);
    }
}