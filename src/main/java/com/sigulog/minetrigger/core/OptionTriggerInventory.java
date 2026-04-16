package com.sigulog.minetrigger.core;

import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;

/**
 * オプショントリガー専用の 6 スロットインベントリ。
 * オプション系 WeaponItem 以外は受け付けない。
 */
public class OptionTriggerInventory extends SimpleInventory {

    public static final int SIZE = 6;

    public OptionTriggerInventory() {
        super(SIZE);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        if (stack.isEmpty()) return true;
        return stack.getItem() instanceof WeaponItem wi && wi.getWeaponType().isOption();
    }

    @Override
    public int getMaxCountPerStack() {
        return 1;
    }
}
