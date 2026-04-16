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
 * アイビス — 超高威力の対物スナイパートリガー。
 *
 * 通常発動: 超高威力弾（damage×3.0、speed×1.5）
 * 特殊技  : 爆破弾（damage×2.0、speed×1.5、着弾時爆発）
 */
public class IbisItem extends ProjectileWeaponItem {

    public IbisItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed * 1.5, p.range, (float) (p.damage * 3.0)).build());
        player.sendMessage(Text.literal("§c[ アイビス ]§r 高威力弾"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed * 1.5, p.range, (float) (p.damage * 2.0))
                .splash(p.splashRadius)
                .build());
        player.sendMessage(Text.literal("§4[ アイビス ]§r 爆破弾"), true);
    }
}
