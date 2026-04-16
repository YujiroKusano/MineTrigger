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
 * ハウンド — 目標を追跡するホーミング弾。
 *
 * 通常発動: ホーミング弾1発
 * 特殊技  : ホーミング弾3連射（扇状に展開）
 */
public class HoundItem extends ProjectileWeaponItem {

    public HoundItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                .homing(p.trackingSpeed)
                .build());
        player.sendMessage(Text.literal("§b[ ハウンド ]§r ホーミング弾発射"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d eyePos = player.getEyePos();

        // 3方向に扇状展開（上下 ±10°）
        double[] offsets = { -0.17, 0.0, 0.17 }; // sin(±10°) ≈ 0.17
        for (double off : offsets) {
            Vec3d dir = new Vec3d(look.x, look.y + off, look.z).normalize();
            Vec3d start = eyePos.add(dir.multiply(0.5));
            BulletManager.fire(player, start, dir,
                BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                    .homing(p.trackingSpeed)
                    .build());
        }
        player.sendMessage(Text.literal("§3[ ハウンド ]§r 3連ホーミング弾発射"), true);
    }
}
