package com.sigulog.minetrigger.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * トリオン弾のtickベースシミュレーター。
 * 全射撃系武器（アステロイド・ハンドガン・スナイパー等）はここを通して弾を飛ばす。
 */
public final class BulletManager {

    private static final List<Bullet> bullets = new ArrayList<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            synchronized (bullets) {
                Iterator<Bullet> iter = bullets.iterator();
                while (iter.hasNext()) {
                    if (iter.next().tick()) iter.remove();
                }
            }
        });
    }

    // ──────────────────────────────────────────────────────────────
    // 弾丸オプション
    // ──────────────────────────────────────────────────────────────

    public static final class BulletOptions {
        public final double speed;
        public final double range;
        public final float damage;
        public final double splashRadius;
        public final double homingStrength;
        public final RegistryEntry<StatusEffect> applyEffect;
        public final int effectDuration;
        public final int effectAmplifier;
        public final boolean shieldPenetrating;
        /** ブロック破壊半径（0 = 破壊なし）。ベドロックは破壊しない。 */
        public final double blockDestroyRadius;
        /**
         * 重力加速度（ブロック/tick²）。毎tickY速度から引かれる。
         * 0 = 完全直線。0.004 = 銃相当（ほぼ直線）、0.02 = グレネード相当（大きめの弧）。
         */
        public final double gravity;

        private BulletOptions(Builder b) {
            this.speed = b.speed;
            this.range = b.range;
            this.damage = b.damage;
            this.splashRadius = b.splashRadius;
            this.homingStrength = b.homingStrength;
            this.applyEffect = b.applyEffect;
            this.effectDuration = b.effectDuration;
            this.effectAmplifier = b.effectAmplifier;
            this.shieldPenetrating = b.shieldPenetrating;
            this.blockDestroyRadius = b.blockDestroyRadius;
            this.gravity = b.gravity;
        }

        public static BulletOptions basic(double speed, double range, float damage) {
            return new Builder(speed, range, damage).build();
        }

        public static Builder builder(double speed, double range, float damage) {
            return new Builder(speed, range, damage);
        }

        public static final class Builder {
            private final double speed, range;
            private final float damage;
            private double splashRadius = 0;
            private double homingStrength = 0;
            private RegistryEntry<StatusEffect> applyEffect = null;
            private int effectDuration = 0;
            private int effectAmplifier = 0;
            private boolean shieldPenetrating = false;
            private double blockDestroyRadius = 0;
            private double gravity = 0.0;

            Builder(double speed, double range, float damage) {
                this.speed = speed;
                this.range = range;
                this.damage = damage;
            }

            public Builder splash(double radius) {
                this.splashRadius = radius;
                return this;
            }

            public Builder homing(double strength) {
                this.homingStrength = strength;
                return this;
            }

            public Builder effect(RegistryEntry<StatusEffect> effect, int duration, int amplifier) {
                this.applyEffect = effect;
                this.effectDuration = duration;
                this.effectAmplifier = amplifier;
                return this;
            }

            public Builder shieldPenetrating() {
                this.shieldPenetrating = true;
                return this;
            }

            public Builder blockDestroy(double radius) {
                this.blockDestroyRadius = radius;
                return this;
            }

            public Builder gravity(double g) {
                this.gravity = g;
                return this;
            }

            public BulletOptions build() {
                return new BulletOptions(this);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 発射メソッド
    // ──────────────────────────────────────────────────────────────

    /** 後方互換用シンプル発射 */
    public static void fire(ServerPlayerEntity shooter, Vec3d start, Vec3d dir,
                            double speed, double range, float damage) {
        fire(shooter, start, dir, BulletOptions.basic(speed, range, damage));
    }

    public static void fire(ServerPlayerEntity shooter, Vec3d start, Vec3d dir,
                            BulletOptions opts) {
        Vec3d velocity = dir.normalize().multiply(opts.speed);
        synchronized (bullets) {
            bullets.add(new Bullet(shooter, start, velocity, opts));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 内部弾丸クラス
    // ──────────────────────────────────────────────────────────────

    private static class Bullet {
        final ServerPlayerEntity shooter;
        Vec3d pos;
        Vec3d velocity;  // ホーミング時に変更される
        double remainingRange;
        final BulletOptions opts;

        Bullet(ServerPlayerEntity shooter, Vec3d pos, Vec3d vel, BulletOptions opts) {
            this.shooter = shooter;
            this.pos = pos;
            this.velocity = vel;
            this.remainingRange = opts.range;
            this.opts = opts;
        }

        boolean tick() {
            if (shooter.isRemoved()) return true;

            ServerWorld world = shooter.getServerWorld();
            Vec3d from = pos;

            // ── ホーミング処理 ────────────────────────────────────
            if (opts.homingStrength > 0) {
                Box searchBox = new Box(pos, pos).expand(opts.range);
                LivingEntity nearest = world.getEntitiesByClass(LivingEntity.class, searchBox,
                    e -> e != shooter && !e.isSpectator())
                    .stream()
                    .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(pos.x, pos.y, pos.z)))
                    .orElse(null);
                if (nearest != null) {
                    Vec3d toTarget = nearest.getPos()
                        .add(0, nearest.getHeight() / 2.0, 0)
                        .subtract(pos)
                        .normalize();
                    double speed = velocity.length();
                    Vec3d curDir = velocity.normalize();
                    double h = opts.homingStrength;
                    Vec3d blended = new Vec3d(
                        curDir.x + (toTarget.x - curDir.x) * h,
                        curDir.y + (toTarget.y - curDir.y) * h,
                        curDir.z + (toTarget.z - curDir.z) * h
                    ).normalize();
                    velocity = blended.multiply(speed);
                }
            }

            // ── 重力適用 ──────────────────────────────────────────
            if (opts.gravity > 0) {
                velocity = velocity.add(0, -opts.gravity, 0);
            }

            Vec3d to = pos.add(velocity);
            double step = velocity.length();

            // ── ブロック衝突チェック ──────────────────────────────
            BlockHitResult blockHit = world.raycast(new RaycastContext(
                from, to,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                shooter
            ));
            if (blockHit.getType() != HitResult.Type.MISS) {
                Vec3d hitPos = blockHit.getPos();
                if (opts.blockDestroyRadius > 0) {
                    destroyBlocks(world, hitPos, opts.blockDestroyRadius);
                }
                if (opts.splashRadius > 0) {
                    doSplash(world, hitPos);
                }
                return true;
            }

            // ── エンティティ衝突チェック ──────────────────────────
            Box path = new Box(from, to).expand(0.3);
            List<LivingEntity> hits = world.getEntitiesByClass(
                LivingEntity.class, path,
                e -> e != shooter && !e.isSpectator()
            );

            if (!hits.isEmpty()) {
                LivingEntity target = hits.stream()
                    .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(from.x, from.y, from.z)))
                    .orElse(null);
                if (target != null) {
                    if (opts.splashRadius > 0) {
                        doSplash(world, target.getPos());
                    } else {
                        applyHit(world, target);
                    }
                    return true;
                }
            }

            // パーティクルで弾丸を可視化（クライアントに送信）
            world.spawnParticles(ParticleTypes.CRIT,
                to.x, to.y, to.z, 1, 0.0, 0.0, 0.0, 0.0);

            pos = to;
            remainingRange -= step;
            return remainingRange <= 0;
        }

        /** 単体ヒット時の処理 */
        private void applyHit(ServerWorld world, LivingEntity target) {
            if (opts.damage > 0) {
                float effective = computeEffectiveDamage(target);
                target.damage(world.getDamageSources().playerAttack(shooter), effective);
            }
            if (opts.applyEffect != null) {
                target.addStatusEffect(new StatusEffectInstance(
                    opts.applyEffect, opts.effectDuration, opts.effectAmplifier));
            }
        }

        /** スプラッシュ（範囲攻撃）処理 */
        private void doSplash(ServerWorld world, Vec3d center) {
            double r = opts.splashRadius;
            Box box = new Box(center, center).expand(r);
            world.getEntitiesByClass(LivingEntity.class, box,
                e -> e != shooter && e.squaredDistanceTo(center.x, center.y, center.z) <= r * r)
                .forEach(e -> applyHit(world, e));
            // 爆発パーティクル
            world.spawnParticles(ParticleTypes.EXPLOSION,
                center.x, center.y, center.z, 5, r * 0.3, r * 0.3, r * 0.3, 0.1);
        }

        /** 球形範囲内のブロックを破壊する（ベドロックと空気は除外、ドロップなし） */
        private void destroyBlocks(ServerWorld world, Vec3d center, double radius) {
            int r = (int) Math.ceil(radius);
            BlockPos centerPos = BlockPos.ofFloored(center);
            double r2 = radius * radius;
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (dx * dx + dy * dy + dz * dz > r2) continue;
                        BlockPos pos = centerPos.add(dx, dy, dz);
                        var state = world.getBlockState(pos);
                        if (state.isAir()) continue;
                        // ベドロック（hardness < 0）は破壊しない
                        if (state.getHardness(world, pos) < 0) continue;
                        world.setBlockState(pos, Blocks.AIR.getDefaultState());
                    }
                }
            }
        }

        /** シールド軽減を考慮した有効ダメージを計算する */
        private float computeEffectiveDamage(LivingEntity target) {
            if (opts.shieldPenetrating) return opts.damage;
            if (target instanceof ServerPlayerEntity targetPlayer) {
                float reduction = (float) ShieldManager.getReduction(targetPlayer);
                return opts.damage * (1.0f - reduction);
            }
            return opts.damage;
        }
    }

    private BulletManager() {}
}
