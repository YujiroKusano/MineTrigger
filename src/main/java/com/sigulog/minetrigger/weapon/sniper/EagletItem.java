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

/**
 * イーグレット — 高速射撃のスナイパートリガー。
 *
 * 通常発動: 速射（speed×2.0、基本ダメージ）
 * 特殊技  : チャージショット（speed×3.0、damage×2.5、1発）
 */
public class EagletItem extends ProjectileWeaponItem {

    public EagletItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed * 2.0, p.range, (float) p.damage)
                .gravity(0.001).build());
        player.sendMessage(Text.literal("§b[ イーグレット ]§r 速射"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed * 3.0, p.range, (float) (p.damage * 2.5))
                .gravity(0.001).build());
        player.sendMessage(Text.literal("§3[ イーグレット ]§r チャージショット"), true);
    }
}
