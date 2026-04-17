package com.sigulog.minetrigger.weapon.attacker;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.weapon.WeaponType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * スコーピオン — 変形ブレード。
 *
 * 通常発動: 前方斬撃（弧月より短射程・低ダメージだが高速）
 * 特殊技  : 全方位斬撃（周囲360°の全敵を同時攻撃）
 */
public class ScorpionItem extends KogetsuItem {

    public ScorpionItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        slash(player, p.range, (float) p.damage);
        player.sendMessage(Text.literal("§e[ スコーピオン ]§r 斬撃"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        slashAllAround(player, p.range, (float) p.damage);
        player.sendMessage(Text.literal("§6[ スコーピオン ]§r 全方位斬撃"), true);
    }

    /** 特殊スキル: 衝撃吸収 — 装備中に落下ダメージが発生しない（Slow Falling） */
    @Override
    public void passiveEffect(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(
            StatusEffects.SLOW_FALLING, 2, 0, false, false, true));
    }

    /** 周囲360°全員を攻撃する */
    private void slashAllAround(ServerPlayerEntity player, double range, float damage) {
        ServerWorld world = player.getServerWorld();
        Vec3d center = player.getPos().add(0, player.getHeight() / 2.0, 0);
        Box box = new Box(center, center).expand(range);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, box,
            e -> e != player && !e.isSpectator() && e.distanceTo(player) <= range);
        for (LivingEntity target : targets) {
            target.damage(world.getDamageSources().playerAttack(player), damage);
        }
    }
}
