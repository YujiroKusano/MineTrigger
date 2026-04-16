package com.sigulog.minetrigger.config;

/**
 * 武器1つ分のパラメータ。
 * config.yml の weapons.&lt;key&gt; セクションを保持する。
 */
public class WeaponParams {

    /** 装備コスト（装備しているだけで実効トリオン総量から差し引かれる） */
    public int trionEquipCost = 5;

    /** 基礎ダメージ（攻撃力倍率が掛かる前の値） */
    public int damage = 5;

    /** 射程距離（ブロック単位） */
    public double range = 3.0;

    /** クールダウン（tick単位。20tick = 1秒） */
    public int cooldownTicks = 10;

    /** 1回の発動で消費するトリオン量 */
    public int trionUse = 5;

    // ── 武器固有フィールド（該当しない武器ではデフォルト値のまま） ──

    /** スコーピオン: 現在の形状インデックス（0=ナイフ, 1=長剣, 2=二刀, 3=槍） */
    public int scorpionForm = 0;

    /** ショットガン: 散弾数 */
    public int pelletCount = 6;

    /** ショットガン: 近距離ボーナス倍率 */
    public double closeRangeBonus = 1.5;

    /** ショットガン: 近距離判定距離（ブロック） */
    public double closeRangeThreshold = 5.0;

    /** スロウネス付与時間（tick） */
    public int slownessDurationTicks = 40;

    /** スロウネスレベル */
    public int slownessLevel = 1;

    /** 爆発範囲（ブロック） */
    public double splashRadius = 3.0;

    /** 地雷起爆範囲（ブロック） */
    public double mineTriggerRadius = 2.0;

    /** シールド貫通フラグ */
    public boolean shieldPenetrate = false;

    /** シールド貫通率（0.0〜1.0） */
    public double shieldPenetrateRate = 0.0;

    /** 弾速（通常弾比） */
    public double speed = 1.0;

    /** ホーミング速度 */
    public double trackingSpeed = 0.3;

    /** バイパー: ノード最大数 */
    public int maxNodes = 5;

    /** バイパー: ノード持続時間（秒） */
    public int nodeDurationSeconds = 8;

    /** バイパー: ノード1つあたりの追加消費 */
    public int trionUsePerNode = 1;

    /** スナイパー: トリオン依存強化項目（"range" / "speed" / "damage"） */
    public String trionScaling = "";

    /** レイガスト: ダメージ軽減率 */
    public double damageReduction = 0.0;

    /** グラスホッパー: 同時設置上限 */
    public int maxCount = 4;

    /** グラスホッパー: 足場持続秒数 */
    public int durationSeconds = 5;

    /** スパイダー: ワイヤー最大長 */
    public double wireLength = 20.0;

    /** スイッチボックス: 同時設置上限 */
    public int maxPlaced = 3;

    /** エスクード: カタパルト時の上方向速度 */
    public double catapultVelocity = 2.0;

    /** バッグワーム: 毎秒消費トリオン */
    public int trionDrainPerSecond = 2;
}
