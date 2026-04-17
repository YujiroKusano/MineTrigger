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

import java.util.Random;

/**
 * アサルトライフル — フルオートの連続射撃トリガー。
 *
 * 通常発動: 基本射撃1発
 * 特殊技  : 5連射（速度1.3倍、ダメージ0.6倍×5発、わずかなランダム拡散）
 */
public class AssaultRifleItem extends ProjectileWeaponItem {

    private static final double SPREAD = 0.02;
    private static final Random RANDOM = new Random();

    public AssaultRifleItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                .gravity(0.004).build());
        player.sendMessage(Text.literal("§a[ アサルトライフル ]§r 発射"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look   = player.getRotationVec(1.0f).normalize();
        Vec3d eyePos = player.getEyePos();
        float burstDamage = (float) (p.damage * 0.6);
        double burstSpeed = p.speed * 1.3;

        for (int i = 0; i < 5; i++) {
            double dx = look.x + (RANDOM.nextDouble() - 0.5) * 2 * SPREAD;
            double dy = look.y + (RANDOM.nextDouble() - 0.5) * 2 * SPREAD;
            double dz = look.z + (RANDOM.nextDouble() - 0.5) * 2 * SPREAD;
            Vec3d dir   = new Vec3d(dx, dy, dz).normalize();
            Vec3d start = eyePos.add(dir.multiply(0.5));
            BulletManager.fire(player, start, dir,
                BulletManager.BulletOptions.builder(burstSpeed, p.range, burstDamage)
                    .gravity(0.004).build());
        }
        player.sendMessage(Text.literal("§2[ アサルトライフル ]§r フルバースト"), true);
    }
}
