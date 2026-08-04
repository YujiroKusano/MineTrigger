import * as handgun from "./weapons/handgun.js";
import * as kogetsu from "./weapons/kogetsu.js";

export const WEAPONS = {
  "minetrigger:handgun": {
    key: "handgun",
    damage: 6,
    range: 30,
    speed: 2.0,
    cooldownTicks: 8,
    trionUse: 3,
    activateNormal: handgun.activateNormal,
    activateSpecial: handgun.activateSpecial,
  },
  "minetrigger:kogetsu": {
    key: "kogetsu",
    damage: 5,
    range: 3.5,
    cooldownTicks: 10,
    trionUse: 5,
    activateNormal: kogetsu.activateNormal,
    activateSpecial: kogetsu.activateSpecial,
  },
};

export function getWeapon(itemStack) {
  return itemStack ? WEAPONS[itemStack.typeId] : undefined;
}
