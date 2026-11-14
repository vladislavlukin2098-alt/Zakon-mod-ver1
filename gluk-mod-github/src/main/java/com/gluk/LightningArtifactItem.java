package com.gluk;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Артефакт молнии: ПКМ включает/выключает. Подробности в ArtifactLogic. */
public class LightningArtifactItem extends Item {
    public LightningArtifactItem(Properties props) { super(props); }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (ArtifactState.LIGHTNING.contains(sp.getUUID())) {
                ArtifactLogic.lightningOff(sp);
                sp.displayClientMessage(Component.translatable("message.gluk.lightning_off"), true);
            } else {
                ArtifactState.LIGHTNING.add(sp.getUUID());
                sp.displayClientMessage(Component.translatable("message.gluk.lightning_on"), true);
                level.playSound(null, sp.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 1.6f);
            }
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
