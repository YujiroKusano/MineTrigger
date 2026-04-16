package com.sigulog.minetrigger.weapon.ranged;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.BulletManager;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.ProjectileWeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * メテオラ — 着弾時に爆発するトリオン弾。
 *
 * 通常発動: 爆発弾（着弾時に範囲ダメージ）
 * 特殊技  : 高速爆発弾（速度2倍、爆発範囲1.5倍）
 */
public class MeteoraItem extends ProjectileWeaponItem {

    public MeteoraItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                .splash(p.splashRadius)
                .build());
        player.sendMessage(Text.literal("§c[ メテオラ ]§r 爆発弾発射"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed * 2.0, p.range, (float) p.damage)
                .splash(p.splashRadius * 1.5)
                .build());
        player.sendMessage(Text.literal("§4[ メテオラ ]§r 高速爆発弾発射"), true);
    }
}
