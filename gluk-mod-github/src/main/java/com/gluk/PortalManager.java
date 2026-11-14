package com.gluk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** H: ставит портал 2x3 (блоки как в Незере, без обсидиана). Вход — мгновенный перенос. */
public final class PortalManager {
    public static final int LIFETIME_TICKS = 20 * 20;
    private static final Map<UUID, Long> COOLDOWN = new ConcurrentHashMap<>();

    private PortalManager() {}

    public static void open(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        if (level.dimension() != Level.OVERWORLD && level.dimension() != Level.NETHER) {
            sp.displayClientMessage(Component.translatable("message.gluk.portal_bad_dim"), true);
            return;
        }
        Direction facing = sp.getDirection();
        Direction right = facing.getClockWise();
        Direction.Axis axis = facing.getAxis() == Direction.Axis.Z ? Direction.Axis.X : Direction.Axis.Z;
        BlockPos base = sp.blockPosition().relative(facing, 3);

        int placed = place(level, base, right, axis);
        if (placed < 4) placed += place(level, base.above(), right, axis);
        if (placed == 0) {
            sp.displayClientMessage(Component.translatable("message.gluk.blink_fail"), true);
            return;
        }
        preload(level, sp);
        level.playSound(null, base, SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.6f, 1.2f);
        sp.displayClientMessage(Component.translatable("message.gluk.portal_opened"), true);
    }

    private static int place(ServerLevel level, BlockPos base, Direction right, Direction.Axis axis) {
        int n = 0;
        for (int c = -1; c <= 0; c++) {
            for (int dy = 0; dy < 3; dy++) {
                BlockPos pos = base.relative(right, c).above(dy);
                BlockState st = level.getBlockState(pos);
                if ((st.isAir() || st.canBeReplaced()) && st.getFluidState().isEmpty()) {
                    level.setBlock(pos, GlukMod.GLUK_PORTAL.get().defaultBlockState()
                            .setValue(GlukPortalBlock.AXIS, axis), 2);
                    level.scheduleTick(pos, GlukMod.GLUK_PORTAL.get(), LIFETIME_TICKS);
                    n++;
                }
            }
        }
        return n;
    }

    /** Заранее грузим/генерируем чанки на той стороне, чтобы переход был быстрым. */
    private static void preload(ServerLevel level, ServerPlayer sp) {
        MinecraftServer server = sp.getServer();
        if (server == null) return;
        boolean toNether = level.dimension() == Level.OVERWORLD;
        ServerLevel target = server.getLevel(toNether ? Level.NETHER : Level.OVERWORLD);
        if (target == null) return;
        double scale = toNether ? 1.0 / 8.0 : 8.0;
        int cx = Mth.floor(sp.getX() * scale) >> 4;
        int cz = Mth.floor(sp.getZ() * scale) >> 4;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) target.getChunk(cx + dx, cz + dz);
    }

    /** Мгновенный перенос Верхний мир <-> Незер. */
    public static void teleport(ServerPlayer pl) {
        MinecraftServer server = pl.getServer();
        if (server == null) return;
        ServerLevel from = pl.serverLevel();
        long now = from.getGameTime();
        Long last = COOLDOWN.get(pl.getUUID());
        if (last != null && now - last < 40) return;
        boolean toNether = from.dimension() == Level.OVERWORLD;
        if (!toNether && from.dimension() != Level.NETHER) return;
        ServerLevel target = server.getLevel(toNether ? Level.NETHER : Level.OVERWORLD);
        if (target == null) return;
        COOLDOWN.put(pl.getUUID(), now);

        double scale = toNether ? 1.0 / 8.0 : 8.0;
        int cx = Mth.floor(pl.getX() * scale);
        int cz = Mth.floor(pl.getZ() * scale);
        target.getChunk(cx >> 4, cz >> 4);
        Vec3 dest = toNether ? findNetherSpot(target, cx, cz) : findOverworldSpot(target, cx, cz);

        from.playSound(null, pl.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 1f);
        pl.teleportTo(target, dest.x, dest.y, dest.z, pl.getYRot(), pl.getXRot());
        pl.resetFallDistance();
        target.playSound(null, BlockPos.containing(dest), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 1f);
    }

    private static Vec3 findOverworldSpot(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        return new Vec3(x + 0.5, y, z + 0.5);
    }

    private static Vec3 findNetherSpot(ServerLevel level, int x, int z) {
        for (int r = 0; r <= 24; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int px = x + dx, pz = z + dz;
                    level.getChunk(px >> 4, pz >> 4);
                    for (int y = 100; y >= 33; y--) {
                        if (isSafe(level, new BlockPos(px, y, pz))) return new Vec3(px + 0.5, y, pz + 0.5);
                    }
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            level.setBlock(new BlockPos(x + dx, 69, z + dz), Blocks.NETHERRACK.defaultBlockState(), 3);
            for (int dy = 70; dy <= 72; dy++) level.setBlock(new BlockPos(x + dx, dy, z + dz), Blocks.AIR.defaultBlockState(), 3);
        }
        return new Vec3(x + 0.5, 70, z + 0.5);
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)) return false;
        if (level.getBlockState(below).is(Blocks.MAGMA_BLOCK)) return false;
        if (!level.getFluidState(below).isEmpty()) return false;
        for (int i = 0; i < 2; i++) {
            BlockPos p = pos.above(i);
            if (!level.getFluidState(p).isEmpty()) return false;
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) return false;
        }
        return true;
    }
}
