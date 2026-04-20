package com.sigulog.minetrigger.weapon.ranged;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.BulletManager;
import com.sigulog.minetrigger.core.GunnerAmmoManager;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.ProjectileWeaponItem;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/**
 * ガトリング — 高速連射の重火器トリガー。
 *
 * 通常発動: 基本射撃1発
 * 特殊技  : 8連射（速度1.5倍、ダメージ0.5倍×8発、spread=0.04）
 */
public class GatlingItem extends ProjectileWeaponItem {

    private static final double SPREAD = 0.04;
    private static final Random RANDOM = new Random();

    public GatlingItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    /** 特殊スキル: 重火器 — 装備中に移動速度が大幅低下（Slowness II。機関砲の重さを表現） */
    @Override
    public void passiveEffect(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(
            StatusEffects.SLOWNESS, 2, 1, false, false, true));
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        if (GunnerAmmoManager.fireAndCycle(player, hand, weaponType) != null) return;

        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                .gravity(0.004).build());
        player.sendMessage(Text.literal("§6[ ガトリング ]§r 発射"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look   = player.getRotationVec(1.0f).normalize();
        Vec3d eyePos = player.getEyePos();
        float burstDamage = (float) (p.damage * 0.5);
        double burstSpeed = p.speed * 1.5;

        for (int i = 0; i < 8; i++) {
            double dx = look.x + (RANDOM.nextDouble() - 0.5) * 2 * SPREAD;
            double dy = look.y + (RANDOM.nextDouble() - 0.5) * 2 * SPREAD;
            double dz = look.z + (RANDOM.nextDouble() - 0.5) * 2 * SPREAD;
            Vec3d dir   = new Vec3d(dx, dy, dz).normalize();
            Vec3d start = eyePos.add(dir.multiply(0.5));
            BulletManager.fire(player, start, dir,
                BulletManager.BulletOptions.builder(burstSpeed, p.range, burstDamage)
                    .gravity(0.004).build());
        }
        player.sendMessage(Text.literal("§c[ ガトリング ]§r 高速連射×8"), true);
    }
}
