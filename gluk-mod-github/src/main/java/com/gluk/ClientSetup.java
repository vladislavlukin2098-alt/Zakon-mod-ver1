package com.gluk;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = GlukMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    public static final KeyMapping KEY_PORTAL =
            new KeyMapping("key.gluk.portal", GLFW.GLFW_KEY_H, "key.categories.gluk");
    public static final KeyMapping KEY_BLINK =
            new KeyMapping("key.gluk.blink", GLFW.GLFW_KEY_X, "key.categories.gluk");

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(GlukMod.GLUK.get(), GlukRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY_PORTAL);
        event.register(KEY_BLINK);
    }
}
