package com.sigulog.minetrigger.weapon.defense;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.ShieldManager;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * エスクード — 障壁展開とカタパルト射出の防御系トリガー。
 *
 * 通常発動: 障壁シールド展開（ShieldManager で自身に防御付与）
 * 特殊技  : カタパルト（プレイヤーを上方向＋前方に射出）
 */
public class EscudoItem extends WeaponItem {

    public EscudoItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        ShieldManager.activate(player, p.damageReduction, 100);
        player.sendMessage(Text.literal("§9[ エスクード ]§r 障壁展開"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f);
        Vec3d boost = new Vec3d(look.x * 0.5, p.catapultVelocity, look.z * 0.5);
        player.setVelocity(boost);
        player.velocityModified = true;
        player.sendMessage(Text.literal("§b[ エスクード ]§r カタパルト発射"), true);
    }
}
