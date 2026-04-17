package com.sigulog.minetrigger.weapon.base;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.BulletManager;
import com.sigulog.minetrigger.weapon.WeaponType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * 射撃系武器の基底クラス（アステロイド・ハンドガン・スナイパー系など）。
 *
 * 通常発動: 弾丸を前方に発射（BulletManager へ登録）
 * 特殊技  : サブクラスでオーバーライド（デフォルトは何もしない）
 */
public class ProjectileWeaponItem extends WeaponItem {

    public ProjectileWeaponItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        // 目線から少し前方を発射起点にして自爆を防ぐ
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                .gravity(0.004).build());
    }
}
