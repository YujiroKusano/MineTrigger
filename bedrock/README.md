# MineTrigger Bedrock Edition — 実装 第一弾

`doc/BedrockControlSpec.md` での検討を踏まえた最初の実装です。全武器の移植ではなく、**土台（トリオンシステム・武器フレームワーク・入力処理）と代表武器2つ**（ハンドガン＝射撃、弧月＝近接）で設計が実際に成立するかを確認するためのものです。

## 含まれるもの

- `behavior_pack/scripts/trion.js` — トリオンシステム。バニラダメージをHP回復→トリオン減算に変換、自動回復、ベイルアウト
- `behavior_pack/scripts/cooldown.js` — Java版 `CooldownManager` 相当
- `behavior_pack/scripts/input.js` — `itemUse`/`playerInteractWithBlock`/`playerInteractWithEntity` の3イベントをフックし、武器を持っている時だけバニラ動作をキャンセルして武器発動へ差し替える（`doc/BedrockControlSpec.md` 5節の分岐ルールに対応）
- `behavior_pack/scripts/bullet_manager.js` — Java版 `BulletManager` 相当。弾をエンティティとしてスポーンし、毎tickスクリプト側で座標を更新して命中判定する完全自前実装
- `behavior_pack/scripts/weapons/handgun.js`, `kogetsu.js` — README記載の数値（ハンドガン: damage6/range30/speed2.0/cd8tick/trion3）とKogetsuItem.javaのロジックを移植
- `behavior_pack/items/handgun.json`, `kogetsu.json` — `minecraft:allow_off_hand: true` を付与し、本物のオフハンドスロットに置ける状態にしてある

## 今回まだ実装していないもの（意図的にスコープ外）

- ハンドガン・弧月以外の18武器
- リソースパック（アイテムテクスチャ・カスタムモーション）。今はデフォルトの見た目で動くだけ
- ロードアウト画面・オプショントリガー・シールド・パッシブスキル・HUD全般
- 移動速度/ジャンプ力の属性変更、食料満タン維持（トリオン本体のロジックのみ実装。周辺ステータスは後回し）

## 未確定の設計判断（doc/BedrockControlSpec.md 4-1 に対応）

`input.js` の `resolveActiveWeapon()` は「メインハンドに武器があればそれを優先、無ければオフハンドを見る」という**暫定ルール**です。ZL/ZR等の押下がメインハンドとオフハンドを実際に区別して検知できるかは`bedrock-verification/`での検証待ちのため、現状は**同時に2丁を独立して撃ち分けることはできません**（メインハンドが空でなければオフハンドは反応しない）。検証結果が出たらここを更新します。

## コードの検証状況について

Script APIの一次資料（Microsoft公式ドキュメント）を当たりながら書きましたが、以下は**実機で未検証**です。動かなかった場合はまずここを疑ってください。

- `EntityHealthComponent` の `effectiveMax`/`setCurrentValue` のプロパティ名
- `Entity.applyDamage()` の第2引数の形（`cause`/`damagingEntity`）
- `Player.getHeadLocation()` の有無（無ければ `player.location` にフォールバックする作りにはしてある）
- `EntityDamageSource.cause` が `"fall"` という文字列で一致するか

## 導入方法

1. `bedrock/behavior_pack/` をVPS上のBDSの `behavior_packs/` にコピー
2. ワールドの `world_behavior_packs.json` にUUID（`manifest.json`参照）を追加
3. サーバー起動、`/give @s minetrigger:handgun` `/give @s minetrigger:kogetsu` で入手
4. ZL（アイテム使用）で通常技、スニーク+ZLで特殊技

## 次にやること

1. 上記「未検証」項目を実機で確認し、動かない箇所を修正
2. `bedrock-verification/` の検証結果を`input.js`に反映（メイン/オフハンド独立発動の可否）
3. 問題なければ残り18武器を同じフレームワークに追加していく
