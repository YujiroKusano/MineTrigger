package com.sigulog.minetrigger.weapon.attacker;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * 弧月 — 近接斬撃。前方の扇形範囲内の敵にダメージ。
 *
 * 通常発動: 通常斬撃（射程 range）
 * 特殊技  : 旋空（射程 range × 2 に延長）
 */
public class KogetsuItem extends WeaponItem {

    public KogetsuItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        slash(player, p.range, (float) p.damage);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        // 旋空: 射程を2倍に延長した斬撃
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        slash(player, p.range * 2.0, (float) p.damage);
    }

    /** 特殊スキル: 抜刀強化 — 装備中に移動速度が上がる（Speed I） */
    @Override
    public void passiveEffect(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(
            StatusEffects.SPEED, 2, 0, false, false, true));
    }

    protected void slash(ServerPlayerEntity player, double range, float damage) {
        ServerWorld world = player.getServerWorld();
        for (LivingEntity target : getTargetsInFront(player, range)) {
            target.damage(
                world.getDamageSources().playerAttack(player),
                damage
            );
        }
    }

    private static List<LivingEntity> getTargetsInFront(ServerPlayerEntity player, double range) {
        Vec3d look   = player.getRotationVec(1.0f);
        Vec3d eyePos = player.getEyePos();
        Box box = new Box(eyePos, eyePos.add(look.multiply(range))).expand(1.2, 0.8, 1.2);

        return player.getServerWorld().getEntitiesByClass(
            LivingEntity.class, box,
            e -> e != player && isFacing(player, e, range)
        );
    }

    private static boolean isFacing(ServerPlayerEntity player, LivingEntity target, double range) {
        Vec3d look     = player.getRotationVec(1.0f).normalize();
        Vec3d toTarget = target.getPos()
            .add(0, target.getHeight() / 2.0, 0)
            .subtract(player.getEyePos())
            .normalize();
        return toTarget.dotProduct(look) > 0.2 && target.distanceTo(player) <= range;
    }
}
