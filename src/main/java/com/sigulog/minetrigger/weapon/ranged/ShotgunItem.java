package com.sigulog.minetrigger.weapon.ranged;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.BulletManager;
import com.sigulog.minetrigger.core.GunnerAmmoManager;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.ProjectileWeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * ショットガン — 複数の散弾を一斉発射するガンナー武器。
 *
 * 通常発動: 散弾（pelletCount 発を扇状に発射）
 * 特殊技  : 集中射撃（散弾を絞って単発高ダメージ相当、近距離ボーナス付き）
 */
public class ShotgunItem extends ProjectileWeaponItem {

    private static final Random RNG = new Random();

    public ShotgunItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        if (GunnerAmmoManager.fireAndCycle(player, hand, weaponType) != null) return;

        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look   = player.getRotationVec(1.0f).normalize();
        Vec3d eyePos = player.getEyePos();

        // 散弾を扇状にランダム拡散
        for (int i = 0; i < p.pelletCount; i++) {
            Vec3d spread = randomSpread(look, 0.1);
            Vec3d start  = eyePos.add(spread.multiply(0.5));
            BulletManager.fire(player, start, spread,
                BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                    .gravity(0.004).build());
        }
        player.sendMessage(Text.literal("§6[ ショットガン ]§r 散弾発射 ×" + p.pelletCount), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        // 集中射撃：散弾を絞って発射（近距離高ダメージ）
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look   = player.getRotationVec(1.0f).normalize();
        Vec3d eyePos = player.getEyePos();

        float bonusDamage = (float) (p.damage * p.closeRangeBonus);
        for (int i = 0; i < p.pelletCount; i++) {
            Vec3d spread = randomSpread(look, 0.03); // 拡散を小さく
            Vec3d start  = eyePos.add(spread.multiply(0.5));
            BulletManager.fire(player, start, spread,
                BulletManager.BulletOptions.builder(p.speed * 1.2, p.closeRangeThreshold, bonusDamage)
                    .gravity(0.004).build());
        }
        player.sendMessage(Text.literal("§e[ ショットガン ]§r 近距離集中射撃"), true);
    }

    /** 指定方向に対して random な拡散を加えた方向ベクトルを返す */
    private static Vec3d randomSpread(Vec3d base, double spread) {
        return new Vec3d(
            base.x + (RNG.nextDouble() - 0.5) * spread * 2,
            base.y + (RNG.nextDouble() - 0.5) * spread * 2,
            base.z + (RNG.nextDouble() - 0.5) * spread * 2
        ).normalize();
    }
}
