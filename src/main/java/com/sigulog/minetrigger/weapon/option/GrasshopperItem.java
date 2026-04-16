package com.sigulog.minetrigger.weapon.option;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * グラスホッパー — 空中・地上の任意の方向へ跳躍するオプション。
 *
 * 通常発動: 上方向ジャンプブースト
 * 特殊技  : 視線方向へのダッシュ（水平移動）
 */
public class GrasshopperItem extends WeaponItem {

    /** 上方ジャンプ時の初速 */
    private static final double JUMP_BOOST = 1.5;
    /** ダッシュ時の水平初速 */
    private static final double DASH_BOOST = 1.8;

    public GrasshopperItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        // 現在の速度に上方向ベクトルを加算
        Vec3d vel = player.getVelocity();
        player.setVelocity(vel.x, vel.y + JUMP_BOOST, vel.z);
        player.velocityModified = true;
        player.sendMessage(Text.literal("§a[ グラスホッパー ]§r ジャンプブースト"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        // 視線方向（水平成分のみ）へ高速ダッシュ
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d horizontal = new Vec3d(look.x, 0, look.z).normalize().multiply(DASH_BOOST);
        Vec3d vel = player.getVelocity();
        player.setVelocity(vel.x + horizontal.x, vel.y + 0.3, vel.z + horizontal.z);
        player.velocityModified = true;
        player.sendMessage(Text.literal("§2[ グラスホッパー ]§r ダッシュ"), true);
    }
}
