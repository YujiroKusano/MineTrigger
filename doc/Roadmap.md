# 実装ロードマップ

WorldTrigger Mod — Implementation Roadmap v1.0

---

## フェーズ構成

```
Phase 1: コアシステム    ← まずここから
Phase 2: 基本武器
Phase 3: 応用武器
Phase 4: オプション系
Phase 5: 後回し項目
```

---

## Phase 1 — コアシステム（最優先）

ClaudeCodeに最初に実装してもらう範囲。ここが動かないと武器の実装ができない。

| タスク | 内容 | 参照ドキュメント |
|---|---|---|
| トリオンゲージ実装 | バニラの体力をトリオンゲージに置き換え | CoreSystem.md §1 |
| 基本ステータス適用 | 移動速度1.5倍・ジャンプ2倍を常時付与 | CoreSystem.md §2 |
| 入力システム | 左右クリック・シフト・Qキーの検知 | CoreSystem.md §3 |
| ロードアウト管理 | スロット制限・装備コスト計算 | CoreSystem.md §4 |
| クールダウン管理 | 武器ごとのtickカウンター | CoreSystem.md §3 |
| config.yml読み込み | 起動時にYAMLを読んでパラメータを反映 | ConfigSpec.md |
| ベイルアウト処理 | トリオンゼロで戦闘離脱 | CoreSystem.md §1 |
| HUD表示 | トリオンゲージをスクリーンに描画 | CoreSystem.md §6 |

---

## Phase 2 — 基本武器

コアシステム完成後に実装する。難易度が低い武器から順に着手する。

| 武器名 | 難易度 | 優先度 |
|---|---|---|
| 弧月 | 🟢 易 | 高 |
| レイガスト | 🟢 易 | 高 |
| アステロイド | 🟢 易 | 高 |
| シールド | 🟢 易 | 高 |
| ハンドガン | 🟢 易 | 中 |
| アサルトライフル | 🟢 易 | 中 |
| イーグレット | 🟢 易 | 中 |
| ライトニング | 🟢 易 | 中 |
| アイビス | 🟢 易 | 中 |

---

## Phase 3 — 応用武器

難易度が中程度の武器。Phase 2完了後に着手する。

| 武器名 | 難易度 | 備考 |
|---|---|---|
| スコーピオン | 🟡 中 | 形状切り替えのステート管理が必要 |
| メテオラ | 🟡 中 | 爆発処理・地雷設置の2モード |
| ハウンド | 🟡 中 | ホーミング処理が必要 |
| エスクード | 🟡 中 | ブロック設置＋カタパルト処理 |
| ショットガン | 🟡 中 | 複数弾・近距離ボーナス |
| ガトリング | 🟡 中 | 高速連射・高トリオン消費 |
| グレネードガン | 🟡 中 | 放物線軌道 |

---

## Phase 4 — オプション系・高難易度武器

| 武器名 | 難易度 | 備考 |
|---|---|---|
| バッグワーム | 🟢 易 | 透明化エフェクト＋継続消費 |
| グラスホッパー | 🟡 中 | 向き検知＋一時ブロック生成 |
| スパイダー | 🟡 中 | ワイヤーのパーティクル表現 |
| バイパー | 🟡 中 | ノード設置→経由発射のフロー |
| レッドバレット | 🟢 易 | シールド無視＋スロウネス付与 |
| スイッチボックス | 🟡 中 | 設置→遅延起動のステート管理 |

---

## Phase 5 — 後回し項目

設計は済んでいるが実装は後回しにする。

| 項目 | 理由 |
|---|---|
| 合成弾 | 設計未完了。別途仕様書を作成予定 |
| マップ設計 | コアシステム・武器が揃ってから着手 |
| ゲームルール（ランク戦） | 勝利条件等が未設計 |
| キャラクター個別トリオン量 | 全員共通で先に動作確認する |

---

## ClaudeCodeへの指示テンプレート

### Phase 1 開始時

```
以下の設計書をもとにWorldTrigger Modの実装を開始してください。

- README.md: プロジェクト概要・全体仕様
- doc/CoreSystem.md: コアシステムの詳細仕様
- doc/ConfigSpec.md: 設定ファイルの仕様
- doc/WeponSpec.md: 武器の詳細仕様
- doc/Roadmap.md: 実装優先度

まずPhase 1（コアシステム）から実装してください。
フレームワーク: Fabric / Minecraft 1.21.1 / Java 21
```

### 武器実装時

```
Phase 2の武器実装を開始してください。
コアシステムは実装済みの前提で進めてください。

最初に実装する武器: 弧月（kogetsu）
仕様: doc/WeponSpec.md の「弧月」セクションを参照
```

---

## ディレクトリ構成（想定）

```
src/
├── main/
│   ├── java/com/sigulog/minetrigger/
│   │   ├── MineTriggerMod.java          # Modエントリポイント
│   │   ├── config/
│   │   │   └── ModConfig.java           # config.yml読み込み
│   │   ├── core/
│   │   │   ├── TrionSystem.java         # トリオンゲージ管理
│   │   │   ├── LoadoutManager.java      # ロードアウト管理
│   │   │   └── CooldownManager.java     # クールダウン管理
│   │   ├── weapon/
│   │   │   ├── base/
│   │   │   │   ├── Weapon.java          # 武器の基底クラス
│   │   │   │   ├── AttackerWeapon.java  # アタッカー系基底
│   │   │   │   ├── GunnerWeapon.java    # ガンナー系基底
│   │   │   │   └── ShooterWeapon.java   # シューター系基底
│   │   │   ├── attacker/
│   │   │   │   ├── Kogetsu.java
│   │   │   │   ├── Scorpion.java
│   │   │   │   └── Raygust.java
│   │   │   ├── gunner/
│   │   │   │   ├── Handgun.java
│   │   │   │   └── AssaultRifle.java
│   │   │   ├── shooter/
│   │   │   │   ├── Asteroid.java
│   │   │   │   ├── Meteora.java
│   │   │   │   ├── Hound.java
│   │   │   │   ├── Viper.java
│   │   │   │   └── RedBullet.java
│   │   │   ├── sniper/
│   │   │   │   ├── Eaglet.java
│   │   │   │   ├── Lightning.java
│   │   │   │   └── Ibis.java
│   │   │   └── option/
│   │   │       ├── Bagworm.java
│   │   │       ├── Grasshopper.java
│   │   │       ├── Spider.java
│   │   │       └── Switchbox.java
│   │   ├── defense/
│   │   │   ├── Shield.java
│   │   │   └── Escudo.java
│   │   ├── input/
│   │   │   └── InputHandler.java        # 入力検知
│   │   └── hud/
│   │       └── TrionHud.java            # HUD描画
│   └── resources/
│       ├── fabric.mod.json
│       └── assets/minetrigger/
└── client/                              # クライアント専用コード
    └── java/com/sigulog/minetrigger/
        └── client/
            └── MineTriggerClient.java
```
