package com.sigulog.minetrigger.core;

import com.sigulog.minetrigger.MineTriggerMod;
import com.sigulog.minetrigger.MineTriggerNetwork;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プレイヤーごとのオプショントリガー枠を管理する。
 *
 * 永続化はファイルベース（NBT回避）。
 * ワールドディレクトリ/minetrigger_options/<uuid>.txt に
 * スロット分のアイテム ID（空スロットは空行）を保存する。
 *
 * JOIN イベント時にロード → 接続済みなので setStack → リスナー → syncToClient が安全。
 */
public final class TriggerFrameManager {

    public static final int SLOT_COUNT = 5;

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
        // 切断時: ファイル保存 → マップから除去
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            saveOptions(server, uuid);
            inventories.remove(uuid);
            listenersSetUp.remove(uuid);
        });

        // 接続完了後: ファイルからロード → HUD 同期
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            loadOptions(server, handler.player);
            syncToClient(handler.player);
        });
    }

    // ──────────────────────────────────────────────────────────────
    // インベントリ取得
    // ──────────────────────────────────────────────────────────────

    /** インベントリが既に存在する場合のみ返す。*/
    @org.jetbrains.annotations.Nullable
    public static OptionTriggerInventory getInventoryOrNull(UUID uuid) {
        return inventories.get(uuid);
    }

    /**
     * リスナーなしでインベントリを取得または作成する。
     * JOIN イベント前（ファイルロード時）に内部から使用。
     */
    private static OptionTriggerInventory getOrCreateInventoryNoListener(UUID uuid) {
        return inventories.computeIfAbsent(uuid, k -> new OptionTriggerInventory());
    }

    public static OptionTriggerInventory getOrCreateInventory(PlayerEntity player) {
        OptionTriggerInventory inv = inventories.computeIfAbsent(player.getUuid(), k -> new OptionTriggerInventory());

        // サーバー側プレイヤーのみ HUD 同期リスナーを設定。
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
        // Shift+数字キーで特殊発動
        return weapon.tryActivate(player, Hand.MAIN_HAND, player.isSneaking());
    }

    // ──────────────────────────────────────────────────────────────
    // HUD 同期
    // ──────────────────────────────────────────────────────────────

    public static void syncToClient(ServerPlayerEntity player) {
        try {
            if (!ServerPlayNetworking.canSend(player, MineTriggerNetwork.TriggerFrameSyncPayload.ID)) return;
        } catch (Exception e) {
            return;
        }

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

    // ──────────────────────────────────────────────────────────────
    // ファイルベース永続化
    // ──────────────────────────────────────────────────────────────

    /** ワールドの minetrigger_options/<uuid>.txt パスを返す。 */
    private static Path optionsFile(MinecraftServer server, UUID uuid) {
        return server.getSavePath(WorldSavePath.ROOT)
            .resolve("minetrigger_options")
            .resolve(uuid + ".txt");
    }

    /** スロット内容をファイルに書き出す。 */
    private static void saveOptions(MinecraftServer server, UUID uuid) {
        OptionTriggerInventory inv = inventories.get(uuid);
        if (inv == null) return;

        try {
            Path file = optionsFile(server, uuid);
            Files.createDirectories(file.getParent());

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < SLOT_COUNT; i++) {
                ItemStack stack = inv.getStack(i);
                if (!stack.isEmpty()) {
                    sb.append(Registries.ITEM.getId(stack.getItem()).toString());
                }
                sb.append('\n');
            }
            Files.writeString(file, sb.toString());
        } catch (IOException e) {
            MineTriggerMod.LOGGER.error("オプションスロット保存失敗 uuid={}", uuid, e);
        }
    }

    /**
     * ファイルからスロット内容を復元する。
     * JOIN イベント後（プレイヤー接続済み）に呼ぶ。
     */
    private static void loadOptions(MinecraftServer server, ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Path file = optionsFile(server, uuid);
        if (!Files.exists(file)) return;

        try {
            List<String> lines = Files.readAllLines(file);
            // リスナーなしでインベントリを準備（まだ接続リスナー未設定の段階）
            OptionTriggerInventory inv = getOrCreateInventoryNoListener(uuid);

            for (int i = 0; i < SLOT_COUNT && i < lines.size(); i++) {
                String id = lines.get(i).trim();
                if (id.isEmpty()) continue;
                Item item = Registries.ITEM.get(Identifier.of(id));
                if (item != null && item instanceof WeaponItem) {
                    inv.setStack(i, new ItemStack(item));
                }
            }
        } catch (IOException e) {
            MineTriggerMod.LOGGER.error("オプションスロット読み込み失敗 uuid={}", uuid, e);
        }
    }

    private TriggerFrameManager() {}
}
