import { world, system, EquipmentSlot } from "@minecraft/server";
import { getWeapon } from "./weapon_registry.js";
import { isOnCooldown, startCooldown } from "./cooldown.js";
import { consumeTrion } from "./trion.js";

const handledThisTick = new Set();

/**
 * メインハンド優先、無ければオフハンドを見る。
 * 4-1 案D（本物のオフハンドスロットにカスタム武器を置く）を前提にしているが、
 * ZL/ZR押下がメインハンドとオフハンドを区別できるイベントは確認できていないため、
 * 「メインハンドが武器ならそれを発動、空ならオフハンドを見る」という暫定ルールにしている。
 * bedrock-verification/ の検証結果が出たら見直す。
 */
function resolveActiveWeapon(player) {
  const eq = player.getComponent("minecraft:equippable");
  if (!eq) return undefined;

  const main = eq.getEquipment(EquipmentSlot.Mainhand);
  const mainWeapon = getWeapon(main);
  if (mainWeapon) return mainWeapon;

  const off = eq.getEquipment(EquipmentSlot.Offhand);
  return getWeapon(off);
}

function tryFire(player, special) {
  if (handledThisTick.has(player.id)) return;
  const weapon = resolveActiveWeapon(player);
  if (!weapon) return;
  handledThisTick.add(player.id);

  if (isOnCooldown(player, weapon.key)) {
    player.playSound("note.bass", { pitch: 0.5, volume: 0.5 });
    return;
  }
  if (!consumeTrion(player, weapon.trionUse)) {
    player.playSound("note.bass", { pitch: 0.3, volume: 0.5 });
    return;
  }

  if (special) weapon.activateSpecial(player, weapon);
  else weapon.activateNormal(player, weapon);

  startCooldown(player, weapon.key, weapon.cooldownTicks);
}

export function registerInputHandler() {
  // 武器を持っている間だけバニラ動作を横取りする。
  // 武器以外（食べ物・空手等）は素通しし、チェスト/ドア等の操作を妨げない（5-2節）。
  world.beforeEvents.itemUse.subscribe((ev) => {
    if (resolveActiveWeapon(ev.source)) ev.cancel = true;
  });
  world.beforeEvents.playerInteractWithBlock.subscribe((ev) => {
    if (resolveActiveWeapon(ev.player)) ev.cancel = true;
  });
  world.beforeEvents.playerInteractWithEntity.subscribe((ev) => {
    if (resolveActiveWeapon(ev.player)) ev.cancel = true;
  });

  world.afterEvents.itemUse.subscribe((ev) => tryFire(ev.source, ev.source.isSneaking));
  world.afterEvents.playerInteractWithBlock.subscribe((ev) =>
    tryFire(ev.player, ev.player.isSneaking)
  );
  world.afterEvents.playerInteractWithEntity.subscribe((ev) =>
    tryFire(ev.player, ev.player.isSneaking)
  );

  system.runInterval(() => {
    handledThisTick.clear();
  }, 1);
}
