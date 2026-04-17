package com.sigulog.minetrigger.core;

import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * 特殊スキル（パッシブ）の毎tick適用システム。
 *
 * メインハンド・オフハンドどちらかに WeaponItem を持っている場合、
 * 毎tick その武器の passiveEffect() を呼び出す。
 *
 * passiveEffect は持続時間2tickのポーション効果を付与し続けることで
 * 「常時バフ/デバフ」を実現する。武器を手放せば即座に効果が切れる。
 */
public final class PassiveEffectSystem {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                applyPassives(player);
            }
        });
    }

    private static void applyPassives(ServerPlayerEntity player) {
        if (player.getMainHandStack().getItem() instanceof WeaponItem w) {
            w.passiveEffect(player);
        }
        if (player.getOffHandStack().getItem() instanceof WeaponItem w) {
            w.passiveEffect(player);
        }
    }

    private PassiveEffectSystem() {}
}
