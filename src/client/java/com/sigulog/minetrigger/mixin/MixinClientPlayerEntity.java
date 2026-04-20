package com.sigulog.minetrigger.mixin;

import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 武器を持っているときの腕振りモーションを抑制する。
 *
 * 修正: キャンセル後も HandSwingC2SPacket を手動送信することで、
 * 空中での左クリック（ミス）でも正しく武器が発動するようにする。
 */
@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {

    @Inject(method = "swingHand", at = @At("HEAD"), cancellable = true)
    private void suppressWeaponSwing(Hand hand, CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        if (hand != Hand.MAIN_HAND) return;

        boolean hasOffHandWeapon = self.getOffHandStack().getItem() instanceof WeaponItem;
        boolean hasMainHandWeapon = self.getMainHandStack().getItem() instanceof WeaponItem;

        if (!hasOffHandWeapon && !hasMainHandWeapon) return;

        // アニメーションを抑制するが、パケットは手動送信してサーバーに知らせる
        ci.cancel();
        var netHandler = MinecraftClient.getInstance().getNetworkHandler();
        if (netHandler != null) {
            netHandler.sendPacket(new HandSwingC2SPacket(hand));
        }
    }
}
