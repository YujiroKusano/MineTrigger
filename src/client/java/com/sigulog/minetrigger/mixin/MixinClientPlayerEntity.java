package com.sigulog.minetrigger.mixin;

import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 武器を持っているときの腕振りモーションを抑制する。
 *
 * ・オフハンドに武器 → 左クリック時のメインハンドスイングをキャンセル
 * ・メインハンドに武器 → 右クリック時の腕振りをキャンセル
 */
@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {

    @Inject(method = "swingHand", at = @At("HEAD"), cancellable = true)
    private void suppressWeaponSwing(Hand hand, CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (hand != Hand.MAIN_HAND) return;

        if (self.getOffHandStack().getItem() instanceof WeaponItem) {
            ci.cancel();
            return;
        }
        if (self.getMainHandStack().getItem() instanceof WeaponItem) {
            ci.cancel();
        }
    }
}
