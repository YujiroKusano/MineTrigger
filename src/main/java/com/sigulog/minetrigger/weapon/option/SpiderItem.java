package com.sigulog.minetrigger.weapon.option;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.BulletManager;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * スパイダー — ワイヤーで敵の動きを封じるオプショントリガー。
 *
 * 通常発動: ワイヤー弾射出（Slowness付与）
 * 特殊技  : ワイヤー網展開（周囲の全エンティティにSlownessを付与）
 */
public class SpiderItem extends WeaponItem {

    public SpiderItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed, p.wireLength, 0f)
                .effect(StatusEffects.SLOWNESS, 60, 2)
                .gravity(0.003)
                .build());
        player.sendMessage(Text.literal("§8[ スパイダー ]§r ワイヤー射出"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        ServerWorld world = player.getServerWorld();
        Box box = new Box(player.getPos(), player.getPos()).expand(p.wireLength / 2);
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, e -> e != player)) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 2));
        }
        player.sendMessage(Text.literal("§7[ スパイダー ]§r ワイヤー網展開"), true);
    }
}
