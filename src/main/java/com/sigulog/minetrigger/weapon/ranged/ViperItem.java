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
 * バイパー — 軌道を制御できる変則弾トリガー。
 *
 * 通常発動: 基本射撃1発
 * 特殊技  : ジグザグ弾（3発を左右±0.2にずらして発射）
 */
public class ViperItem extends ProjectileWeaponItem {

    public ViperItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look, p.speed, p.range, (float) p.damage);
        player.sendMessage(Text.literal("§5[ バイパー ]§r 発射"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look   = player.getRotationVec(1.0f).normalize();
        Vec3d eyePos = player.getEyePos();

        // 水平左右オフセット ±0.2 の3発
        double[] horizontalOffsets = { -0.2, 0.0, 0.2 };
        for (double off : horizontalOffsets) {
            Vec3d dir   = new Vec3d(look.x + off, look.y, look.z).normalize();
            Vec3d start = eyePos.add(dir.multiply(0.5));
            BulletManager.fire(player, start, dir,
                BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage).build());
        }
        player.sendMessage(Text.literal("§d[ バイパー ]§r ジグザグ弾"), true);
    }
}
