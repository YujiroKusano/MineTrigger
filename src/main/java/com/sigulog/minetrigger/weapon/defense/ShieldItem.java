package com.sigulog.minetrigger.weapon.defense;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.CooldownManager;
import com.sigulog.minetrigger.core.ShieldManager;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

/**
 * シールド — バリアを展開してダメージを軽減する。
 *
 * 通常発動: バリア展開（durationTicks 間ダメージ軽減）
 * 特殊技  : 集中（軽減率 × 1.5 倍、持続時間 半分）
 */
public class ShieldItem extends WeaponItem {

    private static final int DEFAULT_DURATION = 60; // 3秒

    public ShieldItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    /** シールドはトリオン消費なし。クールダウンのみ管理する。 */
    @Override
    public boolean tryActivate(ServerPlayerEntity player, Hand hand) {
        if (CooldownManager.isOnCooldown(player, weaponType.configKey)) {
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.5f);
            return false;
        }
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        if (player.isSneaking()) {
            activateSpecial(player, hand);
        } else {
            activateNormal(player, hand);
        }
        CooldownManager.startCooldown(player, weaponType.configKey, p.cooldownTicks);
        return true;
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        ShieldManager.activate(player, p.damageReduction, DEFAULT_DURATION);
        player.sendMessage(Text.literal("§9[ シールド ]§r バリア展開"), true);
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        // 集中: 軽減率1.5倍、持続時間半分
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        double highReduction = Math.min(p.damageReduction * 1.5, 0.9);
        ShieldManager.activate(player, highReduction, DEFAULT_DURATION / 2);
        player.sendMessage(Text.literal("§b[ シールド ]§r 高耐久バリア展開"), true);
    }
}
