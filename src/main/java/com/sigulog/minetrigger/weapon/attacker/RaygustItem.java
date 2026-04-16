package com.sigulog.minetrigger.weapon.attacker;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.ShieldManager;
import com.sigulog.minetrigger.weapon.WeaponType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * レイガスト — 剣モードと盾モードを切り替えられるブレード。
 *
 * 通常発動:
 *   剣モード → 斬撃（KogetsuItem#slash を流用）
 *   盾モード → バリア展開
 *
 * 特殊技（シフト＋クリック）: モード切り替え（剣⇔盾）
 */
public class RaygustItem extends KogetsuItem {

    /** 盾モード中のプレイヤーUUID */
    private static final Set<UUID> shieldMode = new HashSet<>();

    private static final int SHIELD_DURATION = 80; // 4秒

    public RaygustItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        if (shieldMode.contains(player.getUuid())) {
            // 盾モード: バリア展開
            WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
            ShieldManager.activate(player, p.damageReduction, SHIELD_DURATION);
            player.sendMessage(Text.literal("§9[ レイガスト ]§r バリア展開"), true);
        } else {
            // 剣モード: 斬撃
            WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
            slash(player, p.range, (float) p.damage);
        }
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        // モード切り替え
        if (shieldMode.contains(player.getUuid())) {
            shieldMode.remove(player.getUuid());
            ShieldManager.deactivate(player);
            player.sendMessage(Text.literal("§e[ レイガスト ]§r 剣モード"), true);
        } else {
            shieldMode.add(player.getUuid());
            player.sendMessage(Text.literal("§9[ レイガスト ]§r 盾モード"), true);
        }
    }
}
