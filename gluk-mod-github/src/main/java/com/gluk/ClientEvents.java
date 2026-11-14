package com.gluk;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GlukMod.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().player == null) return;
        while (ClientSetup.KEY_PORTAL.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new KeyPacket(KeyPacket.PORTAL));
        }
        while (ClientSetup.KEY_BLINK.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new KeyPacket(KeyPacket.BLINK));
        }
    }
}
