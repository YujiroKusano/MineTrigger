package com.sigulog.minetrigger.client;

import com.sigulog.minetrigger.MineTriggerMod;
import com.sigulog.minetrigger.MineTriggerNetwork;
import com.sigulog.minetrigger.client.hud.RadialMenuHud;
import com.sigulog.minetrigger.client.hud.TrionHud;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import com.sigulog.minetrigger.weapon.composite.CompositeRoundItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.lwjgl.glfw.GLFW;

/**
 * クライアントサイドの初期化。
 *
 * ■ 数字キー1〜6: オプショントリガーを発動
 *   GLFW 直接ポーリング + エッジ検出でキーバインド競合を回避。
 *   画面が開いているとき（インベントリ等）は発動しない。
 *
 * ■ ホットバー切り替え防止
 *   START_CLIENT_TICK でバニラの hotbarKey[0〜5] を先に消費し、
 *   スロット切り替えが起きないようにする。
 */
public class MineTriggerClient implements ClientModInitializer {

    public static final int TRIGGER_SLOT_COUNT = 6;

    private static final int[] GL_KEYS = {
        GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3,
        GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5, GLFW.GLFW_KEY_6
    };

    /** 前tick のキー押下状態（エッジ検出用） */
    private static final boolean[] prevDown = new boolean[TRIGGER_SLOT_COUNT];

    // ── 合成弾ラジアルメニュー用 ──────────────────────────────────
    /** Shift+RMB の連続ホールド tick 数 */
    private static int compositeHoldTicks = 0;
    /** メニュー開幕に必要なホールド tick（約0.2秒） */
    private static final int RADIAL_HOLD_THRESHOLD = 4;

    // ── スコープ用 ────────────────────────────────────────────────
    /** LMB の前tick状態（エッジ検出） */
    private static boolean prevLmbDown = false;

    @Override
    public void onInitializeClient() {
        MineTriggerMod.LOGGER.info("[MineTrigger] Client initializing...");

        // ── S2Cパケット受信 ──────────────────────────────────────────
        ClientPlayNetworking.registerGlobalReceiver(
            MineTriggerNetwork.TrionSyncPayload.ID,
            (payload, context) -> TrionHud.updateTrion(payload.current(), payload.max())
        );

        ClientPlayNetworking.registerGlobalReceiver(
            MineTriggerNetwork.TriggerFrameSyncPayload.ID,
            (payload, context) -> {
                for (int i = 0; i < TRIGGER_SLOT_COUNT; i++) {
                    TrionHud.updateTriggerFrame(i, payload.slot(i));
                }
            }
        );

        // ── ホットバー切り替えを根本から抑制（数字キー1〜TRIGGER_SLOT_COUNT） ──
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            for (int i = 0; i < TRIGGER_SLOT_COUNT && i < client.options.hotbarKeys.length; i++) {
                //noinspection StatementWithEmptyBody
                while (client.options.hotbarKeys[i].wasPressed()) { /* drain */ }
            }
        });

        // ── キー入力処理（GLFW 直接ポーリング） ──────────────────────
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // 画面が開いているとき（インベントリ等）は prevDown をリセットして終了
            if (client.currentScreen != null) {
                for (int i = 0; i < TRIGGER_SLOT_COUNT; i++) prevDown[i] = false;
                if (RadialMenuHud.isOpen()) { RadialMenuHud.close(); compositeHoldTicks = 0; }
                return;
            }

            long window = client.getWindow().getHandle();

            // ── 合成弾ラジアルメニュー処理 ──────────────────────────
            boolean holdsComposite =
                client.player.getMainHandStack().getItem() instanceof CompositeRoundItem ||
                client.player.getOffHandStack().getItem()  instanceof CompositeRoundItem;

            boolean shiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT)  == GLFW.GLFW_PRESS
                             || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            boolean rmbDown   = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

            if (holdsComposite && shiftDown && rmbDown) {
                compositeHoldTicks++;
                if (compositeHoldTicks >= RADIAL_HOLD_THRESHOLD && !RadialMenuHud.isOpen()) {
                    RadialMenuHud.open(client);
                }
                if (RadialMenuHud.isOpen()) {
                    RadialMenuHud.tick(client);
                }
            } else {
                if (RadialMenuHud.isOpen()) {
                    // メニューが開いていた → 確定
                    int sector = RadialMenuHud.getHovered();
                    if (sector >= 0 &&
                        ClientPlayNetworking.canSend(MineTriggerNetwork.CompositeSelectPayload.ID)) {
                        ClientPlayNetworking.send(new MineTriggerNetwork.CompositeSelectPayload(sector));
                        RadialMenuHud.clientMode = CompositeRoundItem.SECTOR_MODES[sector];
                    }
                    RadialMenuHud.close();
                }
                compositeHoldTicks = 0;
            }

            // ── スコープ（ADS）処理 ──────────────────────────────────
            // 銃系武器をメインに持ち、左手が空のとき LMB ホールドでスコープ
            boolean lmbDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean canScope = false;
            boolean isSniperScope = false;
            if (!RadialMenuHud.isOpen()) {
                var mainStack = client.player.getMainHandStack();
                var offStack  = client.player.getOffHandStack();
                if (mainStack.getItem() instanceof WeaponItem wi && wi.getWeaponType().isGun()
                        && offStack.isEmpty()) {
                    canScope     = true;
                    isSniperScope = wi.getWeaponType().isSniper();
                }
            }
            if (canScope && lmbDown) {
                ScopeHandler.activate(isSniperScope);
            } else {
                ScopeHandler.deactivate();
            }
            ScopeHandler.tick();
            prevLmbDown = lmbDown;

            // ── 数字キー（オプショントリガー発動） ──────────────────
            for (int i = 0; i < TRIGGER_SLOT_COUNT; i++) {
                boolean down = GLFW.glfwGetKey(window, GL_KEYS[i]) == GLFW.GLFW_PRESS;
                if (down && !prevDown[i]) {
                    // 立ち上がりエッジ = キーが押された瞬間
                    if (ClientPlayNetworking.canSend(MineTriggerNetwork.TriggerActivatePayload.ID)) {
                        ClientPlayNetworking.send(new MineTriggerNetwork.TriggerActivatePayload(i));
                    }
                }
                prevDown[i] = down;
            }
        });

        // ── HUD登録 ──────────────────────────────────────────────────
        TrionHud.register();
        RadialMenuHud.register();

        MineTriggerMod.LOGGER.info("[MineTrigger] Client ready.");
    }
}
