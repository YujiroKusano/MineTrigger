import { world, system } from "@minecraft/server";

const BASE_TRION = 500;
const TRION_REGEN_PER_SECOND = 10;
const TRION_REGEN_ENABLED = true;

const KEY_CURRENT = "minetrigger:trion_current";
const KEY_MAX = "minetrigger:trion_max";

const bailingOut = new Set();

export function getTrion(player) {
  const v = player.getDynamicProperty(KEY_CURRENT);
  return typeof v === "number" ? v : BASE_TRION;
}

export function getMaxTrion(player) {
  const v = player.getDynamicProperty(KEY_MAX);
  return typeof v === "number" ? v : BASE_TRION;
}

export function setTrion(player, value) {
  const max = getMaxTrion(player);
  const clamped = Math.max(0, Math.min(value, max));
  player.setDynamicProperty(KEY_CURRENT, clamped);
  if (clamped <= 0) bailout(player);
  return clamped;
}

export function consumeTrion(player, amount) {
  const current = getTrion(player);
  if (current < amount) return false;
  setTrion(player, current - amount);
  return true;
}

function bailout(player) {
  if (bailingOut.has(player.id)) return;
  bailingOut.add(player.id);
  try {
    player.onScreenDisplay.setActionBar("§c[ ベイルアウト ]§r トリオンが尽きました。");
    player.kill();
  } finally {
    bailingOut.delete(player.id);
  }
}

export function initPlayer(player) {
  player.setDynamicProperty(KEY_MAX, BASE_TRION);
  player.setDynamicProperty(KEY_CURRENT, BASE_TRION);
  const health = player.getComponent("minecraft:health");
  if (health) health.setCurrentValue(health.effectiveMax);
}

export function registerTrionSystem() {
  world.afterEvents.playerSpawn.subscribe((ev) => {
    if (ev.initialSpawn) initPlayer(ev.player);
  });

  // バニラダメージをトリオンダメージへ変換する。
  // Bedrockにはダメージをキャンセルできる before イベントが無いため、
  // 食らった直後にHPを戻して差分をトリオンから引く方式で代替する。
  world.afterEvents.entityHurt.subscribe((ev) => {
    const { hurtEntity, damage, damageSource } = ev;
    if (hurtEntity.typeId !== "minecraft:player") return;
    const player = hurtEntity;
    if (bailingOut.has(player.id)) return;
    if (getTrion(player) <= 0) return;

    const health = player.getComponent("minecraft:health");
    if (health) health.setCurrentValue(health.effectiveMax);

    if (damageSource.cause === "fall") return;

    setTrion(player, getTrion(player) - damage);
  });

  system.runInterval(() => {
    for (const player of world.getAllPlayers()) {
      const health = player.getComponent("minecraft:health");
      if (health && health.currentValue < health.effectiveMax) {
        health.setCurrentValue(health.effectiveMax);
      }
      if (TRION_REGEN_ENABLED) {
        const current = getTrion(player);
        const max = getMaxTrion(player);
        if (current < max) {
          setTrion(player, Math.min(current + TRION_REGEN_PER_SECOND, max));
        }
      }
    }
  }, 20);
}
