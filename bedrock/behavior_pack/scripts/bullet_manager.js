import { system, EntityDamageCause } from "@minecraft/server";

const bullets = [];

export function fireBullet(player, start, direction, opts) {
  const entity = player.dimension.spawnEntity("minetrigger:bullet", start);
  const unitsPerTick = opts.speed * 0.1;
  bullets.push({
    entity,
    velocity: {
      x: direction.x * unitsPerTick,
      y: direction.y * unitsPerTick,
      z: direction.z * unitsPerTick,
    },
    gravity: opts.gravity ?? 0,
    remainingRange: opts.range,
    damage: opts.damage,
    shooterId: player.id,
    shooter: player,
  });
}

function findHit(bullet, at) {
  const nearby = bullet.entity.dimension.getEntities({
    location: at,
    maxDistance: 1.0,
  });
  for (const e of nearby) {
    if (e.id === bullet.shooterId) continue;
    if (e.typeId === "minetrigger:bullet") continue;
    if (!e.getComponent("minecraft:health")) continue;
    return e;
  }
  return undefined;
}

function tickBullet(bullet) {
  if (!bullet.entity.isValid()) return true;

  bullet.velocity.y -= bullet.gravity;

  const from = bullet.entity.location;
  const to = {
    x: from.x + bullet.velocity.x,
    y: from.y + bullet.velocity.y,
    z: from.z + bullet.velocity.z,
  };

  const stepDist = Math.sqrt(
    bullet.velocity.x ** 2 + bullet.velocity.y ** 2 + bullet.velocity.z ** 2
  );
  bullet.remainingRange -= stepDist;

  const hit = findHit(bullet, to);
  if (hit) {
    hit.applyDamage(bullet.damage, {
      cause: EntityDamageCause.entityAttack,
      damagingEntity: bullet.shooter,
    });
    bullet.entity.remove();
    return true;
  }

  if (bullet.remainingRange <= 0) {
    bullet.entity.remove();
    return true;
  }

  bullet.entity.teleport(to);
  return false;
}

export function registerBulletManager() {
  system.runInterval(() => {
    for (let i = bullets.length - 1; i >= 0; i--) {
      if (tickBullet(bullets[i])) bullets.splice(i, 1);
    }
  }, 1);
}
