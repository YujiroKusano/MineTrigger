package com.sigulog.minetrigger.weapon.ranged;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.BulletManager;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.ProjectileWeaponItem;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * グレネードガン — 爆発とスロウネスを組み合わせた制圧武器。
 *
 * 通常発動: 低速爆発弾（着弾範囲ダメージ＋スロウネス）
 * 特殊技  : 直撃弾（高速・爆発範囲拡大）
 */
public class GrenadeGunItem extends ProjectileWeaponItem {

    public GrenadeGunItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        // 低速・爆発+スロウネス
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed * 0.6, p.range, (float) p.damage)
                .splash(p.splashRadius)
                .blockDestroy(p.splashRadius)
                .effect(StatusEffects.SLOWNESS, p.slownessDurationTicks, p.slownessLevel - 1)
                .build());
        player.sendMessage(Text.literal("§2[ グレネードガン ]§r 爆発弾発射"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        // 高速・爆発範囲拡大
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed * 1.5, p.range, (float) (p.damage * 1.2))
                .splash(p.splashRadius * 1.5)
                .blockDestroy(p.splashRadius * 1.5)
                .effect(StatusEffects.SLOWNESS, p.slownessDurationTicks / 2, p.slownessLevel - 1)
                .build());
        player.sendMessage(Text.literal("§a[ グレネードガン ]§r 高速爆発弾発射"), true);
    }
}
