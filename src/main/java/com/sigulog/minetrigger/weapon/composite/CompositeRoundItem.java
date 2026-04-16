package com.sigulog.minetrigger.weapon.composite;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.BulletManager;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 合成弾 — 特殊技（Shift+右クリック）で組み合わせを切り替えられるシューター系複合トリガー。
 *
 * 通常発動: 現在のモードで弾を発射
 * 特殊技  : 次のモードへサイクル（4種ループ）
 *
 * モード一覧:
 *   HOUND_METEORA    — ホーミング爆発弾
 *   VIPER_METEORA    — ジグザグ爆発弾（3方向）
 *   ASTEROID_METEORA — 分裂爆発弾（5方向）
 *   ASTEROID_HOUND   — 分裂ホーミング弾（5方向）
 */
public class CompositeRoundItem extends WeaponItem {

    public enum Mode {
        HOUND_METEORA   ("ハウンドメテオラ",    "§4"),
        VIPER_METEORA   ("バイパーメテオラ",    "§5"),
        ASTEROID_METEORA("アステロイドメテオラ", "§6"),
        ASTEROID_HOUND  ("アステロイドハウンド", "§b");

        public final String displayName;
        public final String color;

        Mode(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    /**
     * ラジアルメニューのセクター番号 → モードの対応表。
     * RadialMenuHud と一致させること。
     *   0: right  → VIPER_METEORA
     *   1: bottom → ASTEROID_METEORA
     *   2: left   → ASTEROID_HOUND
     *   3: top    → HOUND_METEORA
     */
    public static final Mode[] SECTOR_MODES = {
        Mode.VIPER_METEORA,
        Mode.ASTEROID_METEORA,
        Mode.ASTEROID_HOUND,
        Mode.HOUND_METEORA,
    };

    private static final Map<UUID, Mode> playerModes = new ConcurrentHashMap<>();

    public CompositeRoundItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    public static Mode getMode(UUID uuid) {
        return playerModes.getOrDefault(uuid, Mode.HOUND_METEORA);
    }

    public static Mode getMode(ServerPlayerEntity player) {
        return getMode(player.getUuid());
    }

    /** ラジアルメニューからモードを設定する（C2Sパケット受信時に呼ぶ） */
    public static void setMode(UUID uuid, int sectorIndex) {
        if (sectorIndex >= 0 && sectorIndex < SECTOR_MODES.length) {
            playerModes.put(uuid, SECTOR_MODES[sectorIndex]);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Shift押下時はラジアルメニューに委ねる（トリオン消費なし）
    // ──────────────────────────────────────────────────────────────

    @Override
    public boolean tryActivate(ServerPlayerEntity player, Hand hand) {
        if (player.isSneaking()) return false; // ラジアルメニューが担当
        return super.tryActivate(player, hand);
    }

    // ──────────────────────────────────────────────────────────────
    // 通常発動: 現在のモードで発射
    // ──────────────────────────────────────────────────────────────

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        Mode mode = getMode(player);
        Vec3d look   = player.getRotationVec(1.0f).normalize();
        Vec3d eyePos = player.getEyePos();

        switch (mode) {
            case HOUND_METEORA -> {
                // ホーミング爆発弾 1発
                Vec3d start = eyePos.add(look.multiply(0.5));
                BulletManager.fire(player, start, look,
                    BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                        .homing(p.trackingSpeed)
                        .splash(p.splashRadius)
                        .build());
                player.sendMessage(Text.literal(mode.color + "[ " + mode.displayName + " ]§r 発射"), true);
            }
            case VIPER_METEORA -> {
                // ジグザグ爆発弾 3方向
                double[] offsets = { -0.2, 0.0, 0.2 };
                for (double off : offsets) {
                    Vec3d dir   = new Vec3d(look.x + off, look.y, look.z).normalize();
                    Vec3d start = eyePos.add(dir.multiply(0.5));
                    BulletManager.fire(player, start, dir,
                        BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                            .splash(p.splashRadius)
                            .build());
                }
                player.sendMessage(Text.literal(mode.color + "[ " + mode.displayName + " ]§r 3方向発射"), true);
            }
            case ASTEROID_METEORA -> {
                // 前方5方向へ爆発弾（分裂イメージ）
                fireSpread(player, look, eyePos, p, false);
                player.sendMessage(Text.literal(mode.color + "[ " + mode.displayName + " ]§r 5方向発射"), true);
            }
            case ASTEROID_HOUND -> {
                // 前方5方向へホーミング弾
                fireSpread(player, look, eyePos, p, true);
                player.sendMessage(Text.literal(mode.color + "[ " + mode.displayName + " ]§r 5方向発射"), true);
            }
        }
    }

    /** 前方+上下左右 5方向に弾を発射する */
    private void fireSpread(ServerPlayerEntity player, Vec3d look, Vec3d eyePos,
                             WeaponParams p, boolean homing) {
        // 正面 + 上下左右それぞれ±0.2オフセット
        double[][] dirs = {
            {  0.0,  0.0 },
            {  0.2,  0.0 },
            { -0.2,  0.0 },
            {  0.0,  0.2 },
            {  0.0, -0.2 },
        };
        for (double[] d : dirs) {
            Vec3d dir   = new Vec3d(look.x + d[0], look.y + d[1], look.z).normalize();
            Vec3d start = eyePos.add(dir.multiply(0.5));
            BulletManager.BulletOptions.Builder builder =
                BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage);
            if (homing) builder.homing(p.trackingSpeed);
            else        builder.splash(p.splashRadius);
            BulletManager.fire(player, start, dir, builder.build());
        }
    }

}
