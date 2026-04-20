package com.sigulog.minetrigger.core;

import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

/**
 * ガンナー/スナイパー弾薬専用の 3 スロットインベントリ。
 *
 * slot 0, 1 : ガンナー弾薬（ASTEROID / METEORA / VIPER / HOUND のみ）
 * slot 2    : スナイパー弾薬（RED_BULLET のみ）
 */
public class GunnerAmmoInventory extends SimpleInventory {

    public static final int SIZE       = 3;
    public static final int GUNNER_SLOT_0 = 0;
    public static final int GUNNER_SLOT_1 = 1;
    public static final int SNIPER_SLOT_0 = 2;

    public GunnerAmmoInventory() {
        super(SIZE);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (!(stack.getItem() instanceof WeaponItem wi)) return false;
        if (slot < 2) return wi.getWeaponType().isGunnerAmmo();
        return wi.getWeaponType().isSniperAmmo();
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }
}
