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
 * ハンドガン — 基本的な射撃トリガー。
 *
 * 通常発動: 基本射撃1発
 * 特殊技  : 3連射（速度1.2倍、ダメージ0.7倍×3発）
 */
public class HandgunItem extends ProjectileWeaponItem {

    public HandgunItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look, p.speed, p.range, (float) p.damage);
        player.sendMessage(Text.literal("§e[ ハンドガン ]§r 発射"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look   = player.getRotationVec(1.0f).normalize();
        Vec3d eyePos = player.getEyePos();
        float burstDamage = (float) (p.damage * 0.7);
        double burstSpeed = p.speed * 1.2;

        for (int i = 0; i < 3; i++) {
            Vec3d start = eyePos.add(look.multiply(0.5));
            BulletManager.fire(player, start, look,
                BulletManager.BulletOptions.builder(burstSpeed, p.range, burstDamage).build());
        }
        player.sendMessage(Text.literal("§6[ ハンドガン ]§r 3連射"), true);
    }
}
