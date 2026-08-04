import { world, EquipmentSlot } from "@minecraft/server";

const TAG = "§e[MT-Verify]§r";

function log(player, msg) {
  player.sendMessage(`${TAG} ${msg}`);
}

/**
 * 検証1（4-1 案D）：オフハンドに置いたカスタム武器を、ZL/ZR等の「アイテム使用」系
 * イベントがメインハンドと区別して拾えるか確認する。
 * 各イベントが発火するたびに、その時点のメインハンド/オフハンドの中身を突き合わせて表示する。
 */
function reportHand(player, itemStack, eventName) {
  const itemId = itemStack?.typeId ?? "(itemStack undefined)";
  const eq = player.getComponent("minecraft:equippable");
  if (!eq) {
    log(player, `${eventName}: item=${itemId} (equippable component取得失敗)`);
    return;
  }
  const main = eq.getEquipment(EquipmentSlot.Mainhand);
  const off = eq.getEquipment(EquipmentSlot.Offhand);
  log(
    player,
    `${eventName}: item=${itemId} | mainhand=${main?.typeId ?? "empty"} offhand=${off?.typeId ?? "empty"}`
  );
}

world.afterEvents.itemUse.subscribe((ev) => {
  reportHand(ev.source, ev.itemStack, "itemUse");
});

world.afterEvents.itemUseOn.subscribe((ev) => {
  reportHand(ev.source, ev.itemStack, "itemUseOn");
});

world.afterEvents.itemStartUseOn.subscribe((ev) => {
  reportHand(ev.source, ev.itemStack, "itemStartUseOn");
});

world.afterEvents.playerInteractWithEntity.subscribe((ev) => {
  reportHand(ev.player, ev.itemStack, "playerInteractWithEntity");
});

world.afterEvents.entityHitEntity.subscribe((ev) => {
  const damager = ev.damagingEntity;
  if (damager?.typeId === "minecraft:player") {
    log(damager, "entityHitEntity: 攻撃(ZR想定)が敵に命中");
  }
});

world.afterEvents.entityHitBlock.subscribe((ev) => {
  const damager = ev.damagingEntity;
  if (damager?.typeId === "minecraft:player") {
    log(damager, "entityHitBlock: 攻撃(ZR想定)がブロックに命中");
  }
});

/**
 * 検証2（4-4）：L/Rボタン（ホットバーのカーソル移動）を、特殊技ボタンとして
 * 間接検知できるかを確認する。押した瞬間からイベント発火までの体感ラグと、
 * 選択スロットが実際どう動くかをログで見る。
 */
world.afterEvents.playerHotbarSelectedSlotChange.subscribe((ev) => {
  log(
    ev.player,
    `hotbar変化: ${ev.previousSlotSelected} -> ${ev.newSlotSelected} (item=${ev.itemStack?.typeId ?? "empty"})`
  );
});

world.afterEvents.playerSpawn.subscribe((ev) => {
  if (!ev.initialSpawn) return;
  const player = ev.player;
  log(
    player,
    "検証パック読み込み完了。/give @s minetrigger_test:offhand_weapon でテスト武器を入手し、doc/BedrockControlSpec.md 記載の手順で検証してください。"
  );
});
