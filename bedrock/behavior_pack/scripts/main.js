import { registerTrionSystem } from "./trion.js";
import { registerCooldownTicker } from "./cooldown.js";
import { registerBulletManager } from "./bullet_manager.js";
import { registerInputHandler } from "./input.js";

registerTrionSystem();
registerCooldownTicker();
registerBulletManager();
registerInputHandler();
