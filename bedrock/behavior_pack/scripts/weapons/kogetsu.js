import { EntityDamageCause } from "@minecraft/server";

function eyePosition(player) {
  return typeof player.getHeadLocation === "function"
    ? player.getHeadLocation()
    : player.location;
}

function isFacing(eye, view, target, range) {
  const targetPoint = {
    x: target.location.x,
    y: target.location.y + 0.9,
    z: target.location.z,
  };
  const toTarget = {
    x: targetPoint.x - eye.x,
    y: targetPoint.y - eye.y,
    z: targetPoint.z - eye.z,
  };
  const dist = Math.sqrt(toTarget.x ** 2 + toTarget.y ** 2 + toTarget.z ** 2);
  if (dist > range) return false;
  const dot =
    (toTarget.x * view.x + toTarget.y * view.y + toTarget.z * view.z) / (dist || 1);
  return dot > 0.5;
}

function slash(player, range, damage) {
  const eye = eyePosition(player);
  const view = player.getViewDirection();
  const nearby = player.dimension.getEntities({
    location: eye,
    maxDistance: range + 1.5,
  });
  for (const target of nearby) {
    if (target.id === player.id) continue;
    if (!target.getComponent("minecraft:health")) continue;
    if (!isFacing(eye, view, target, range)) continue;
    target.applyDamage(damage, {
      cause: EntityDamageCause.entityAttack,
      damagingEntity: player,
    });
  }
}

export function activateNormal(player, params) {
  slash(player, params.range, params.damage);
  player.onScreenDisplay.setActionBar("§e[ 弧月 ]§r 斬撃");
}

export function activateSpecial(player, params) {
  slash(player, params.range * 2, params.damage);
  player.onScreenDisplay.setActionBar("§6[ 弧月 ]§r 旋空");
}
