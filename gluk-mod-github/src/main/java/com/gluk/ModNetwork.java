package com.gluk;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GlukMod.MOD_ID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(0, KeyPacket.class, KeyPacket::encode, KeyPacket::decode, KeyPacket::handle);
    }
}
