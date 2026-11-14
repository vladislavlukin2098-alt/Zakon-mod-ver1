package com.gluk;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Рисует Глюка моделью игрока (Steve, широкие руки) с твоей текстурой. */
public class GlukRenderer extends MobRenderer<GlukEntity, PlayerModel<GlukEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(GlukMod.MOD_ID, "textures/entity/gluk.png");

    public GlukRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(GlukEntity entity) {
        return TEXTURE;
    }
}
