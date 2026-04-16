package com.sigulog.minetrigger.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * インベントリ画面にオプショントリガーパネルの背景・タイトル・番号ラベルを描画する。
 * スロット自体は MixinPlayerScreenHandler が PlayerScreenHandler に追加した
 * 本物の Slot として描画・操作される。
 *
 * スロット座標 (MixinPlayerScreenHandler と一致):
 *   slot.x = -28  (背景左端から左へ 28px)
 *   slot.y = 26 + i * 20  (縦 6 枠)
 */
@Mixin(InventoryScreen.class)
public abstract class MixinInventoryScreen extends HandledScreen<PlayerScreenHandler> {

    protected MixinInventoryScreen(PlayerScreenHandler h, PlayerInventory inv, Text t) {
        super(h, inv, t);
    }

    private int bgX() { return (this.width  - 176) / 2; }
    private int bgY() { return (this.height - 166) / 2; }

    // ── レイアウト定数 ───────────────────────────────────────────
    // スロット基準: slot.x = -28, slot.y = 26 + i*20  (slot size = 18)
    private static final int SLOT_X_REL  = -28;   // bgX からのスロット左端
    private static final int SLOT_Y0_REL = 26;    // bgY からの最初のスロット上端
    private static final int SLOT_STEP   = 20;    // スロット間隔 (18 + gap 2)
    private static final int SLOT_COUNT  = 6;

    // パネル padding
    private static final int PAD_X = 4;   // スロット左右の余白
    private static final int PAD_TOP = 4; // スロット上の余白
    private static final int PAD_BOT = 4; // スロット下の余白

    // パネル寸法
    private static final int PANEL_W = 18 + PAD_X * 2;  // = 26
    private static final int PANEL_H = SLOT_COUNT * 18 + (SLOT_COUNT - 1) * 2 + PAD_TOP + PAD_BOT; // = 118

    // パネル左上 (bgX相対)
    private static final int PANEL_DX = SLOT_X_REL - PAD_X;          // = -32
    private static final int PANEL_DY = SLOT_Y0_REL - PAD_TOP;       // = 22

    // ── 色 ───────────────────────────────────────────────────────
    private static final int C_PANEL_BG = 0xCC0A0A14;
    private static final int C_ACCENT   = 0xFF44AAFF;
    private static final int C_KEY      = 0xFFFFDD44;

    // ────────────────────────────────────────────────────────────
    // パネル背景をインベントリテクスチャの直後に描画
    // ────────────────────────────────────────────────────────────

    @Inject(method = "drawBackground", at = @At("RETURN"))
    private void drawTriggerPanelBg(DrawContext ctx, float delta,
                                    int mouseX, int mouseY, CallbackInfo ci) {
        int px = bgX() + PANEL_DX;
        int py = bgY() + PANEL_DY;

        // パネル背景
        ctx.fill(px, py, px + PANEL_W, py + PANEL_H, C_PANEL_BG);
        // 右ライン (背景との境界)
        ctx.fill(px + PANEL_W, py - 1, px + PANEL_W + 1, py + PANEL_H + 1, C_ACCENT);
    }

    // ────────────────────────────────────────────────────────────
    // スロット番号ラベルをスロット描画の上に重ねて描画
    // ────────────────────────────────────────────────────────────

    @Inject(method = "render", at = @At("RETURN"))
    private void drawTriggerPanelLabels(DrawContext ctx, int mouseX, int mouseY,
                                        float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        for (int i = 0; i < SLOT_COUNT; i++) {
            int sx = bgX() + SLOT_X_REL;
            int sy = bgY() + SLOT_Y0_REL + i * SLOT_STEP;
            // スロット右下に番号
            ctx.drawText(mc.textRenderer,
                Text.literal(String.valueOf(i + 1)),
                sx + 10, sy + 10, C_KEY, false);
        }
    }
}
