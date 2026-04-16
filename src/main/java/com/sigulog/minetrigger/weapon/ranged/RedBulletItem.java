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
 * レッドバレット — 命中した相手を重度スロウネスにするトリオン弾。
 * ダメージなし・シールド貫通・スロウネス付与。
 *
 * 通常発動: スロウネス弾（低速、シールド貫通）
 * 特殊技  : 広範囲スプラッシュ版
 */
public class RedBulletItem extends ProjectileWeaponItem {

    public RedBulletItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        // ダメージ0、スロウネス付与、シールド貫通
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed, p.range, 0f)
                .effect(StatusEffects.SLOWNESS, p.slownessDurationTicks, p.slownessLevel - 1)
                .shieldPenetrating()
                .build());
        player.sendMessage(Text.literal("§4[ レッドバレット ]§r スロウネス弾発射"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        // 着弾点中心に範囲スロウネス
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed, p.range, 0f)
                .splash(3.0)
                .effect(StatusEffects.SLOWNESS, p.slownessDurationTicks, p.slownessLevel - 1)
                .shieldPenetrating()
                .build());
        player.sendMessage(Text.literal("§c[ レッドバレット ]§r 範囲スロウネス弾発射"), true);
    }
}
