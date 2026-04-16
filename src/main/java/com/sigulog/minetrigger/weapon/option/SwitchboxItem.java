package com.sigulog.minetrigger.weapon.option;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.BulletManager;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * スイッチボックス — 爆発物設置と一斉起爆を行うオプショントリガー。
 *
 * 通常発動: 爆発弾設置（低速・高スプラッシュ弾を前方へ発射）
 * 特殊技  : 一斉起爆（周囲の全エンティティに即時ダメージ+ノックバック）
 */
public class SwitchboxItem extends WeaponItem {

    public SwitchboxItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));
        BulletManager.fire(player, start, look,
            BulletManager.BulletOptions.builder(p.speed * 0.3, p.range, (float) p.damage)
                .splash(p.splashRadius)
                .build());
        player.sendMessage(Text.literal("§c[ スイッチボックス ]§r 爆発弾設置"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        ServerWorld world = player.getServerWorld();
        Box box = new Box(player.getPos(), player.getPos()).expand(p.range * 2);
        for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, box, e -> e != player)) {
            e.damage(world.getDamageSources().explosion(null), (float) p.damage * 2);
            Vec3d knockback = e.getPos().subtract(player.getPos()).normalize().multiply(2.0);
            e.addVelocity(knockback.x, 0.5, knockback.z);
        }
        player.sendMessage(Text.literal("§4[ スイッチボックス ]§r 一斉起爆!"), true);
    }
}
