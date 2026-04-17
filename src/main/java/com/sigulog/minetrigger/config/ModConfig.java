package com.sigulog.minetrigger.config;

import com.sigulog.minetrigger.MineTriggerMod;
import net.fabricmc.loader.api.FabricLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * config/worldtrigger/config.yml の読み込みと保持。
 *
 * SnakeYAML で YAML をパースし、各フィールドに値をマッピングする。
 * 起動時に load() を呼ぶ。ファイルが存在しない場合はデフォルト値で生成する。
 */
public final class ModConfig {

    private static ModConfig INSTANCE = new ModConfig();

    // ── プレイヤー基本ステータス ──
    public int baseTrion = 100;
    public double moveSpeed = 1.5;
    public double jumpBoost = 2.0;
    public double attackPowerMultiplier = 0.5;

    // ── ゲームルール ──
    public boolean trionRegen = false;
    public double criticalRateBase = 0.1;
    public double criticalDamageMultiplier = 2.0;

    // ── 武器パラメータ（configKey → WeaponParams） ──
    private final Map<String, WeaponParams> weapons = new HashMap<>();

    public static ModConfig get() {
        return INSTANCE;
    }

    /** WeaponParams を取得。未設定の場合はデフォルト値を返す */
    public WeaponParams getWeaponParams(String configKey) {
        return weapons.computeIfAbsent(configKey, k -> new WeaponParams());
    }

    // ──────────────────────────────────────────────────────────────
    // 読み込み
    // ──────────────────────────────────────────────────────────────

    public static void load() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("worldtrigger");
        Path configFile = configDir.resolve("config.yml");

        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(configDir);
                writeDefault(configFile);
                MineTriggerMod.LOGGER.info("[MineTrigger] デフォルト config.yml を生成しました: {}", configFile);
            }

            try (InputStream in = Files.newInputStream(configFile)) {
                Yaml yaml = new Yaml();
                Map<String, Object> root = yaml.load(in);
                if (root != null) {
                    INSTANCE = parse(root);
                }
            }
            MineTriggerMod.LOGGER.info("[MineTrigger] config.yml を読み込みました。baseTrion={}", INSTANCE.baseTrion);

        } catch (IOException e) {
            MineTriggerMod.LOGGER.error("[MineTrigger] config.yml の読み込みに失敗しました。デフォルト値を使用します。", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static ModConfig parse(Map<String, Object> root) {
        ModConfig cfg = new ModConfig();

        // player section
        if (root.get("player") instanceof Map<?, ?> player) {
            cfg.baseTrion             = asInt(player.get("base_trion"), cfg.baseTrion);
            cfg.moveSpeed             = asDouble(player.get("move_speed"), cfg.moveSpeed);
            cfg.jumpBoost             = asDouble(player.get("jump_boost"), cfg.jumpBoost);
            cfg.attackPowerMultiplier = asDouble(player.get("attack_power_multiplier"), cfg.attackPowerMultiplier);
        }

        // game section
        if (root.get("game") instanceof Map<?, ?> game) {
            cfg.trionRegen              = asBool(game.get("trion_regen"), cfg.trionRegen);
            cfg.criticalRateBase        = asDouble(game.get("critical_rate_base"), cfg.criticalRateBase);
            cfg.criticalDamageMultiplier = asDouble(game.get("critical_damage_multiplier"), cfg.criticalDamageMultiplier);
        }

        // weapons section
        if (root.get("weapons") instanceof Map<?, ?> weaponsMap) {
            for (Map.Entry<?, ?> entry : weaponsMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> wMap) {
                    cfg.weapons.put(key, parseWeapon((Map<String, Object>) wMap));
                }
            }
        }

        // options section (同じ WeaponParams を流用)
        if (root.get("options") instanceof Map<?, ?> optMap) {
            for (Map.Entry<?, ?> entry : optMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> wMap) {
                    cfg.weapons.put(key, parseWeapon((Map<String, Object>) wMap));
                }
            }
        }

        // composite section (合成弾パラメータ)
        if (root.get("composite") instanceof Map<?, ?> compMap) {
            for (Map.Entry<?, ?> entry : compMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (entry.getValue() instanceof Map<?, ?> wMap) {
                    cfg.weapons.put(key, parseWeapon((Map<String, Object>) wMap));
                }
            }
        }

        return cfg;
    }

    private static WeaponParams parseWeapon(Map<String, Object> m) {
        WeaponParams p = new WeaponParams();
        p.trionEquipCost         = asInt(m.get("trion_equip_cost"), p.trionEquipCost);
        p.damage                 = asInt(m.get("damage"), p.damage);
        p.range                  = asDouble(m.get("range"), p.range);
        p.cooldownTicks          = asInt(m.get("cooldown_ticks"), p.cooldownTicks);
        p.trionUse               = asInt(m.get("trion_use"), p.trionUse);
        p.pelletCount            = asInt(m.get("pellet_count"), p.pelletCount);
        p.closeRangeBonus        = asDouble(m.get("close_range_bonus"), p.closeRangeBonus);
        p.closeRangeThreshold    = asDouble(m.get("close_range_threshold"), p.closeRangeThreshold);
        p.slownessDurationTicks  = asInt(m.get("slowness_duration_ticks"), p.slownessDurationTicks);
        p.slownessLevel          = asInt(m.get("slowness_level"), p.slownessLevel);
        p.splashRadius           = asDouble(m.get("splash_radius"), p.splashRadius);
        p.mineTriggerRadius      = asDouble(m.get("mine_trigger_radius"), p.mineTriggerRadius);
        p.shieldPenetrate        = asBool(m.get("shield_penetrate"), p.shieldPenetrate);
        p.shieldPenetrateRate    = asDouble(m.get("shield_penetrate_rate"), p.shieldPenetrateRate);
        p.speed                  = asDouble(m.get("speed"), p.speed);
        p.trackingSpeed          = asDouble(m.get("tracking_speed"), p.trackingSpeed);
        p.maxNodes               = asInt(m.get("max_nodes"), p.maxNodes);
        p.nodeDurationSeconds    = asInt(m.get("node_duration_seconds"), p.nodeDurationSeconds);
        p.trionUsePerNode        = asInt(m.get("trion_use_per_node"), p.trionUsePerNode);
        p.trionScaling           = asStr(m.get("trion_scaling"), p.trionScaling);
        p.damageReduction        = asDouble(m.get("damage_reduction"), p.damageReduction);
        p.maxCount               = asInt(m.get("max_count"), p.maxCount);
        p.durationSeconds        = asInt(m.get("duration_seconds"), p.durationSeconds);
        p.wireLength             = asDouble(m.get("wire_length"), p.wireLength);
        p.maxPlaced              = asInt(m.get("max_placed"), p.maxPlaced);
        p.catapultVelocity       = asDouble(m.get("catapult_velocity"), p.catapultVelocity);
        p.trionDrainPerSecond    = asInt(m.get("trion_drain_per_second"), p.trionDrainPerSecond);
        return p;
    }

    // ──────────────────────────────────────────────────────────────
    // デフォルト config.yml 生成
    // ──────────────────────────────────────────────────────────────

    private static void writeDefault(Path path) throws IOException {
        try (Writer w = Files.newBufferedWriter(path)) {
            w.write(DEFAULT_CONFIG);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // ユーティリティ
    // ──────────────────────────────────────────────────────────────

    private static int asInt(Object v, int def) {
        if (v instanceof Number n) return n.intValue();
        return def;
    }

    private static double asDouble(Object v, double def) {
        if (v instanceof Number n) return n.doubleValue();
        return def;
    }

    private static boolean asBool(Object v, boolean def) {
        if (v instanceof Boolean b) return b;
        return def;
    }

    private static String asStr(Object v, String def) {
        if (v != null) return String.valueOf(v);
        return def;
    }

    private ModConfig() {}

    // ──────────────────────────────────────────────────────────────
    // デフォルト設定値（doc/ConfigSpec.md のサンプルと同一）
    // ──────────────────────────────────────────────────────────────

    private static final String DEFAULT_CONFIG =
        """
        # ============================================================
        # WorldTrigger Mod - config.yml
        # ============================================================

        player:
          base_trion: 100
          move_speed: 1.5
          jump_boost: 2.0
          attack_power_multiplier: 0.5

        game:
          trion_regen: false
          critical_rate_base: 0.1
          critical_damage_multiplier: 2.0

        weapons:

          kogetsu:
            trion_equip_cost: 5
            damage: 8
            range: 3.0
            cooldown_ticks: 10
            trion_use: 5

          scorpion:
            trion_equip_cost: 4
            damage: 6
            range: 2.0
            cooldown_ticks: 8
            trion_use: 5

          raygust:
            trion_equip_cost: 6
            damage: 5
            range: 2.0
            cooldown_ticks: 12
            trion_use: 5
            damage_reduction: 0.35

          handgun:
            trion_equip_cost: 4
            damage: 5
            range: 50.0
            speed: 6.0
            cooldown_ticks: 12
            trion_use: 4

          assault_rifle:
            trion_equip_cost: 6
            damage: 5
            range: 100.0
            speed: 6.0
            cooldown_ticks: 8
            trion_use: 5

          shotgun:
            trion_equip_cost: 6
            damage: 7
            close_range_bonus: 1.5
            close_range_threshold: 5.0
            pellet_count: 6
            range: 20.0
            speed: 5.0
            cooldown_ticks: 20
            trion_use: 6

          gatling:
            trion_equip_cost: 10
            damage: 6
            range: 80.0
            speed: 6.0
            cooldown_ticks: 4
            trion_use: 10

          grenade_gun:
            trion_equip_cost: 8
            damage: 10
            splash_radius: 4.0
            slowness_duration_ticks: 60
            range: 50.0
            speed: 4.0
            cooldown_ticks: 30
            trion_use: 9

          asteroid:
            trion_equip_cost: 6
            damage: 7
            range: 40.0
            speed: 2.5
            cooldown_ticks: 15
            trion_use: 5

          meteora:
            trion_equip_cost: 7
            damage: 9
            splash_radius: 3.0
            mine_trigger_radius: 2.0
            speed: 2.5
            cooldown_ticks: 20
            trion_use: 8

          hound:
            trion_equip_cost: 6
            damage: 5
            range: 40.0
            speed: 1.5
            tracking_speed: 0.3
            slowness_duration_ticks: 40
            cooldown_ticks: 15
            trion_use: 5

          viper:
            trion_equip_cost: 8
            damage: 6
            max_nodes: 5
            node_duration_seconds: 8
            speed: 2.5
            trion_use: 7
            trion_use_per_node: 1
            cooldown_ticks: 20

          red_bullet:
            trion_equip_cost: 7
            damage: 0
            slowness_level: 3
            slowness_duration_ticks: 100
            speed: 1.5
            shield_penetrate: true
            cooldown_ticks: 20
            trion_use: 8

          eaglet:
            trion_equip_cost: 8
            damage: 8
            range: 300.0
            speed: 5.0
            cooldown_ticks: 40
            trion_use: 7
            trion_scaling: range

          lightning:
            trion_equip_cost: 7
            damage: 5
            range: 200.0
            speed: 5.0
            cooldown_ticks: 35
            trion_use: 6
            trion_scaling: speed

          ibis:
            trion_equip_cost: 10
            damage: 15
            range: 400.0
            speed: 4.0
            shield_penetrate_rate: 0.5
            cooldown_ticks: 50
            trion_use: 9
            trion_scaling: damage

          shield:
            trion_equip_cost: 3
            damage_reduction: 0.5
            trion_use: 3

          escudo:
            trion_equip_cost: 12
            catapult_velocity: 2.0
            trion_use: 10

        options:

          bagworm:
            trion_equip_cost: 3
            trion_drain_per_second: 2

          grasshopper:
            trion_equip_cost: 4
            max_count: 4
            duration_seconds: 5
            trion_use: 4
            cooldown_ticks: 10

          spider:
            trion_equip_cost: 4
            slowness_level: 1
            slowness_duration_ticks: 60
            wire_length: 20.0
            trion_use: 4

          switchbox:
            trion_equip_cost: 5
            max_placed: 3
            trion_use: 3

        composite:

          composite_round:
            trion_equip_cost: 12
            damage: 7
            range: 20.0
            speed: 2.5
            splash_radius: 3.0
            tracking_speed: 0.3
            cooldown_ticks: 20
            trion_use: 10
        """;
}
