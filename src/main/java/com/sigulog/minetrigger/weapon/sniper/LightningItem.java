package com.sigulog.minetrigger.weapon.sniper;

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
 * ライトニング — 超高速弾を放つスナイパートリガー。
 *
 * 通常発動: 超高速弾（speed×4.0、基本ダメージ）
 * 特殊技  : 連続高速弾（speed×4.0、spread=0.01 の5連続弾で貫通を代替）
 */
public class LightningItem extends ProjectileWeaponItem {

    private static final double SPREAD = 0.01;
    private static final Random RANDOM = new Random();

    public LightningItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed * 4.0, p.range, (float) p.damage)
                .gravity(0.001).build());
        player.sendMessage(Text.literal("§e[ ライトニング ]§r 高速弾"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look   = player.getRotationVec(1.0f).normalize();
        Vec3d eyePos = player.getEyePos();
        double burstSpeed = p.speed * 4.0;

        // 貫通代替: spread=0.01 の5連続高速弾
        for (int i = 0; i < 5; i++) {
            double dx = look.x + (RANDOM.nextDouble() - 0.5) * 2 * SPREAD;
            double dy = look.y + (RANDOM.nextDouble() - 0.5) * 2 * SPREAD;
            double dz = look.z + (RANDOM.nextDouble() - 0.5) * 2 * SPREAD;
            Vec3d dir   = new Vec3d(dx, dy, dz).normalize();
            Vec3d start = eyePos.add(dir.multiply(0.5));
            BulletManager.fire(player, start, dir,
                BulletManager.BulletOptions.builder(burstSpeed, p.range, (float) p.damage)
                    .gravity(0.001).build());
        }
        player.sendMessage(Text.literal("§6[ ライトニング ]§r 連続高速弾"), true);
    }
}
