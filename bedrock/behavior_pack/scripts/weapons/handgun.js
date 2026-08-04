import { fireBullet } from "../bullet_manager.js";

function eyePosition(player) {
  return typeof player.getHeadLocation === "function"
    ? player.getHeadLocation()
    : player.location;
}

export function activateNormal(player, params) {
  fireBullet(player, eyePosition(player), player.getViewDirection(), {
    speed: params.speed,
    range: params.range,
    damage: params.damage,
    gravity: 0.004,
  });
  player.onScreenDisplay.setActionBar("§e[ ハンドガン ]§r 発射");
}

export function activateSpecial(player, params) {
  const eye = eyePosition(player);
  const view = player.getViewDirection();
  for (let i = 0; i < 3; i++) {
    fireBullet(player, eye, view, {
      speed: params.speed * 1.2,
      range: params.range,
      damage: params.damage * 0.7,
      gravity: 0.004,
    });
  }
  player.onScreenDisplay.setActionBar("§6[ ハンドガン ]§r 3連射");
}
