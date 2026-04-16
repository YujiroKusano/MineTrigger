package com.sigulog.minetrigger.core;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ロードアウト管理。
 *
 * ホットバー (slot 0〜8) をスキャンして装備中の武器を特定し、
 * 装備コスト合計を計算して実効トリオン最大値を TrionSystem に渡す。
 *
 * スロット割り当て:
 *   0〜3  : 左手武器スロット（左クリックで発動。オフハンドスロットに置く）
 *   4     : オプション専用スロット（Qキーで発動）
 *   5     : 右手武器スロット（右クリックで発動。固定スロット）
 *   6〜8  : 予備スロット（ロードアウトのコスト計算には含む）
 */
public final class LoadoutManager {

    // 前回スキャン時のインベントリハッシュ（変化検知用）
    private static final Map<UUID, Integer> lastInventoryHash = new HashMap<>();

    public static void register() {
        // 毎20tick（1秒）ごとにインベントリをスキャン
        // （毎tickだと重いため、変化検知を前置して実質的な処理を間引く）
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity serverPlayer : server.getPlayerManager().getPlayerList()) {
                if (serverPlayer.age % 20 == 0) {
                    checkAndUpdate(serverPlayer);
                }
            }
        });
    }

    private static void checkAndUpdate(ServerPlayerEntity player) {
        int hash = computeInventoryHash(player);
        Integer prev = lastInventoryHash.get(player.getUuid());
        if (prev != null && prev == hash) return; // 変化なし
        lastInventoryHash.put(player.getUuid(), hash);
        onLoadoutChanged(player);
    }

    /**
     * ロードアウト変更時の処理（装備コスト再計算 → 実効トリオン更新）。
     */
    public static void onLoadoutChanged(ServerPlayerEntity player) {
        List<WeaponType> equipped = getEquippedWeapons(player);

        // 装備コスト合計
        int totalCost = 0;
        ModConfig cfg = ModConfig.get();
        for (WeaponType type : equipped) {
            WeaponParams params = cfg.getWeaponParams(type.configKey);
            totalCost += params.trionEquipCost;
        }

        float newMax = cfg.baseTrion - totalCost;
        TrionSystem.updateMaxTrion(player, newMax);
    }

    /**
     * ホットバー（slot 0〜8）をスキャンして装備中の WeaponItem のリストを返す。
     */
    public static List<WeaponType> getEquippedWeapons(ServerPlayerEntity player) {
        List<WeaponType> result = new ArrayList<>();
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof WeaponItem wi) {
                result.add(wi.getWeaponType());
            }
        }
        return result;
    }

    /**
     * スロット 0〜3 のメインハンド用武器を返す（null なら空きスロット）。
     */
    public static WeaponType getMainHandWeapon(ServerPlayerEntity player) {
        for (int i = 0; i <= 3; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof WeaponItem wi) {
                return wi.getWeaponType();
            }
        }
        return null;
    }

    /**
     * スロット 5〜8 のオフハンド用武器を返す（null なら空きスロット）。
     */
    public static WeaponType getOffHandWeapon(ServerPlayerEntity player) {
        for (int i = 5; i <= 8; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof WeaponItem wi) {
                return wi.getWeaponType();
            }
        }
        return null;
    }

    /**
     * スロット 4 のオプション武器を返す（null なら未設定）。
     */
    public static WeaponType getOptionWeapon(ServerPlayerEntity player) {
        ItemStack stack = player.getInventory().getStack(4);
        if (!stack.isEmpty() && stack.getItem() instanceof WeaponItem wi && wi.getWeaponType().isOption()) {
            return wi.getWeaponType();
        }
        return null;
    }

    // インベントリ変化検知用ハッシュ（アイテムIDのみ比較）
    private static int computeInventoryHash(ServerPlayerEntity player) {
        int hash = 1;
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            hash = 31 * hash + (stack.isEmpty() ? 0 : stack.getItem().hashCode());
        }
        return hash;
    }

    private LoadoutManager() {}
}
