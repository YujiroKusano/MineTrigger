package com.sigulog.minetrigger.weapon.ranged;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.BulletManager;
import com.sigulog.minetrigger.core.CooldownManager;
import com.sigulog.minetrigger.core.TrionSystem;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.ProjectileWeaponItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * アステロイド — 空中に弾を設置し一斉発射するシューター系トリガー。
 *
 * 特殊技  : 照準先の空中に弾を設置（最大6発、位置と発射方向を記憶）
 * 通常発動: 設置した全弾を同時発射（トリオン消費なし）
 *
 * ・設置弾は30秒（600tick）で自動消滅
 * ・上限に達している場合はトリオンを消費せず設置も行わない
 */
public class AsteroidItem extends ProjectileWeaponItem {

    /** 設置できる弾の最大数 */
    private static final int MAX_NODES = 6;

    /** 設置弾の有効期限（tick）。30秒 */
    private static final long EXPIRE_TICKS = 600L;

    /** 設置時のクールダウン（短め） */
    private static final int PLACE_CD_TICKS = 5;

    /** 設置弾の情報（空中位置 + 発射方向 + 消滅時刻） */
    private record BulletNode(Vec3d position, Vec3d direction, long expireTime) {}

    /** プレイヤーUUID → 設置弾リスト */
    private static final Map<UUID, List<BulletNode>> pendingNodes = new ConcurrentHashMap<>();

    public AsteroidItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    // ──────────────────────────────────────────────────────────────
    // 登録
    // ──────────────────────────────────────────────────────────────

    public static void register() {
        // 切断時クリーンアップ
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
            pendingNodes.remove(handler.player.getUuid()));

        // 毎tick: 期限切れノード削除 + 残存ノードのパーティクル表示
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                List<BulletNode> nodes = pendingNodes.get(player.getUuid());
                if (nodes == null || nodes.isEmpty()) continue;

                long now = player.getServerWorld().getTime();
                nodes.removeIf(n -> n.expireTime() <= now);

                // 10tickごとにパーティクルで位置を示す
                if (now % 10 == 0) {
                    ServerWorld world = player.getServerWorld();
                    for (BulletNode n : nodes) {
                        world.spawnParticles(ParticleTypes.CRIT,
                            n.position().x, n.position().y, n.position().z,
                            2, 0.1, 0.1, 0.1, 0.0);
                    }
                }
            }
        });
    }

    // ──────────────────────────────────────────────────────────────
    // 発動ロジック（設置と発射でクールダウン・トリオン消費を分ける）
    // ──────────────────────────────────────────────────────────────

    @Override
    public boolean tryActivate(ServerPlayerEntity player, Hand hand, boolean special) {
        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);

        if (special) {
            // ── 特殊技: 設置 ────────────────────────────────────────
            String placeKey = weaponType.configKey + "_place";
            if (CooldownManager.isOnCooldown(player, placeKey)) {
                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.5f);
                return false;
            }
            // 上限チェック（トリオン消費前）
            List<BulletNode> nodes = pendingNodes.computeIfAbsent(player.getUuid(), k -> new ArrayList<>());
            if (nodes.size() >= MAX_NODES) {
                player.sendMessage(Text.literal("§c[ アステロイド ]§r 設置上限 (" + MAX_NODES + ")"), true);
                return false;
            }
            // トリオン消費（上限未満のときのみ）
            if (!TrionSystem.consumeTrion(player, p.trionUse)) {
                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.3f);
                return false;
            }
            activateSpecial(player, hand);
            CooldownManager.startCooldown(player, placeKey, PLACE_CD_TICKS);
            return true;

        } else {
            // ── 通常: 発射（トリオン消費なし）─────────────────────────
            if (CooldownManager.isOnCooldown(player, weaponType.configKey)) {
                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.5f);
                return false;
            }
            activateNormal(player, hand);
            CooldownManager.startCooldown(player, weaponType.configKey, p.cooldownTicks);
            return true;
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 通常: 設置した全弾を一斉発射
    // ──────────────────────────────────────────────────────────────

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        List<BulletNode> nodes = pendingNodes.remove(player.getUuid());
        if (nodes == null || nodes.isEmpty()) {
            player.sendMessage(Text.literal("§7[ アステロイド ]§r 設置弾なし"), true);
            return;
        }

        WeaponParams p = ModConfig.get().getWeaponParams(weaponType.configKey);
        for (BulletNode node : nodes) {
            BulletManager.fire(player, node.position(), node.direction(),
                BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                    .gravity(0.005).build());
        }
        player.sendMessage(Text.literal("§f[ アステロイド ]§r " + nodes.size() + "発一斉発射"), true);
    }

    // ──────────────────────────────────────────────────────────────
    // 特殊技: 照準先の空中に弾を設置
    // ──────────────────────────────────────────────────────────────

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        List<BulletNode> nodes = pendingNodes.computeIfAbsent(player.getUuid(), k -> new ArrayList<>());

        Vec3d look = player.getRotationVec(1.0f).normalize();
        // 目線から 4 ブロック前方の空中に設置
        Vec3d placePos = player.getEyePos().add(look.multiply(4.0));
        long expireTime = player.getServerWorld().getTime() + EXPIRE_TICKS;

        nodes.add(new BulletNode(placePos, look, expireTime));

        // 設置位置をパーティクルで可視化
        ServerWorld world = player.getServerWorld();
        world.spawnParticles(ParticleTypes.CRIT,
            placePos.x, placePos.y, placePos.z, 8, 0.2, 0.2, 0.2, 0.0);

        player.sendMessage(
            Text.literal("§7[ アステロイド ]§r 設置 (" + nodes.size() + "/" + MAX_NODES + ")"), true);
    }
}
