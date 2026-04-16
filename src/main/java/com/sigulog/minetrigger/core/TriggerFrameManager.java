package com.sigulog.minetrigger.core;

import com.sigulog.minetrigger.MineTriggerNetwork;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プレイヤーごとのオプショントリガー枠を管理する。
 *
 * シングルプレイでは client スレッドと server スレッドが同じ static map を共有するため、
 * computeIfAbsent だけではリスナーが設定されないことがある。
 * listenersSetUp で「サーバー側リスナーが設定済みかどうか」を別管理する。
 */
public final class TriggerFrameManager {

    public static final int SLOT_COUNT = 6;

    private static final Map<UUID, OptionTriggerInventory> inventories = new ConcurrentHashMap<>();

    /**
     * サーバー側リスナーを設定済みのUUIDセット。
     * シングルプレイでクライアントスレッドが先に inventories に登録しても
     * サーバースレッドが後からリスナーを追加できるようにするため分離管理する。
     */
    private static final Set<UUID> listenersSetUp = ConcurrentHashMap.newKeySet();

    // ──────────────────────────────────────────────────────────────
    // 登録
    // ──────────────────────────────────────────────────────────────

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            inventories.remove(uuid);
            listenersSetUp.remove(uuid);
        });
    }

    // ──────────────────────────────────────────────────────────────
    // インベントリ取得（PlayerScreenHandler Mixin からも呼ばれる）
    // ──────────────────────────────────────────────────────────────

    /** インベントリが既に存在する場合のみ返す。NBT保存用（存在しない場合は null）。 */
    @org.jetbrains.annotations.Nullable
    public static OptionTriggerInventory getInventoryOrNull(java.util.UUID uuid) {
        return inventories.get(uuid);
    }

    public static OptionTriggerInventory getOrCreateInventory(PlayerEntity player) {
        // インベントリオブジェクトを取得 or 作成（クライアント/サーバーどちらから呼ばれても同じオブジェクト）
        OptionTriggerInventory inv = inventories.computeIfAbsent(player.getUuid(), k -> new OptionTriggerInventory());

        // サーバー側プレイヤーのみ HUD 同期リスナーを設定。
        // listenersSetUp.add が true = まだ未設定（クライアントが先に inventories を作った場合を含む）
        if (player instanceof ServerPlayerEntity sp && listenersSetUp.add(sp.getUuid())) {
            inv.addListener(invX -> syncToClient(sp));
        }
        return inv;
    }

    // ──────────────────────────────────────────────────────────────
    // 発動
    // ──────────────────────────────────────────────────────────────

    public static boolean activate(ServerPlayerEntity player, int slot) {
        OptionTriggerInventory inv = inventories.computeIfAbsent(player.getUuid(),
            k -> getOrCreateInventory(player));
        ItemStack stack = inv.getStack(slot);
        if (stack.isEmpty()) return false;
        if (!(stack.getItem() instanceof WeaponItem weapon)) return false;
        return weapon.tryActivate(player, Hand.MAIN_HAND);
    }

    // ──────────────────────────────────────────────────────────────
    // HUD 同期
    // ──────────────────────────────────────────────────────────────

    public static void syncToClient(ServerPlayerEntity player) {
        OptionTriggerInventory inv = inventories.computeIfAbsent(player.getUuid(),
            k -> getOrCreateInventory(player));
        String[] keys = new String[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = inv.getStack(i);
            keys[i] = (!stack.isEmpty() && stack.getItem() instanceof WeaponItem wi)
                      ? wi.getWeaponType().configKey : "";
        }
        MineTriggerNetwork.sendTriggerFrameSync(player, keys);
    }

    private TriggerFrameManager() {}
}
