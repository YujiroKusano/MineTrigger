package com.sigulog.minetrigger.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ハート（HP）・食料バーをHUDから非表示にする。
 * トリオンシステムで管理するため、バニラのステータスバーは不要。
 */
@Mixin(InGameHud.class)
public class MixinInGameHud {

    /** ハート・防具・食料・空気バーを全て非表示 */
    @Inject(method = "renderStatusBars", at = @At("HEAD"), cancellable = true)
    private void hideStatusBars(DrawContext context, CallbackInfo ci) {
        ci.cancel();
    }
}
