package com.sigulog.minetrigger.core;

import com.sigulog.minetrigger.MineTriggerMod;
import com.sigulog.minetrigger.MineTriggerNetwork;
import com.sigulog.minetrigger.TrionDataAttachments;
import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.core.ShieldManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * トリオンシステムのコアロジック。
 *
 * - バニラダメージをキャンセルしてトリオンダメージに変換
 * - スポーン/リスポーン時にステータス効果（速度・ジャンプ）を適用
 * - トリオンが 0 以下になったらベイルアウト（死亡処理）
 * - トリオン値の変化をクライアントへ同期
 */
public final class TrionSystem {

    // ベイルアウト中のプレイヤー（ALLOW_DAMAGEの再帰を防ぐ）
    private static final Set<UUID> bailingOut =
        Collections.synchronizedSet(new HashSet<>());

    // 前tickの着地状態（ジャンプ検知用）
    private static final Map<UUID, Boolean> prevOnGround = new ConcurrentHashMap<>();

    /**
     * ジャンプ時の水平速度ブースト倍率。
     * 走りながらジャンプすると横方向の慣性が増し、爽快感が上がる。
     */
    private static final double JUMP_HORIZONTAL_BOOST = 1.6;

    // 速度・ジャンプ属性モディファイアの識別子
    private static final Identifier SPEED_MODIFIER_ID =
        Identifier.of(MineTriggerMod.MOD_ID, "trion_speed");
    private static final Identifier JUMP_MODIFIER_ID =
        Identifier.of(MineTriggerMod.MOD_ID, "trion_jump");

    public static void register() {
        // ── 1. バニラダメージをトリオンダメージに置き換え ──────────────
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player) {
                // ベイルアウト中（player.kill() によるダメージなど）はバニラに委ねる
                if (bailingOut.contains(player.getUuid())) {
                    return true;
                }
                // トリオンが既に 0 以下（bailout 済み）もバニラに委ねて死亡を確定させる
                if (getTrion(player) <= 0) {
                    return true;
                }
                applyDamage(player, amount);
                return false; // バニラのダメージをキャンセル
            }
            return true;
        });

        // ── 2. スポーン / リスポーン時の初期化 ───────────────────────
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            initPlayer(newPlayer);
        });

        // ── 3. 毎ティック処理（バッグワームの継続消費など将来使用） ──
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity serverPlayer : server.getPlayerManager().getPlayerList()) {
                tickPlayer(serverPlayer);
            }
        });

        // ── 4. プレイヤー参加時の初期化 ──────────────────────────────
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            initPlayer(handler.player);
        });

        // ── 5. プレイヤー切断時のクリーンアップ ──────────────────────
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            prevOnGround.remove(handler.player.getUuid());
        });
    }

    // ──────────────────────────────────────────────────────────────
    // 初期化
    // ──────────────────────────────────────────────────────────────

    /**
     * プレイヤー参加・リスポーン時に呼ぶ。
     * - 体力を最大に保つ（バニラのハートゲージは常時満タンにして隠す）
     * - 速度・ジャンプ効果を付与
     * - トリオン値を同期
     */
    public static void initPlayer(ServerPlayerEntity player) {
        // バニラ体力を常時満タン（トリオンとは別管理）
        player.setHealth(player.getMaxHealth());

        // 速度・ジャンプ属性モディファイアを追加（重複防止のため先に削除）
        applyAttributeModifiers(player);

        // トリオン最大値を取得（未設定なら基礎値で初期化）
        float max = player.getAttachedOrSet(TrionDataAttachments.TRION_MAX, (float) ModConfig.get().baseTrion);
        // リスポーン・参加時は常にトリオンを満タンに回復
        player.setAttached(TrionDataAttachments.TRION_CURRENT, max);

        syncToClient(player, max, max);
    }

    private static void applyAttributeModifiers(ServerPlayerEntity player) {
        ModConfig cfg = ModConfig.get();

        // 移動速度
        var speedAttr = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(SPEED_MODIFIER_ID);
            // ADD_MULTIPLIED_BASE: total = base + base * value → 1.5x なら value = 0.5
            speedAttr.addPersistentModifier(new EntityAttributeModifier(
                SPEED_MODIFIER_ID,
                cfg.moveSpeed - 1.0,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        }

        // ジャンプ力（1.21 で追加された GENERIC_JUMP_STRENGTH 属性）
        var jumpAttr = player.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH);
        if (jumpAttr != null) {
            jumpAttr.removeModifier(JUMP_MODIFIER_ID);
            jumpAttr.addPersistentModifier(new EntityAttributeModifier(
                JUMP_MODIFIER_ID,
                cfg.jumpBoost - 1.0,
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        }
    }

    // ──────────────────────────────────────────────────────────────
    // トリオン操作
    // ──────────────────────────────────────────────────────────────

    /** 現在のトリオン量を取得する */
    public static float getTrion(ServerPlayerEntity player) {
        return player.getAttachedOrSet(TrionDataAttachments.TRION_CURRENT,
            (float) ModConfig.get().baseTrion);
    }

    /** 最大トリオン量を取得する */
    public static float getMaxTrion(ServerPlayerEntity player) {
        return player.getAttachedOrSet(TrionDataAttachments.TRION_MAX,
            (float) ModConfig.get().baseTrion);
    }

    /** トリオン量を直接セットしてクライアントに同期する */
    public static void setTrion(ServerPlayerEntity player, float value) {
        float max = getMaxTrion(player);
        float clamped = Math.max(0, Math.min(value, max));
        player.setAttached(TrionDataAttachments.TRION_CURRENT, clamped);
        syncToClient(player, clamped, max);

        if (clamped <= 0) {
            bailout(player);
        }
    }

    /**
     * トリオンを消費する。
     * @return 消費できた場合 true、残量不足の場合 false
     */
    public static boolean consumeTrion(ServerPlayerEntity player, float amount) {
        float current = getTrion(player);
        if (current < amount) {
            return false;
        }
        setTrion(player, current - amount);
        return true;
    }

    /**
     * ダメージをトリオン減少に変換する（ServerLivingEntityEvents.ALLOW_DAMAGE から呼ばれる）。
     * 攻撃力倍率はダメージ量そのものに反映済みとして扱う。
     */
    public static void applyDamage(ServerPlayerEntity player, float amount) {
        // シールドによるダメージ軽減
        float reduction = (float) ShieldManager.getReduction(player);
        float effective = amount * (1.0f - reduction);

        float current = getTrion(player);
        float newTrion = current - effective;
        setTrion(player, newTrion);
        // ※ バニラ体力の回復は tickPlayer が毎tick行う。
        //   ここで setHealth するとベイルアウトの kill が無効化されるため削除。
    }

    /**
     * 最大トリオン量を更新する（ロードアウト変更時に LoadoutManager から呼ばれる）。
     * 現在のトリオン量が新しい最大値を超える場合はクランプする。
     */
    public static void updateMaxTrion(ServerPlayerEntity player, float newMax) {
        float clamped = Math.max(1, newMax);
        player.setAttached(TrionDataAttachments.TRION_MAX, clamped);

        float current = getTrion(player);
        if (current > clamped) {
            player.setAttached(TrionDataAttachments.TRION_CURRENT, clamped);
        }
        syncToClient(player, Math.min(current, clamped), clamped);
    }

    // ──────────────────────────────────────────────────────────────
    // ベイルアウト
    // ──────────────────────────────────────────────────────────────

    /**
     * トリオンがゼロになったときのベイルアウト処理。
     * プレイヤーを死亡状態にしてリスポーン地点へ戻す。
     * アイテムはドロップしない（keep_inventoryに関わらず）。
     */
    public static void bailout(ServerPlayerEntity player) {
        if (!bailingOut.add(player.getUuid())) return; // 既にベイルアウト中
        try {
            MineTriggerMod.LOGGER.info("[MineTrigger] ベイルアウト: {}", player.getName().getString());
            player.sendMessage(Text.literal("§c[ ベイルアウト ]§r トリオンが尽きました。"), true);
            player.kill();
        } finally {
            bailingOut.remove(player.getUuid());
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 毎ティック処理
    // ──────────────────────────────────────────────────────────────

    private static void tickPlayer(ServerPlayerEntity player) {
        // 死亡中は体力を操作しない（死亡判定・リスポーン画面を妨げないため）
        if (!player.isAlive()) return;

        // バニラ体力が下がっていたら（他のダメージソースなど）満タンに戻す
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        // ── ジャンプ時の水平慣性ブースト ─────────────────────────────
        boolean onGround = player.isOnGround();
        Boolean prev = prevOnGround.get(player.getUuid());

        if (prev != null && prev && !onGround && player.getVelocity().y > 0) {
            // 着地→離地かつ上昇中 = ジャンプした瞬間
            Vec3d vel = player.getVelocity();
            double hx = vel.x;
            double hz = vel.z;
            double hSpeed = Math.sqrt(hx * hx + hz * hz);
            if (hSpeed > 0.01) {
                // 水平成分をブースト
                player.setVelocity(
                    hx * JUMP_HORIZONTAL_BOOST,
                    vel.y,
                    hz * JUMP_HORIZONTAL_BOOST
                );
                player.velocityModified = true;
            }
        }

        prevOnGround.put(player.getUuid(), onGround);
    }

    // ──────────────────────────────────────────────────────────────
    // クライアント同期
    // ──────────────────────────────────────────────────────────────

    private static void syncToClient(ServerPlayerEntity player, float current, float max) {
        MineTriggerNetwork.sendTrionSync(player, current, max);
    }

    private TrionSystem() {}
}
