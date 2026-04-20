package com.sigulog.minetrigger.mixin;

import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * オプション系WeaponItem をプレイヤーの通常インベントリ（ホットバー・メイン）に
 * 入れられないようにする。オプション武器はオプションスロット専用。
 */
@Mixin(PlayerInventory.class)
public class MixinPlayerInventory {

    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void blockOptionWeapons(int slot, ItemStack stack,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!stack.isEmpty()
                && stack.getItem() instanceof WeaponItem wi
                && wi.getWeaponType().isOption()) {
            cir.setReturnValue(false);
        }
    }
}
