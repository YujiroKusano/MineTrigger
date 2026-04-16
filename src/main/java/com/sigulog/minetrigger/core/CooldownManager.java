package com.sigulog.minetrigger.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 武器ごとのクールダウンをtick単位で管理するクラス。
 *
 * データ構造: playerUUID → (weaponConfigKey → 残りtick数)
 * クールダウン中は WeaponItem.use() から発動不可を返す。
 */
public final class CooldownManager {

    // スレッドセーフなマップ（サーバーティックスレッドからのみアクセスするが念のため）
    private static final Map<UUID, Map<String, Integer>> COOLDOWNS = new ConcurrentHashMap<>();

    public static void register() {
        // 毎ティック全プレイヤーのクールダウンをデクリメント
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Map<String, Integer> playerCd : COOLDOWNS.values()) {
                playerCd.replaceAll((key, ticks) -> Math.max(0, ticks - 1));
                playerCd.entrySet().removeIf(e -> e.getValue() <= 0);
            }
        });

        // プレイヤー切断時にクリーンアップ
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            COOLDOWNS.remove(handler.player.getUuid());
        });
    }

    /**
     * 指定武器のクールダウンを開始する。
     * @param player 対象プレイヤー
     * @param weaponKey config.yml の武器キー
     * @param ticks クールダウン tick 数
     */
    public static void startCooldown(ServerPlayerEntity player, String weaponKey, int ticks) {
        COOLDOWNS
            .computeIfAbsent(player.getUuid(), id -> new HashMap<>())
            .put(weaponKey, ticks);
    }

    /**
     * 指定武器がクールダウン中かどうかを確認する。
     * @return クールダウン中なら true
     */
    public static boolean isOnCooldown(ServerPlayerEntity player, String weaponKey) {
        Map<String, Integer> playerCd = COOLDOWNS.get(player.getUuid());
        if (playerCd == null) return false;
        Integer remaining = playerCd.get(weaponKey);
        return remaining != null && remaining > 0;
    }

    /**
     * 指定武器の残りクールダウン tick 数を返す（0 = クールダウンなし）。
     */
    public static int getRemaining(ServerPlayerEntity player, String weaponKey) {
        Map<String, Integer> playerCd = COOLDOWNS.get(player.getUuid());
        if (playerCd == null) return 0;
        return playerCd.getOrDefault(weaponKey, 0);
    }

    /**
     * 全クールダウンをリセットする（ベイルアウト後のリスポーン時などに使用）。
     */
    public static void clearAll(ServerPlayerEntity player) {
        COOLDOWNS.remove(player.getUuid());
    }

    private CooldownManager() {}
}
