import { system } from "@minecraft/server";

const cooldowns = new Map();

export function startCooldown(player, weaponKey, ticks) {
  let m = cooldowns.get(player.id);
  if (!m) {
    m = new Map();
    cooldowns.set(player.id, m);
  }
  m.set(weaponKey, ticks);
}

export function isOnCooldown(player, weaponKey) {
  const m = cooldowns.get(player.id);
  if (!m) return false;
  return (m.get(weaponKey) ?? 0) > 0;
}

export function registerCooldownTicker() {
  system.runInterval(() => {
    for (const [playerId, m] of cooldowns) {
      for (const [key, ticks] of m) {
        const next = ticks - 1;
        if (next <= 0) m.delete(key);
        else m.set(key, next);
      }
      if (m.size === 0) cooldowns.delete(playerId);
    }
  }, 1);
}
