package com.sigulog.minetrigger.core;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.util.ActionResult;

/**
 * プレイヤーによる通常採掘を無効化する。
 *
 * - AttackBlockCallback  : ブロックへの最初の一撃をキャンセルしてクラックアニメーションを抑制
 * - PlayerBlockBreakEvents.BEFORE : 万一アニメーションが進んでも実際の破壊を防ぐ
 *
 * 武器効果（BulletManager.destroyBlocks など）は world.setBlockState() を直接呼ぶため
 * どちらのイベントも経由せず、影響を受けない。
 * クリエイティブモードのプレイヤーは除外する。
 */
public final class BlockInteractionSystem {

    public static void register() {
        // ブロックへの一撃をキャンセル → クラックアニメーション発生を防ぐ
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (player.isCreative()) return ActionResult.PASS;
            return ActionResult.FAIL;
        });

        // 採掘完了によるブロック破壊を防ぐ（二重保護）
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
            player.isCreative());
    }

    private BlockInteractionSystem() {}
}
