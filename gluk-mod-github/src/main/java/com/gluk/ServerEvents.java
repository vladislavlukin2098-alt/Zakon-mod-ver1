package com.gluk;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = GlukMod.MOD_ID)
public class ServerEvents {

    // ======== Выпад «Ошибки Бытия» ========
    public static final int LUNGE_COOLDOWN = 30;   // тики между выпадами
    private static final int LUNGE_TICKS = 6;
    private static final double LUNGE_SPEED = 1.1;

    private static final class Lunge {
        final Vec3 dir; int ticks = LUNGE_TICKS; final Set<Integer> hit = new HashSet<>();
        Lunge(Vec3 dir) { this.dir = dir; }
    }
    private static final Map<UUID, Lunge> LUNGES = new ConcurrentHashMap<>();

    public static void startLunge(ServerPlayer sp, Vec3 dir) {
        LUNGES.put(sp.getUUID(), new Lunge(dir));
        sp.setDeltaMovement(dir.x * LUNGE_SPEED, sp.onGround() ? 0.2 : sp.getDeltaMovement().y, dir.z * LUNGE_SPEED);
        sp.hurtMarked = true;
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.8f);
    }

    private static void tickLunge(ServerPlayer sp) {
        Lunge l = LUNGES.get(sp.getUUID());
        if (l == null) return;
        if (l.ticks-- <= 0 || !sp.isAlive() || !sp.getMainHandItem().is(GlukMod.ERROR_OF_BEING.get())) {
            LUNGES.remove(sp.getUUID());
            return;
        }
        Vec3 m = sp.getDeltaMovement();
        sp.setDeltaMovement(l.dir.x * LUNGE_SPEED, m.y, l.dir.z * LUNGE_SPEED);
        sp.hurtMarked = true;
        sp.fallDistance = 0;

        // урон как у незеритового меча: берём реальный атрибут (1 + 7 = 8)
        float dmg = (float) sp.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AABB box = sp.getBoundingBox().inflate(1.0, 0.3, 1.0);
        List<LivingEntity> targets = sp.serverLevel().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != sp && e.isAlive() && !l.hit.contains(e.getId()));
        for (LivingEntity e : targets) {
            l.hit.add(e.getId());
            if (e.hurt(sp.damageSources().playerAttack(sp), dmg)) {
                e.knockback(0.8, -l.dir.x, -l.dir.z);
                sp.serverLevel().sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(),
                        14, 0.3, 0.3, 0.3, 0.2);
                sp.serverLevel().playSound(null, e.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1f, 1f);
            }
        }
    }

    // ======== События ========

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer sp)) return;
        ArtifactLogic.tickFire(sp);
        ArtifactLogic.tickLightning(sp);
        tickLunge(sp);
    }

    /** Огненный артефакт: +10 урона каждым ударом. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p
                && event.getEntity() != p
                && ArtifactState.FIRE.contains(p.getUUID())) {
            event.setAmount(event.getAmount() + ArtifactLogic.FIRE_BONUS_DAMAGE);
        }
    }

    /** Артефакт молнии: молния не причиняет вреда. */
    @SubscribeEvent
    public static void onLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && ArtifactState.LIGHTNING.contains(p.getUUID())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            ArtifactLogic.cleanup(p);
            LUNGES.remove(p.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            ArtifactLogic.cleanup(p);
            LUNGES.remove(p.getUUID());
        }
    }
}
