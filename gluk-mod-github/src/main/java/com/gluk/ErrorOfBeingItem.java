package com.gluk;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

/**
 * «Ошибка Бытия»: урон и скорость как у незеритового меча, прочность бесконечная.
 * ПКМ на бегу = выпад как у копья (пробивает всех на пути уроном меча).
 */
public class ErrorOfBeingItem extends SwordItem {

    public ErrorOfBeingItem(Properties props) {
        super(Tiers.NETHERITE, 3, -2.4f, props);
    }

    /** Бесконечная прочность. */
    @Override
    public boolean canBeDepleted() {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSprinting() && !player.getCooldowns().isOnCooldown(this)) {
            Vec3 look = player.getLookAngle();
            Vec3 dir = new Vec3(look.x, 0, look.z);
            if (dir.lengthSqr() > 1.0E-4) {
                if (!level.isClientSide && player instanceof ServerPlayer sp) {
                    ServerEvents.startLunge(sp, dir.normalize());
                }
                player.getCooldowns().addCooldown(this, ServerEvents.LUNGE_COOLDOWN);
                player.swing(hand);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }
        return InteractionResultHolder.pass(stack);
    }
}
