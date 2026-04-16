package com.sigulog.minetrigger.core;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * シールド・レイガスト盾モードのアクティブ状態管理。
 *
 * TrionSystem.applyDamage() がここを参照してダメージを軽減する。
 */
public final class ShieldManager {

    /** UUID → 有効期限（サーバー絶対tick） */
    private static final Map<UUID, Long>   expiry    = new ConcurrentHashMap<>();
    /** UUID → ダメージ軽減率（0.0〜1.0） */
    private static final Map<UUID, Double> reduction = new ConcurrentHashMap<>();

    public static void activate(ServerPlayerEntity player, double reductionRate, int durationTicks) {
        long expire = player.getServer().getTicks() + durationTicks;
        expiry.put(player.getUuid(), expire);
        reduction.put(player.getUuid(), reductionRate);
    }

    public static void deactivate(ServerPlayerEntity player) {
        expiry.remove(player.getUuid());
        reduction.remove(player.getUuid());
    }

    public static boolean isActive(ServerPlayerEntity player) {
        Long exp = expiry.get(player.getUuid());
        if (exp == null) return false;
        if (player.getServer().getTicks() >= exp) {
            deactivate(player);
            return false;
        }
        return true;
    }

    /** ダメージ軽減率を返す（シールド非アクティブなら 0.0）。 */
    public static double getReduction(ServerPlayerEntity player) {
        if (!isActive(player)) return 0.0;
        return reduction.getOrDefault(player.getUuid(), 0.0);
    }

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            expiry.remove(handler.player.getUuid());
            reduction.remove(handler.player.getUuid());
        });
    }

    private ShieldManager() {}
}
