package com.sigulog.minetrigger.client.hud;

import com.sigulog.minetrigger.client.MineTriggerClient;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * クライアントサイドの HUD。
 *
 * ① トリオンゲージ（画面左下）
 * ② オプショントリガー枠×6（画面左下、3列×2行のヘキサゴン風配置）
 *    - 枠はインベントリスロットとは独立
 *    - オプション系トリガーのみ設定可能
 *    - 武器セット済み: 青枠・武器名表示
 *    - 未セット: グレー枠
 */
public final class TrionHud {

    // ── トリオンゲージ ──────────────────────────────────────────
    private static volatile float currentTrion = 100f;
    private static volatile float maxTrion     = 100f;

    private static final int BAR_WIDTH     = 100;
    private static final int BAR_HEIGHT    = 8;
    private static final int MARGIN_X      = 10;
    private static final int MARGIN_BOTTOM = 30;

    // ── トリガー枠（3列×2行） ───────────────────────────────────
    /** サーバーから同期されたトリガー枠の武器キー（"" = 未設定） */
    private static final String[] triggerFrameKeys =
        new String[MineTriggerClient.TRIGGER_SLOT_COUNT];

    static {
        java.util.Arrays.fill(triggerFrameKeys, "");
    }

    // スロットのサイズ・配置定数
    private static final int CELL      = 32;   // 外枠一辺
    private static final int CORNER    = 6;    // 八角形の角カット幅
    private static final int BORDER    = 2;    // 枠線の太さ
    private static final int COL_GAP   = 5;    // 列間隔
    private static final int ROW_GAP   = 5;    // 行間隔
    private static final int COLS      = 3;
    private static final int ROWS      = 2;

    // 画面左下からのオフセット
    private static final int PANEL_LEFT   = 8;
    private static final int PANEL_BOTTOM = 50;   // 画面下端からの距離

    // 色
    private static final int C_BG_EMPTY    = 0xCC111111;
    private static final int C_BG_SET      = 0xCC001840;
    private static final int C_BORDER_E    = 0xFF555577;
    private static final int C_BORDER_S    = 0xFF44AAFF;
    private static final int C_CORNER_MASK = 0xFF000000; // コーナーをマスクする黒
    private static final int C_KEY_LABEL   = 0xFFFFDD44;
    private static final int C_WEAPON_NAME = 0xFFAADDFF;
    private static final int C_EMPTY_DASH  = 0xFF666666;

    // ──────────────────────────────────────────────────────────

    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) return;

            int sw = client.getWindow().getScaledWidth();
            int sh = client.getWindow().getScaledHeight();

            renderTrionBar(ctx, client, MARGIN_X, sh - MARGIN_BOTTOM);
            renderTriggerPanel(ctx, client, sh);
        });
    }

    // ──────────────────────────────────────────────────────────────
    // トリオンゲージ
    // ──────────────────────────────────────────────────────────────

    private static void renderTrionBar(DrawContext ctx, MinecraftClient client, int x, int y) {
        float ratio = (maxTrion > 0) ? (currentTrion / maxTrion) : 0f;
        ratio = Math.max(0f, Math.min(1f, ratio));

        ctx.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xAA000000);

        int barColor;
        if (ratio > 0.5f)       barColor = 0xFF00AAFF;
        else if (ratio > 0.25f) barColor = 0xFFFFAA00;
        else                    barColor = 0xFFFF4444;

        int fw = (int) (BAR_WIDTH * ratio);
        if (fw > 0) ctx.fill(x, y, x + fw, y + BAR_HEIGHT, barColor);

        ctx.drawText(client.textRenderer,
            Text.literal(String.format("TRION  %d / %d", (int) currentTrion, (int) maxTrion)),
            x, y - 10, 0xFFFFFFFF, true);
    }

    // ──────────────────────────────────────────────────────────────
    // トリガー枠パネル（3×2）
    // ──────────────────────────────────────────────────────────────

    private static void renderTriggerPanel(DrawContext ctx, MinecraftClient client, int sh) {
        int panelH = ROWS * CELL + (ROWS - 1) * ROW_GAP;
        int panelY = sh - PANEL_BOTTOM - panelH;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int index = row * COLS + col;
                int cx = PANEL_LEFT + col * (CELL + COL_GAP);
                int cy = panelY + row * (CELL + ROW_GAP);
                renderHexSlot(ctx, client, cx, cy, index);
            }
        }
    }

    /**
     * 八角形（四隅カット）スロットを描画する。
     * @param ox 左上X
     * @param oy 左上Y
     */
    private static void renderHexSlot(DrawContext ctx, MinecraftClient client,
                                      int ox, int oy, int index) {
        String key   = triggerFrameKeys[index];
        boolean isSet = !key.isEmpty();

        int borderColor = isSet ? C_BORDER_S : C_BORDER_E;
        int bgColor     = isSet ? C_BG_SET   : C_BG_EMPTY;
        int r = CELL;

        // ─── 背景全塗り（八角形をくり抜く前の下地） ───
        // 八角形 = 中央矩形 + 上下の細い帯（CORNER分だけ狭い）
        fillOctagon(ctx, ox, oy, r, CORNER, bgColor);

        // ─── 枠線（外側+1px） ───
        fillOctagonBorder(ctx, ox - BORDER, oy - BORDER, r + BORDER * 2, CORNER + BORDER, borderColor);

        // 枠線内側を背景色で塗って枠だけ残す
        fillOctagon(ctx, ox, oy, r, CORNER, bgColor);

        // ─── キー番号（左上） ───
        ctx.drawText(client.textRenderer,
            Text.literal(String.valueOf(index + 1)),
            ox + 3, oy + 3, C_KEY_LABEL, true);

        // ─── 武器名 or ダッシュ ───
        if (isSet) {
            // 枠の中央に短縮表示
            String display = key.length() > 7 ? key.substring(0, 6) + "." : key;
            int tx = ox + (r - client.textRenderer.getWidth(display)) / 2;
            int ty = oy + r / 2 + 2;
            ctx.drawText(client.textRenderer, Text.literal(display), tx, ty, C_WEAPON_NAME, true);
        } else {
            ctx.drawText(client.textRenderer,
                Text.literal("--"),
                ox + (r - client.textRenderer.getWidth("--")) / 2,
                oy + r / 2 - 3, C_EMPTY_DASH, true);
        }
    }

    /**
     * 八角形（CORNER px 四隅カット）を fill する。
     * 黒背景との組み合わせで各コーナーを隠すことで八角形に見せる。
     */
    private static void fillOctagon(DrawContext ctx, int x, int y, int size, int cut, int color) {
        // 縦帯（左右をcutぶん削る）
        ctx.fill(x + cut, y,        x + size - cut, y + size,       color);
        // 左右帯（上下をcutぶん削る）
        ctx.fill(x,       y + cut,  x + cut,        y + size - cut, color);
        ctx.fill(x + size - cut, y + cut, x + size, y + size - cut, color);
    }

    private static void fillOctagonBorder(DrawContext ctx, int x, int y,
                                          int size, int cut, int color) {
        ctx.fill(x + cut, y,        x + size - cut, y + size,       color);
        ctx.fill(x,       y + cut,  x + cut,        y + size - cut, color);
        ctx.fill(x + size - cut, y + cut, x + size, y + size - cut, color);
    }

    // ──────────────────────────────────────────────────────────────
    // 同期受信
    // ──────────────────────────────────────────────────────────────

    public static void updateTrion(float current, float max) {
        currentTrion = current;
        maxTrion     = max;
    }

    public static void updateTriggerFrame(int index, String weaponKey) {
        if (index >= 0 && index < triggerFrameKeys.length) {
            triggerFrameKeys[index] = weaponKey != null ? weaponKey : "";
        }
    }

    /** インベントリ画面から参照するためのスナップショット */
    public static String[] getTriggerFrameKeys() {
        return triggerFrameKeys.clone();
    }

    private TrionHud() {}
}
