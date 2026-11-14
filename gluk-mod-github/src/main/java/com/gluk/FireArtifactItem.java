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

/** Огненный артефакт: ПКМ включает/выключает. Подробности в ArtifactLogic. */
public class FireArtifactItem extends Item {
    public FireArtifactItem(Properties props) { super(props); }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (ArtifactState.FIRE.contains(sp.getUUID())) {
                ArtifactLogic.fireOff(sp);
                sp.displayClientMessage(Component.translatable("message.gluk.fire_off"), true);
            } else {
                ArtifactState.FIRE.add(sp.getUUID());
                sp.displayClientMessage(Component.translatable("message.gluk.fire_on"), true);
                level.playSound(null, sp.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1f, 1f);
            }
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
