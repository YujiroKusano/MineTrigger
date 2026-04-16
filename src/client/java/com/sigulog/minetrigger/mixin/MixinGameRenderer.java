package com.sigulog.minetrigger.mixin;

import com.sigulog.minetrigger.client.ScopeHandler;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * スコープ中の FOV を縮小する。
 * GameRenderer#getFov の戻り値に zoomFactor を乗算する。
 */
@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void applyScope(Camera camera, float tickDelta, boolean changingFov,
                             CallbackInfoReturnable<Double> cir) {
        float zoom = ScopeHandler.zoomFactor();
        if (zoom < 0.999f) {
            cir.setReturnValue(cir.getReturnValue() * zoom);
        }
    }
}
