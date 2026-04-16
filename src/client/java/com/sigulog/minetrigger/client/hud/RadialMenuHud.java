package com.sigulog.minetrigger.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sigulog.minetrigger.weapon.composite.CompositeRoundItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import org.joml.Matrix4f;

/**
 * 合成弾ラジアルメニューの描画と状態管理。
 *
 * 【配置】
 *         [3: ハウンドメテオラ]    ← top
 * [2: A.ハウンド]  [center]  [0: バイパーメテオラ]
 *         [1: A.メテオラ]          ← bottom
 *
 * 【セクター番号 / 角度（atan2基準: 0°=right, 90°=down）】
 *   0: right  −45° 〜 +45°
 *   1: bottom +45° 〜 +135°
 *   2: left  +135° 〜 +225°
 *   3: top   +225° 〜 +315°
 */
public final class RadialMenuHud {

    // ── 状態 ──────────────────────────────────────────────────────
    private static boolean open = false;
    /** メニューを開いた瞬間の生ピクセル座標 */
    private static double startMouseX, startMouseY;
    /** 現在ホバー中のセクター（-1 = デッドゾーン） */
    private static int hoveredSector = -1;
    /** クライアント側で保持する現在のモード */
    public static CompositeRoundItem.Mode clientMode = CompositeRoundItem.Mode.HOUND_METEORA;

    // ── 定数 ──────────────────────────────────────────────────────
    private static final float OUTER_R    = 72f;
    private static final float INNER_R    = 22f;
    /** デッドゾーン（生ピクセル。これより近いとセクター未選択） */
    private static final double DEAD_PX   = 28.0;
    private static final int    SEGMENTS  = 20;   // 円弧の分割数
    private static final float  GAP_DEG   = 4f;   // セクター間の隙間（度）

    // ── セクター別カラー [r,g,b] ──────────────────────────────────
    // 0:VIPER=紫  1:A.METEORA=橙  2:A.HOUND=水  3:H.METEORA=赤
    private static final int[][] COLORS = {
        {150, 50, 210},
        {210, 130, 20},
        {30,  150, 220},
        {210, 50,  50},
    };

    // ──────────────────────────────────────────────────────────────

    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> render(ctx));
    }

    public static void open(MinecraftClient client) {
        startMouseX  = client.mouse.getX();
        startMouseY  = client.mouse.getY();
        hoveredSector = -1;
        open = true;
    }

    public static void close() {
        open = false;
        hoveredSector = -1;
    }

    public static boolean isOpen()           { return open; }
    public static int     getHovered()       { return hoveredSector; }

    /** 毎tick呼ぶ。マウス移動量からホバーセクターを更新する。 */
    public static void tick(MinecraftClient client) {
        if (!open) return;
        double dx   = client.mouse.getX() - startMouseX;
        double dy   = client.mouse.getY() - startMouseY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < DEAD_PX) {
            hoveredSector = -1;
        } else {
            double deg     = Math.toDegrees(Math.atan2(dy, dx)); // −180〜180
            double shifted = ((deg + 45 + 360) % 360);           // 0〜360 (0=right sector start)
            hoveredSector  = (int) (shifted / 90) % 4;
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 描画
    // ──────────────────────────────────────────────────────────────

    private static void render(DrawContext ctx) {
        if (!open) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int W  = mc.getWindow().getScaledWidth();
        int H  = mc.getWindow().getScaledHeight();
        float cx = W / 2f;
        float cy = H / 2f;

        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        // ── 背景の大円 ────────────────────────────────────────────
        drawCircle(mat, cx, cy, OUTER_R + 10f, 0, 0, 0, 160);

        // ── 4セクター ────────────────────────────────────────────
        for (int i = 0; i < 4; i++) {
            boolean hovered = (i == hoveredSector);
            float sa = (float) Math.toRadians(i * 90f - 45f + GAP_DEG);
            float ea = (float) Math.toRadians((i + 1) * 90f - 45f - GAP_DEG);
            int[] c  = COLORS[i];
            int alpha = hovered ? 230 : 150;
            float or  = hovered ? OUTER_R + 6f : OUTER_R;
            drawArc(mat, cx, cy, INNER_R, or, sa, ea, c[0], c[1], c[2], alpha);
        }

        // ── 中央穴（暗い円） ────────────────────────────────────
        drawCircle(mat, cx, cy, INNER_R - 1f, 0, 0, 0, 220);

        // ── マウス方向インジケーター ─────────────────────────────
        double scaleFactor = mc.getWindow().getScaleFactor();
        double rawDx = mc.mouse.getX() - startMouseX;
        double rawDy = mc.mouse.getY() - startMouseY;
        double guiDx = rawDx / scaleFactor;
        double guiDy = rawDy / scaleFactor;
        double dist  = Math.sqrt(guiDx * guiDx + guiDy * guiDy);
        if (dist > 1.0) {
            double clamp  = Math.min(dist, OUTER_R - 10);
            float dotX    = cx + (float)(guiDx / dist * clamp);
            float dotY    = cy + (float)(guiDy / dist * clamp);
            drawCircle(mat, dotX, dotY, 5f, 255, 255, 255, 220);
        }

        RenderSystem.disableBlend();

        // ── ラベル ────────────────────────────────────────────────
        TextRenderer tr = mc.textRenderer;
        float ld = OUTER_R + 18f; // ラベル距離

        // セクター0〜3 の中心方向角度（0=right 90=down 180=left 270=top）
        float[] angles = { 0f, 90f, 180f, 270f };

        for (int i = 0; i < 4; i++) {
            CompositeRoundItem.Mode mode = CompositeRoundItem.SECTOR_MODES[i];
            boolean hovered  = (i == hoveredSector);
            boolean isCurrent = (mode == clientMode);

            float ax  = (float) Math.toRadians(angles[i]);
            int lx    = (int)(cx + Math.cos(ax) * ld);
            int ly    = (int)(cy + Math.sin(ax) * ld) - tr.fontHeight / 2;

            int color = hovered  ? 0xFFFFFF :
                        isCurrent ? 0xFFDD44 : 0x999999;
            ctx.drawCenteredTextWithShadow(tr, mode.displayName, lx, ly, color);

            if (isCurrent) {
                ctx.drawCenteredTextWithShadow(tr, "▶", lx, ly - tr.fontHeight - 1, 0xFFDD44);
            }
        }

        // ── 中央: 現在のモード表示 ──────────────────────────────
        String centerText = hoveredSector >= 0
            ? "→ " + CompositeRoundItem.SECTOR_MODES[hoveredSector].displayName
            : clientMode.displayName;
        ctx.drawCenteredTextWithShadow(tr, centerText,
            (int) cx, (int) cy - tr.fontHeight / 2, 0xEEEEEE);
    }

    // ──────────────────────────────────────────────────────────────
    // プリミティブ描画
    // ──────────────────────────────────────────────────────────────

    /** 塗りつぶし円 */
    private static void drawCircle(Matrix4f m, float cx, float cy, float r,
                                    int red, int grn, int blu, int alpha) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(m, cx, cy, 0).color(red, grn, blu, alpha);
        for (int i = 0; i <= SEGMENTS; i++) {
            float a = (float)(i * 2 * Math.PI / SEGMENTS);
            buf.vertex(m, cx + r * (float)Math.cos(a), cy + r * (float)Math.sin(a), 0)
               .color(red, grn, blu, alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    /** ドーナツ型セクター（TRIANGLE_STRIP） */
    private static void drawArc(Matrix4f m, float cx, float cy,
                                 float innerR, float outerR,
                                 float startA, float endA,
                                 int r, int g, int b, int alpha) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= SEGMENTS; i++) {
            float a   = startA + (endA - startA) * i / SEGMENTS;
            float cos = (float)Math.cos(a);
            float sin = (float)Math.sin(a);
            buf.vertex(m, cx + outerR * cos, cy + outerR * sin, 0).color(r, g, b, alpha);
            buf.vertex(m, cx + innerR * cos, cy + innerR * sin, 0).color(r, g, b, alpha / 2);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    private RadialMenuHud() {}
}
