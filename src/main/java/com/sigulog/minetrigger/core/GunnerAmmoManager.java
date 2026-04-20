package com.sigulog.minetrigger.core;

import com.sigulog.minetrigger.MineTriggerMod;
import com.sigulog.minetrigger.MineTriggerNetwork;
import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.weapon.WeaponType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 銃ごとの弾薬設定と弾薬モードを管理する。
 *
 * gunAmmos  : Map<UUID, Map<WeaponType銃種, WeaponType[] 弾薬>>
 *             ガンナー系: 最大2スロット; スナイパー系: 最大1スロット
 * gunModes  : Map<UUID, Map<WeaponType銃種, Integer>>
 *             0=通常弾, 1=ammo[0], 2=ammo[1]
 */
public final class GunnerAmmoManager {

    /** 銃種ごとのアモスロット数 */
    public static int ammoSlotCount(WeaponType gun) {
        if (gun.isGunnerGun()) return 2;
        if (gun.isSniper())    return 1;
        return 0;
    }

    // ── ストレージ ────────────────────────────────────────────────
    /** player UUID → (gun WeaponType → ammo WeaponType[]) */
    private static final Map<UUID, Map<WeaponType, WeaponType[]>> gunAmmos =
        new ConcurrentHashMap<>();

    /** player UUID → (gun WeaponType → current mode index) */
    private static final Map<UUID, Map<WeaponType, Integer>> gunModes =
        new ConcurrentHashMap<>();

    // ──────────────────────────────────────────────────────────────
    // 登録
    // ──────────────────────────────────────────────────────────────

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.player.getUuid();
            saveAmmo(server, uuid);
            gunAmmos.remove(uuid);
            gunModes.remove(uuid);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            loadAmmo(server, handler.player);
        });
    }

    // ──────────────────────────────────────────────────────────────
    // 弾薬設定の取得・変更
    // ──────────────────────────────────────────────────────────────

    /** 指定した銃のアモ配列を返す（設定されていないスロットは null） */
    public static WeaponType[] getAmmos(UUID player, WeaponType gun) {
        int slots = ammoSlotCount(gun);
        WeaponType[] arr = gunAmmos.getOrDefault(player, Map.of()).get(gun);
        WeaponType[] result = new WeaponType[slots];
        if (arr != null) {
            for (int i = 0; i < Math.min(slots, arr.length); i++) {
                result[i] = arr[i];
            }
        }
        return result;
    }

    /**
     * 銃のアモスロットを設定する（サーバー側から呼ぶ）。
     * @param ammo null = クリア
     */
    public static void setAmmo(ServerPlayerEntity player, WeaponType gun, int slot, @Nullable WeaponType ammo) {
        int slots = ammoSlotCount(gun);
        if (slot < 0 || slot >= slots) return;

        gunAmmos.computeIfAbsent(player.getUuid(), k -> new ConcurrentHashMap<>())
            .compute(gun, (k, arr) -> {
                if (arr == null) arr = new WeaponType[slots];
                else if (arr.length < slots) {
                    WeaponType[] newArr = new WeaponType[slots];
                    System.arraycopy(arr, 0, newArr, 0, arr.length);
                    arr = newArr;
                }
                arr[slot] = ammo;
                return arr;
            });

        // モードが範囲外になったらリセット
        int mode = getMode(player.getUuid(), gun);
        int filled = countFilled(player.getUuid(), gun);
        if (mode > filled) {
            setMode(player.getUuid(), gun, 0);
        }

        syncGunToClient(player, gun);
    }

    // ──────────────────────────────────────────────────────────────
    // モード管理
    // ──────────────────────────────────────────────────────────────

    public static int getMode(UUID player, WeaponType gun) {
        return gunModes.getOrDefault(player, Map.of()).getOrDefault(gun, 0);
    }

    private static void setMode(UUID player, WeaponType gun, int mode) {
        gunModes.computeIfAbsent(player, k -> new ConcurrentHashMap<>()).put(gun, mode);
    }

    /** 設定済みアモ数（空スロット除く）を数える */
    private static int countFilled(UUID player, WeaponType gun) {
        WeaponType[] arr = gunAmmos.getOrDefault(player, Map.of()).get(gun);
        if (arr == null) return 0;
        int count = 0;
        for (WeaponType a : arr) if (a != null) count++;
        return count;
    }

    /** 現在のアモタイプを返す（null = 通常弾モード） */
    @Nullable
    public static WeaponType getCurrentAmmo(UUID player, WeaponType gun) {
        int mode = getMode(player, gun);
        if (mode == 0) return null;
        WeaponType[] arr = gunAmmos.getOrDefault(player, Map.of()).get(gun);
        if (arr == null || mode - 1 >= arr.length) return null;
        return arr[mode - 1];
    }

    /**
     * モードを次に進める。
     * 設定済みスロットのみ: 通常 → ammo0 → ammo1 → 通常...
     */
    public static void cycleAmmo(ServerPlayerEntity player, WeaponType gun) {
        WeaponType[] arr = gunAmmos.getOrDefault(player.getUuid(), Map.of()).get(gun);
        int slots = ammoSlotCount(gun);
        int cur = getMode(player.getUuid(), gun);

        // 次の有効なモードを探す
        for (int i = 1; i <= slots; i++) {
            int next = (cur + i) % (slots + 1); // 0..slots の循環
            if (next == 0) { setMode(player.getUuid(), gun, 0); syncGunToClient(player, gun); return; }
            if (arr != null && next - 1 < arr.length && arr[next - 1] != null) {
                setMode(player.getUuid(), gun, next);
                syncGunToClient(player, gun);
                return;
            }
        }
        // 有効なアモがなければ通常に
        setMode(player.getUuid(), gun, 0);
        syncGunToClient(player, gun);
    }

    // ──────────────────────────────────────────────────────────────
    // 発射（サイクル込み）
    // ──────────────────────────────────────────────────────────────

    /**
     * 現在のアモで発射しモードを次へ進める。
     * @return 発射したアモタイプ（null = 通常弾モード → 呼び元が通常発動）
     */
    @Nullable
    public static WeaponType fireAndCycle(ServerPlayerEntity player, Hand hand, WeaponType gun) {
        WeaponType ammo = getCurrentAmmo(player.getUuid(), gun);
        cycleAmmo(player, gun);
        if (ammo == null) return null;

        WeaponParams p = ModConfig.get().getWeaponParams(ammo.configKey);
        if (p == null) return null;

        Vec3d look  = player.getRotationVec(1.0f).normalize();
        Vec3d start = player.getEyePos().add(look.multiply(0.5));

        switch (ammo) {
            case ASTEROID -> BulletManager.fire(player, start, look,
                BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                    .gravity(0.005).build());
            case METEORA  -> BulletManager.fire(player, start, look,
                BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                    .splash(p.splashRadius).blockDestroy(p.splashRadius * 0.5)
                    .gravity(0.005).build());
            case VIPER    -> BulletManager.fire(player, start, look,
                BulletManager.BulletOptions.builder(0.12, p.range, (float) p.damage)
                    .viperSteering().build());
            case HOUND    -> BulletManager.fire(player, start, look,
                BulletManager.BulletOptions.builder(p.speed, p.range, (float) p.damage)
                    .homing(p.trackingSpeed).gravity(0.004).build());
            case RED_BULLET -> BulletManager.fire(player, start, look,
                BulletManager.BulletOptions.builder(p.speed, p.range, 0f)
                    .effect(net.minecraft.entity.effect.StatusEffects.SLOWNESS,
                            p.slownessDurationTicks, p.slownessLevel - 1)
                    .shieldPenetrating().stickArrow().gravity(0.003).build());
            default -> { return null; }
        }

        String name = switch (ammo) {
            case ASTEROID  -> "§f[アステロイド]";
            case METEORA   -> "§c[メテオラ]";
            case VIPER     -> "§d[バイパー]";
            case HOUND     -> "§b[ハウンド]";
            case RED_BULLET-> "§4[レッドバレット]";
            default        -> "§7[弾薬]";
        };
        player.sendMessage(Text.literal(name + "§r 弾発射"), true);
        return ammo;
    }

    // ──────────────────────────────────────────────────────────────
    // HUD 同期
    // ──────────────────────────────────────────────────────────────

    public static void syncGunToClient(ServerPlayerEntity player, WeaponType gun) {
        try {
            if (!ServerPlayNetworking.canSend(player, MineTriggerNetwork.GunAmmoUpdatePayload.ID)) return;
        } catch (Exception e) { return; }

        WeaponType[] arr = getAmmos(player.getUuid(), gun);
        String a0 = (arr.length > 0 && arr[0] != null) ? arr[0].configKey : "";
        String a1 = (arr.length > 1 && arr[1] != null) ? arr[1].configKey : "";
        int mode = getMode(player.getUuid(), gun);
        ServerPlayNetworking.send(player,
            new MineTriggerNetwork.GunAmmoUpdatePayload(gun.configKey, a0, a1, mode));
    }

    /** ログイン時: 全銃種を同期 */
    public static void syncAllToClient(ServerPlayerEntity player) {
        for (WeaponType gun : WeaponType.values()) {
            if (gun.isGunnerGun() || gun.isSniper()) {
                syncGunToClient(player, gun);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 永続化
    // ──────────────────────────────────────────────────────────────

    private static Path ammoFile(MinecraftServer server, UUID uuid) {
        return server.getSavePath(WorldSavePath.ROOT)
            .resolve("minetrigger_ammo2")
            .resolve(uuid + ".txt");
    }

    private static void saveAmmo(MinecraftServer server, UUID uuid) {
        Map<WeaponType, WeaponType[]> ammos = gunAmmos.get(uuid);
        Map<WeaponType, Integer> modes = gunModes.get(uuid);
        if (ammos == null && modes == null) return;
        try {
            Path file = ammoFile(server, uuid);
            Files.createDirectories(file.getParent());
            StringBuilder sb = new StringBuilder();
            // ammos
            if (ammos != null) {
                for (var entry : ammos.entrySet()) {
                    WeaponType gun = entry.getKey();
                    WeaponType[] arr = entry.getValue();
                    for (int i = 0; i < arr.length; i++) {
                        if (arr[i] != null) {
                            sb.append("ammo ").append(gun.configKey)
                              .append(' ').append(i)
                              .append(' ').append(arr[i].configKey).append('\n');
                        }
                    }
                }
            }
            // modes
            if (modes != null) {
                for (var entry : modes.entrySet()) {
                    sb.append("mode ").append(entry.getKey().configKey)
                      .append(' ').append(entry.getValue()).append('\n');
                }
            }
            Files.writeString(file, sb.toString());
        } catch (IOException e) {
            MineTriggerMod.LOGGER.error("弾薬設定保存失敗 uuid={}", uuid, e);
        }
    }

    private static void loadAmmo(MinecraftServer server, ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        Path file = ammoFile(server, uuid);
        if (!Files.exists(file)) return;
        try {
            List<String> lines = Files.readAllLines(file);
            Map<WeaponType, WeaponType[]> ammos = new ConcurrentHashMap<>();
            Map<WeaponType, Integer>      modes  = new ConcurrentHashMap<>();

            for (String line : lines) {
                String[] parts = line.trim().split(" ");
                if (parts.length < 3) continue;
                switch (parts[0]) {
                    case "ammo" -> {
                        if (parts.length < 4) continue;
                        WeaponType gun  = WeaponType.fromConfigKey(parts[1]);
                        WeaponType ammo = WeaponType.fromConfigKey(parts[3]);
                        if (gun == null || ammo == null) continue;
                        int slot = Integer.parseInt(parts[2]);
                        int slots = ammoSlotCount(gun);
                        if (slot < 0 || slot >= slots) continue;
                        // validate ammo type
                        if (gun.isGunnerGun() && !ammo.isGunnerAmmo()) continue;
                        if (gun.isSniper()    && !ammo.isSniperAmmo()) continue;
                        WeaponType[] arr = ammos.computeIfAbsent(gun, k -> new WeaponType[slots]);
                        arr[slot] = ammo;
                    }
                    case "mode" -> {
                        WeaponType gun = WeaponType.fromConfigKey(parts[1]);
                        if (gun == null) continue;
                        try { modes.put(gun, Integer.parseInt(parts[2])); }
                        catch (NumberFormatException ignored) {}
                    }
                }
            }
            gunAmmos.put(uuid, ammos);
            gunModes.put(uuid, modes);
        } catch (IOException e) {
            MineTriggerMod.LOGGER.error("弾薬設定読み込み失敗 uuid={}", uuid, e);
        }
        // ログイン後に同期
        syncAllToClient(player);
    }

    private GunnerAmmoManager() {}
}
