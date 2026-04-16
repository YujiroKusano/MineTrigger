package com.sigulog.minetrigger.mixin;

import com.sigulog.minetrigger.input.InputHandler;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 左クリック入力を横取りして左手武器を発動し、ブロック破壊を無効化する。
 *
 * ・HandSwingC2SPacket(MAIN_HAND)      → 空振り時の左手武器発動
 * ・PlayerActionC2SPacket(START/STOP/ABORT_DESTROY_BLOCK)
 *                                      → ブロック破壊を完全キャンセル
 */
@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "onHandSwing", at = @At("HEAD"))
    private void onHandSwing(HandSwingC2SPacket packet, CallbackInfo ci) {
        if (packet.getHand() != Hand.MAIN_HAND) return;
        com.sigulog.minetrigger.MineTriggerMod.LOGGER.info("[MineTrigger] onHandSwing MAIN_HAND handled={} rightClick={}",
            InputHandler.isHandledThisTick(player.getUuid()),
            InputHandler.isRightClickHandledThisTick(player.getUuid()));

        // AttackEntityCallback（左クリック攻撃）で処理済みなら二重発火しない
        if (InputHandler.isHandledThisTick(player.getUuid())) return;
        // 右クリック処理済みのtickは HandSwing が右クリックのスイングなのでスキップ
        if (InputHandler.isRightClickHandledThisTick(player.getUuid())) return;

        InputHandler.fireLeftClick(player);
    }

    /**
     * ブロック破壊を完全無効化。
     * START / STOP / ABORT_DESTROY_BLOCK をすべてキャンセルする。
     */
    @Inject(method = "onPlayerAction", at = @At("HEAD"), cancellable = true)
    private void onPlayerAction(PlayerActionC2SPacket packet, CallbackInfo ci) {
        switch (packet.getAction()) {
            case START_DESTROY_BLOCK,
                 STOP_DESTROY_BLOCK,
                 ABORT_DESTROY_BLOCK -> ci.cancel();
            default -> { /* ドロップ等は通す */ }
        }
    }
}
