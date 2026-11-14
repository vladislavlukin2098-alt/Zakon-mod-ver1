package com.gluk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArtifactLogic {

    // ======== Настройки (можно менять) ========
    /** Бонус урона за каждый удар с огненным артефактом. */
    public static final float FIRE_BONUS_DAMAGE = 10.0f;
    /** Дальность телепорта на X. */
    public static final double BLINK_DISTANCE = 24.0;
    /** Сколько блоков надо пройти для +1 ступени скорости. */
    public static final double BLOCKS_PER_STACK = 100.0;
    /** Прирост скорости за ступень (0.10 = +10% от базовой). */
    public static final double SPEED_PER_STACK = 0.10;
    /** Максимум ступеней скорости. */
    public static final int MAX_STACKS = 30;
    /** Как часто бьёт молния при беге/полёте (в тиках; с ростом скорости чаще). */
    public static final int BOLT_BASE_INTERVAL = 50;
    public static final int BOLT_MIN_INTERVAL = 20;

    private static final UUID SPEED_ID = UUID.fromString("5f1a3c2e-9b44-4a7e-8a10-6c0d2f7d1a01");
    private static final float BASE_FLY_SPEED = 0.05f;

    private static final Map<UUID, Vec3> LAST_POS = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> DIST = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> STACKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_BLINK = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> NEXT_BOLT = new ConcurrentHashMap<>();

    private ArtifactLogic() {}

    // ================= ОГОНЬ =================

    public static void tickFire(ServerPlayer sp) {
        UUID id = sp.getUUID();
        if (!ArtifactState.FIRE.contains(id)) return;
        if (!ArtifactState.hasItem(sp, GlukMod.FIRE_ARTIFACT.get())) {
            ArtifactState.FIRE.remove(id);
            fireOff(sp);
            return;
        }
        if (sp.tickCount % 20 == 0) {
            MobEffectInstance cur = sp.getEffect(MobEffects.FIRE_RESISTANCE);
            if (cur == null || !cur.isInfiniteDuration()) {
                sp.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                        MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
            }
        }
        if (sp.isOnFire()) sp.clearFire();
        if (sp.tickCount % 4 == 0) {
            sp.serverLevel().sendParticles(ParticleTypes.FLAME, sp.getX(), sp.getY() + 1.0, sp.getZ(),
                    1, 0.3, 0.5, 0.3, 0.01);
        }
    }

    public static void fireOff(ServerPlayer sp) {
        ArtifactState.FIRE.remove(sp.getUUID());
        MobEffectInstance cur = sp.getEffect(MobEffects.FIRE_RESISTANCE);
        if (cur != null && cur.isInfiniteDuration()) sp.removeEffect(MobEffects.FIRE_RESISTANCE);
    }

    public static void onKey(ServerPlayer sp, int action) {
        if (!ArtifactState.FIRE.contains(sp.getUUID())) {
            sp.displayClientMessage(Component.translatable("message.gluk.need_fire"), true);
            return;
        }
        if (action == KeyPacket.PORTAL) PortalManager.open(sp);
        else if (action == KeyPacket.BLINK) blink(sp);
    }

    /** Телепорт по взгляду с частицами эндермена. */
    private static void blink(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        long now = level.getGameTime();
        Long last = LAST_BLINK.get(sp.getUUID());
        if (last != null && now - last < 10) return;

        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        Vec3 end = eye.add(look.scale(BLINK_DISTANCE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        Vec3 destEye = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(look.scale(0.6));
        double fx = destEye.x, fz = destEye.z, fy = destEye.y - sp.getEyeHeight();

        AABB box = sp.getBoundingBox().move(fx - sp.getX(), fy - sp.getY(), fz - sp.getZ());
        int tries = 0;
        while (!level.noCollision(sp, box) && tries < 4) {
            box = box.move(0, 0.5, 0);
            fy += 0.5;
            tries++;
        }
        if (!level.noCollision(sp, box)) {
            sp.displayClientMessage(Component.translatable("message.gluk.blink_fail"), true);
            return;
        }
        LAST_BLINK.put(sp.getUUID(), now);
        level.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1, sp.getZ(), 60, 0.4, 0.8, 0.4, 0.6);
        level.playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
        sp.connection.teleport(fx, fy, fz, sp.getYRot(), sp.getXRot());
        sp.resetFallDistance();
        level.sendParticles(ParticleTypes.PORTAL, fx, fy + 1, fz, 60, 0.4, 0.8, 0.4, 0.6);
        level.playSound(null, BlockPos.containing(fx, fy, fz), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
    }

    // ================= МОЛНИЯ =================

    public static void tickLightning(ServerPlayer sp) {
        UUID id = sp.getUUID();
        if (!ArtifactState.LIGHTNING.contains(id)) return;
        if (!ArtifactState.hasItem(sp, GlukMod.LIGHTNING_ARTIFACT.get())) {
            lightningOff(sp);
            return;
        }
        Abilities ab = sp.getAbilities();
        if (!ab.mayfly) {
            ab.mayfly = true;
            sp.onUpdateAbilities();
        }
        ServerLevel level = sp.serverLevel();

        Vec3 pos = sp.position();
        Vec3 last = LAST_POS.put(id, pos);
        double moved = 0;
        if (last != null) {
            double d = pos.distanceTo(last);
            if (d < 10.0) { // большие скачки (телепорты) не считаем
                moved = d;
                double total = DIST.merge(id, d, Double::sum);
                int newStacks = Math.min(MAX_STACKS, (int) (total / BLOCKS_PER_STACK));
                if (newStacks != STACKS.getOrDefault(id, 0)) {
                    STACKS.put(id, newStacks);
                    applySpeed(sp, newStacks);
                }
            }
        }
        int stacks = STACKS.getOrDefault(id, 0);

        // электрическая аура вокруг тела (всегда, пока артефакт включён)
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y + 1.0, pos.z,
                3 + stacks / 3, 0.45, 0.9, 0.45, 0.12);

        // искровой след за игроком при движении
        if (last != null && moved > 0.15) {
            Vec3 vel = pos.subtract(last);
            for (int k = 1; k <= 4; k++) {
                Vec3 p = pos.subtract(vel.scale(k / 4.0));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.9, p.z, 2, 0.2, 0.4, 0.2, 0.02);
            }
        }

        // настоящие молнии бьют в игрока при беге/полёте (только визуально: ничего не поджигают)
        boolean moving = moved > 0.2 || (ab.flying && moved > 0.05);
        if (moving) {
            long now = level.getGameTime();
            Long next = NEXT_BOLT.get(id);
            if (next == null || now >= next) {
                spawnVisualBolt(level,
                        pos.x + (level.random.nextDouble() - 0.5),
                        pos.y,
                        pos.z + (level.random.nextDouble() - 0.5));
                int interval = Math.max(BOLT_MIN_INTERVAL, BOLT_BASE_INTERVAL - stacks);
                NEXT_BOLT.put(id, now + interval + level.random.nextInt(10));
            }
        }
    }

    private static void spawnVisualBolt(ServerLevel level, double x, double y, double z) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return;
        bolt.moveTo(x, y, z);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    private static void applySpeed(ServerPlayer sp, int stacks) {
        AttributeInstance inst = sp.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst != null) {
            inst.removeModifier(SPEED_ID);
            if (stacks > 0) {
                inst.addTransientModifier(new AttributeModifier(SPEED_ID, "gluk_lightning_speed",
                        SPEED_PER_STACK * stacks, AttributeModifier.Operation.MULTIPLY_BASE));
            }
        }
        sp.getAbilities().setFlyingSpeed(BASE_FLY_SPEED * (float) (1.0 + SPEED_PER_STACK * stacks));
        sp.onUpdateAbilities();
    }

    public static void lightningOff(ServerPlayer sp) {
        UUID id = sp.getUUID();
        ArtifactState.LIGHTNING.remove(id);
        LAST_POS.remove(id);
        DIST.remove(id);
        STACKS.remove(id);
        NEXT_BOLT.remove(id);
        AttributeInstance inst = sp.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst != null) inst.removeModifier(SPEED_ID);
        Abilities ab = sp.getAbilities();
        ab.setFlyingSpeed(BASE_FLY_SPEED);
        if (!sp.isCreative() && !sp.isSpectator()) {
            ab.mayfly = false;
            ab.flying = false;
        }
        sp.onUpdateAbilities();
    }

    public static void cleanup(ServerPlayer sp) {
        fireOff(sp);
        lightningOff(sp);
        LAST_BLINK.remove(sp.getUUID());
    }
}
