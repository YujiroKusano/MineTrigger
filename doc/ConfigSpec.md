# 設定ファイル仕様書

WorldTrigger Mod — Config Specification v1.0

---

## 概要

全パラメータは `config/worldtrigger/config.yml` で管理する。
Mod起動時に自動生成され、変更後はサーバー再起動で反映される。

---

## config.yml 全体サンプル

```yaml
# ============================================================
# WorldTrigger Mod - config.yml
# ============================================================

# プレイヤー基本ステータス
player:
  base_trion: 100               # 基礎トリオン量
  move_speed: 1.5               # 移動速度倍率（バニラ比）
  jump_boost: 2.0               # ジャンプ力倍率（バニラ比）
  attack_power_multiplier: 0.5  # トリオン→攻撃力の変換係数

# ゲームルール
game:
  trion_regen: false            # 戦闘中のトリオン回復（falseで回復なし）
  critical_rate_base: 0.1       # 基礎クリティカル率（0.0〜1.0）
  critical_damage_multiplier: 2.0  # クリティカル時のダメージ倍率

# 武器パラメータ
weapons:

  # ── アタッカー系 ──
  kogetsu:                      # 弧月
    trion_equip_cost: 5
    damage: 8
    range: 3.0
    cooldown_ticks: 10

  scorpion:                     # スコーピオン（形状共通の装備コスト）
    trion_equip_cost: 4
    forms:
      knife:
        damage: 6
        range: 2.0
        cooldown_ticks: 8
        trion_use: 5
      longsword:
        damage: 6
        range: 3.0
        cooldown_ticks: 10
        trion_use: 6
      dual:
        damage: 8
        range: 2.0
        cooldown_ticks: 12
        trion_use: 8
      spear:
        damage: 6
        range: 3.0
        cooldown_ticks: 12
        trion_use: 5

  raygust:                      # レイガスト
    trion_equip_cost: 6
    sword:
      damage: 5
      range: 2.0
      cooldown_ticks: 12
      trion_use: 5
    shield:
      damage: 0
      damage_reduction: 0.6     # 受けるダメージを60%軽減
      trion_use: 4

  # ── ガンナー系 ──
  handgun:                      # ハンドガン
    trion_equip_cost: 4
    damage: 5
    range: 20.0
    cooldown_ticks: 12
    trion_use: 4

  assault_rifle:                # アサルトライフル
    trion_equip_cost: 6
    damage: 5
    range: 20.0
    cooldown_ticks: 8
    trion_use: 5

  shotgun:                      # ショットガン
    trion_equip_cost: 6
    damage: 7
    close_range_bonus: 1.5      # 近距離時のダメージ倍率
    close_range_threshold: 5.0  # 近距離判定の距離（ブロック）
    pellet_count: 6             # 散弾の数
    range: 8.0
    cooldown_ticks: 20
    trion_use: 6

  gatling:                      # ガトリング
    trion_equip_cost: 10
    damage: 6
    range: 20.0
    cooldown_ticks: 4
    trion_use: 10

  grenade_gun:                  # グレネードガン
    trion_equip_cost: 8
    damage: 10
    splash_radius: 4.0          # 爆発範囲（ブロック）
    slowness_duration_ticks: 60 # スロウネス付与時間
    range: 15.0
    cooldown_ticks: 30
    trion_use: 9

  # ── シューター系 ──
  asteroid:                     # アステロイド
    trion_equip_cost: 6
    damage: 7
    range: 20.0
    cooldown_ticks: 15
    trion_use: 5

  meteora:                      # メテオラ
    trion_equip_cost: 7
    damage: 9
    splash_radius: 3.0
    mine_trigger_radius: 2.0    # 地雷の起爆範囲
    cooldown_ticks: 20
    trion_use: 8

  hound:                        # ハウンド
    trion_equip_cost: 6
    damage: 5
    range: 20.0
    tracking_speed: 0.3         # 追尾速度
    slowness_duration_ticks: 40
    cooldown_ticks: 15
    trion_use: 5

  viper:                        # バイパー
    trion_equip_cost: 8
    damage: 6
    max_nodes: 5                # ノード最大数
    node_duration_seconds: 8    # ノードの持続時間
    trion_use_base: 7           # 基本消費
    trion_use_per_node: 1       # ノード1つあたりの追加消費
    cooldown_ticks: 20

  red_bullet:                   # レッドバレット（鉛弾）
    trion_equip_cost: 7
    damage: 0                   # ダメージなし
    slowness_level: 3           # スロウネスのレベル（1〜6）
    slowness_duration_ticks: 100
    speed: 0.5                  # 弾速（通常弾比）
    shield_penetrate: true      # シールド貫通
    cooldown_ticks: 20
    trion_use: 8

  # ── スナイパー系 ──
  eaglet:                       # イーグレット
    trion_equip_cost: 8
    damage: 8
    range: 80.0
    cooldown_ticks: 40
    trion_use: 7
    trion_scaling: range        # トリオン量で強化される項目

  lightning:                    # ライトニング
    trion_equip_cost: 7
    damage: 5
    range: 60.0
    speed: 5.0                  # 弾速
    cooldown_ticks: 35
    trion_use: 6
    trion_scaling: speed

  ibis:                         # アイビス
    trion_equip_cost: 10
    damage: 15
    range: 60.0
    shield_penetrate_rate: 0.5  # シールド貫通率（0.0〜1.0）
    cooldown_ticks: 50
    trion_use: 9
    trion_scaling: damage

  # ── 防御系 ──
  shield:                       # シールド
    trion_equip_cost: 3
    damage_reduction: 0.5       # 通常時の被ダメ軽減率
    focused_damage_reduction: 0.85  # 集中時の被ダメ軽減率
    trion_use_per_hit: 3        # 被弾時のトリオン消費

  escudo:                       # エスクード
    trion_equip_cost: 12
    block_material: "iron_block" # 設置されるブロック種別
    catapult_velocity: 2.0      # カタパルト時の上方向速度
    trion_use: 10               # 設置1回あたりの消費

# オプショントリガーパラメータ
options:

  bagworm:                      # バッグワーム
    trion_equip_cost: 3
    trion_drain_per_second: 2   # 毎秒のトリオン消費

  grasshopper:                  # グラスホッパー
    trion_equip_cost: 4
    max_count: 4                # 同時設置上限
    duration_seconds: 5         # 足場の持続時間
    place_distance: 1           # 設置距離（ブロック）
    trion_use: 4                # 1回の設置コスト
    cooldown_ticks: 10

  spider:                       # スパイダー
    trion_equip_cost: 4
    slowness_level: 1
    slowness_duration_ticks: 60
    wire_length: 20.0           # ワイヤーの最大長
    trion_use: 4

  switchbox:                    # スイッチボックス
    trion_equip_cost: 5
    max_placed: 3               # 同時設置上限
    trion_use_place: 3          # 設置時消費
    trion_use_trigger: 2        # 起動時消費
```

---

## パラメータ詳細

### player

| キー | 型 | 説明 |
|---|---|---|
| base_trion | int | 基礎トリオン量。装備コストを引く前の最大値 |
| move_speed | double | バニラの移動速度に対する倍率 |
| jump_boost | double | バニラのジャンプ力に対する倍率 |
| attack_power_multiplier | double | 実効トリオン総量をこの値で掛けたものが攻撃力倍率になる |

### game

| キー | 型 | 説明 |
|---|---|---|
| trion_regen | boolean | trueにすると戦闘中もトリオンが回復する（デバッグ用途） |
| critical_rate_base | double | 基礎クリティカル率。0.1 = 10% |
| critical_damage_multiplier | double | クリティカル時のダメージ倍率 |

### weapons共通

| キー | 型 | 説明 |
|---|---|---|
| trion_equip_cost | int | 装備しているだけで実効トリオン総量から差し引かれる値 |
| damage | int | 基礎ダメージ（攻撃力倍率が掛かる前の値） |
| range | double | 射程距離（ブロック単位） |
| cooldown_ticks | int | 次の発動まで待つtick数（20tick = 1秒） |
| trion_use | int | 1回の発動で消費するトリオン量 |

### trion_scaling（スナイパー系専用）

| 値 | 効果 |
|---|---|
| range | トリオン量が多いほど射程が伸びる |
| speed | トリオン量が多いほど弾速が上がる |
| damage | トリオン量が多いほどダメージが上がる |

---

## 読み込みタイミング

1. Modの初期化時（サーバー起動時）に読み込む
2. ファイルが存在しない場合はデフォルト値でファイルを生成する
3. `/worldtrigger reload` コマンドでランタイム中に再読み込み可能にする（将来対応）
