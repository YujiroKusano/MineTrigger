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
 * バイパー — 極低速で操舵可能なトリオン弾。
 *
 * 通常発動: 低速弾1発。長押し中は向いた方向へ曲がる。
 * 特殊技  : 3発同時発射（それぞれ操舵可）
 */
public class ViperItem extends ProjectileWeaponItem {

    /** バイパー弾の速度（通常弾の約1/8） */
    private static final double VIPER_SPEED = 0.12;

    public ViperItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(VIPER_SPEED, p.range, (float) p.damage)
                .viperSteering()
                .build());
        player.sendMessage(Text.literal("§5[ バイパー ]§r 操舵弾発射"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look   = player.getRotationVec(1.0f).normalize();
        Vec3d eyePos = player.getEyePos();

        // 縦方向に3発展開
        double[] offsets = { -0.08, 0.0, 0.08 };
        for (double off : offsets) {
            Vec3d dir   = new Vec3d(look.x, look.y + off, look.z).normalize();
            Vec3d start = eyePos.add(dir.multiply(0.5));
            BulletManager.fire(player, start, dir,
                BulletManager.BulletOptions.builder(VIPER_SPEED, p.range, (float) p.damage)
                    .viperSteering()
                    .build());
        }
        player.sendMessage(Text.literal("§d[ バイパー ]§r 3連操舵弾発射"), true);
    }
}
