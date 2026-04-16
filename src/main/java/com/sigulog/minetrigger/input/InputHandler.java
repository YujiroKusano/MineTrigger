package com.sigulog.minetrigger.input;

import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.entity.player.PlayerInventory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 入力イベントのルーティング。
 *
 * ■ 左クリック  → オフハンドスロット（バニラ盾スロット）の武器を発動
 * ■ 右クリック  → 現在選択中のホットバースロット（メインハンド）の武器を発動
 * ■ Qキー       → オプショントリガースロットの武器を発動
 */
public final class InputHandler {

    /** 左クリック（エンティティ攻撃）処理済み */
    private static final Set<UUID> handledThisTick =
        Collections.synchronizedSet(new HashSet<>());

    /** 右クリック処理済み（HandSwing による誤発動を防ぐ） */
    private static final Set<UUID> rightClickHandledThisTick =
        Collections.synchronizedSet(new HashSet<>());

    public static void register() {
        // 毎tick先頭でフラグをリセット
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            handledThisTick.clear();
            rightClickHandledThisTick.clear();
        });

        // 両手武器ルール: メインハンドに両手武器がある場合、左手武器をインベントリに戻す
        // また両手武器が直接オフハンドに置かれた場合も即座に戻す
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                enforceTwoHandedRule(player);
            }
        });

        // ── 左クリック（エンティティへの攻撃）────────────────────────
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            handledThisTick.add(sp.getUuid());
            return fireLeftClick(sp);
        });

        // ── 右クリック: ブロック操作 → メインハンドの武器を発動 ──────────
        // UseBlockCallback が最初に来る。ここで処理済みフラグを立てる。
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (hand == Hand.MAIN_HAND && !rightClickHandledThisTick.contains(sp.getUuid())) {
                rightClickHandledThisTick.add(sp.getUuid());
                fireRightClick(sp);
            }
            return ActionResult.FAIL;
        });

        // ── 右クリック: エンティティ操作 → メインハンドの武器を発動 ──────
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient) return ActionResult.PASS;
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            if (hand == Hand.MAIN_HAND && !rightClickHandledThisTick.contains(sp.getUuid())) {
                rightClickHandledThisTick.add(sp.getUuid());
                fireRightClick(sp);
            }
            return ActionResult.FAIL;
        });

        // ── 右クリック: アイテム使用 → メインハンドの武器を発動 ─────────
        // UseBlock/UseEntity が先に処理済みフラグを立てていれば二重発火しない。
        // OFF_HAND も FAIL で止める。左手武器は AttackEntityCallback / HandSwing で発動する。
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClient) return TypedActionResult.pass(player.getStackInHand(hand));
            if (!(player instanceof ServerPlayerEntity sp)) return TypedActionResult.pass(player.getStackInHand(hand));
            if (hand == Hand.MAIN_HAND && !rightClickHandledThisTick.contains(sp.getUuid())) {
                rightClickHandledThisTick.add(sp.getUuid());
                fireRightClick(sp);
            }
            // MAIN_HAND / OFF_HAND どちらも Item.use() に渡さない
            return TypedActionResult.fail(sp.getStackInHand(hand));
        });
    }

    // ──────────────────────────────────────────────────────────────
    // 左クリック発動（オフハンドスロット）
    // ──────────────────────────────────────────────────────────────

    /**
     * 左クリック発動。Mixin の onHandSwing（空振り）からも呼ばれる。
     * オフハンドに武器があれば発動、なければ何もしない。
     */
    public static ActionResult fireLeftClick(ServerPlayerEntity player) {
        if (player.getOffHandStack().getItem() instanceof WeaponItem offWeapon) {
            offWeapon.tryActivate(player, Hand.OFF_HAND);
        }
        return ActionResult.FAIL; // バニラ攻撃は常にキャンセル
    }

    // ──────────────────────────────────────────────────────────────
    // 右クリック発動（スロット5固定）
    // ──────────────────────────────────────────────────────────────

    /**
     * 右クリック発動。現在選択中のホットバースロット（メインハンド）の武器を使用する。
     */
    public static void fireRightClick(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (stack.getItem() instanceof WeaponItem weapon) {
            weapon.tryActivate(player, Hand.MAIN_HAND);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 両手武器ルール
    // ──────────────────────────────────────────────────────────────

    /**
     * 両手武器のルールを強制する。
     *
     * ・メインハンドに両手武器がある → オフハンドの武器をインベントリに戻す
     * ・オフハンドに両手武器が直接置かれた → インベントリに戻す
     *
     * インベントリが満杯の場合はドロップする。
     */
    private static void enforceTwoHandedRule(ServerPlayerEntity player) {
        ItemStack offStack = player.getOffHandStack();
        // オフハンドに武器がなければ何もしない
        if (offStack.isEmpty() || !(offStack.getItem() instanceof WeaponItem)) return;

        boolean mustClearOffHand;

        if (offStack.getItem() instanceof WeaponItem offWi && offWi.getWeaponType().isTwoHanded()) {
            // 両手武器がオフハンドに直接置かれた
            mustClearOffHand = true;
        } else {
            // メインハンドに両手武器があるかチェック
            ItemStack mainStack = player.getMainHandStack();
            mustClearOffHand = mainStack.getItem() instanceof WeaponItem wi && wi.getWeaponType().isTwoHanded();
        }

        if (!mustClearOffHand) return;

        // オフハンドのアイテムをメインインベントリに挿入、入らない分はドロップ
        PlayerInventory inv = player.getInventory();
        ItemStack toReturn = offStack.copy();
        player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
        inv.insertStack(toReturn);          // 入った分だけ toReturn のカウントが減る
        if (!toReturn.isEmpty()) {          // 入らなかった分 → 足元にドロップ
            player.dropItem(toReturn, false);
        }
        inv.markDirty();
    }

    // ──────────────────────────────────────────────────────────────
    // ユーティリティ
    // ──────────────────────────────────────────────────────────────

    /** このtickでエンティティ攻撃が既に処理されているか（Mixin用）。 */
    public static boolean isHandledThisTick(UUID uuid) {
        return handledThisTick.contains(uuid);
    }

    /** このtickで右クリックが既に処理されているか（Mixin用）。 */
    public static boolean isRightClickHandledThisTick(UUID uuid) {
        return rightClickHandledThisTick.contains(uuid);
    }

    /** 処理済みフラグを手動でセットする。 */
    public static void markHandledThisTick(UUID uuid) {
        handledThisTick.add(uuid);
    }


    private InputHandler() {}
}
