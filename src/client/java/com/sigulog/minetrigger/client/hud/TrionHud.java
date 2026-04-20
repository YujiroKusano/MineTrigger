package com.sigulog.minetrigger.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sigulog.minetrigger.ModItems;
import com.sigulog.minetrigger.client.MineTriggerClient;
import com.sigulog.minetrigger.client.ScopeHandler;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    // スロットのサイズ・配置定数（コンパクトサイズに変更）
    private static final int CELL      = 22;   // 外枠一辺（縮小）
    private static final int CORNER    = 4;    // 八角形の角カット幅
    private static final int BORDER    = 2;    // 枠線の太さ
    private static final int COL_GAP   = 3;    // 列間隔
    private static final int ROW_GAP   = 3;    // 行間隔
    private static final int COLS      = 5;
    private static final int ROWS      = 1;

    // 画面左下からのオフセット
    private static final int PANEL_LEFT   = 6;
    private static final int PANEL_BOTTOM = 45;   // 画面下端からの距離

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
            renderAmmoMode(ctx, client, MARGIN_X, sh - MARGIN_BOTTOM - 22);
            if (ScopeHandler.showOverlay()) renderScopeOverlay(ctx, sw, sh);
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
    // 弾薬モードインジケーター（メインハンドの銃種別）
    // ──────────────────────────────────────────────────────────────

    private static void renderAmmoMode(DrawContext ctx, MinecraftClient client, int x, int y) {
        if (client.player == null) return;

        // メインハンドまたはオフハンドの銃を確認
        WeaponType wt = null;
        var main = client.player.getMainHandStack();
        var off  = client.player.getOffHandStack();
        if (main.getItem() instanceof WeaponItem wi && (wi.getWeaponType().isGunnerGun() || wi.getWeaponType().isSniper()))
            wt = wi.getWeaponType();
        else if (off.getItem() instanceof WeaponItem wi && (wi.getWeaponType().isGunnerGun() || wi.getWeaponType().isSniper()))
            wt = wi.getWeaponType();

        if (wt == null) return;

        String[] ammos = getGunAmmos(wt.configKey);
        int mode = getGunMode(wt.configKey);
        String a0 = ammos.length > 0 ? ammos[0] : "";
        String a1 = ammos.length > 1 ? ammos[1] : "";
        String prefix = wt.isSniper() ? "S" : "G";

        String label = switch (mode) {
            case 1 -> prefix + ": §a" + (a0.isEmpty() ? "?" : a0);
            case 2 -> prefix + ": §a" + (a1.isEmpty() ? "?" : a1);
            default -> prefix + ": §7通常弾";
        };
        ctx.drawText(client.textRenderer, Text.literal(label), x, y, 0xFFFFFFFF, true);
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

        // ─── アイテムアイコン or ダッシュ ───
        if (isSet) {
            // アイテムアイコンを描画
            WeaponType wt = WeaponType.fromConfigKey(key);
            if (wt != null) {
                var item = ModItems.get(wt);
                if (item != null) {
                    int iconX = ox + (r - 16) / 2;
                    int iconY = oy + (r - 16) / 2;
                    ctx.drawItem(new ItemStack(item), iconX, iconY);
                }
            }
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
    // 弾薬キャッシュ（銃種 configKey → ammo配列・モード）
    // ──────────────────────────────────────────────────────────────

    private static final Map<String, String[]> gunAmmoCache = new ConcurrentHashMap<>();
    private static final Map<String, Integer>  gunModeCache = new ConcurrentHashMap<>();

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

    /** S2C GunAmmoUpdatePayload 受信時に呼ぶ */
    public static void updateGunAmmo(String gunKey, String a0, String a1, int mode) {
        gunAmmoCache.put(gunKey, new String[]{
            a0 != null ? a0 : "",
            a1 != null ? a1 : ""
        });
        gunModeCache.put(gunKey, mode);
    }

    /** インベントリ画面から現在アモ配列を取得する */
    public static String[] getGunAmmos(String gunKey) {
        return gunAmmoCache.getOrDefault(gunKey, new String[]{"", ""});
    }

    /** インベントリ画面から現在モードを取得する */
    public static int getGunMode(String gunKey) {
        return gunModeCache.getOrDefault(gunKey, 0);
    }

    /** インベントリ画面から参照するためのスナップショット */
    public static String[] getTriggerFrameKeys() {
        return triggerFrameKeys.clone();
    }

    // ──────────────────────────────────────────────────────────────
    // スコープオーバーレイ
    // ──────────────────────────────────────────────────────────────

    private static void renderScopeOverlay(DrawContext ctx, int sw, int sh) {
        float cx = sw / 2f;
        float cy = sh / 2f;
        // スコープ円の半径: 画面短辺の38%
        float r  = Math.min(sw, sh) * 0.38f;

        // ── 四辺の暗いマスク（中央円の外側を覆う矩形4枚） ─────────────
        int dark = 0xFF000000;
        int ri = (int) r;
        ctx.fill(0,         0,          sw,          (int)(cy - r), dark); // 上
        ctx.fill(0,         (int)(cy+r), sw,          sh,            dark); // 下
        ctx.fill(0,         (int)(cy-r), (int)(cx-r), (int)(cy+r),   dark); // 左
        ctx.fill((int)(cx+r),(int)(cy-r), sw,          (int)(cy+r),   dark); // 右

        Matrix4f mat = ctx.getMatrices().peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        // ── スコープ円の外縁リング ──────────────────────────────────
        drawScopeRing(mat, cx, cy, r, r + 6f);

        // ── 十字線 ────────────────────────────────────────────────
        drawScopeCross(mat, cx, cy, r);

        RenderSystem.disableBlend();
    }

    /** スコープ円の外縁リング（黒い太いリング） */
    private static void drawScopeRing(Matrix4f m, float cx, float cy, float inner, float outer) {
        int seg = 64;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        for (int i = 0; i <= seg; i++) {
            float a   = (float)(i * 2 * Math.PI / seg);
            float cos = (float)Math.cos(a);
            float sin = (float)Math.sin(a);
            buf.vertex(m, cx + outer * cos, cy + outer * sin, 0).color(0, 0, 0, 255);
            buf.vertex(m, cx + inner * cos, cy + inner * sin, 0).color(0, 0, 0, 0);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    /** 十字線（4本の短い線） */
    private static void drawScopeCross(Matrix4f m, float cx, float cy, float r) {
        float gapLen  = r * 0.12f; // 中央の空白
        float lineLen = r * 0.22f; // 線の長さ
        int   alpha   = 180;

        Tessellator tess = Tessellator.getInstance();

        // 横線（左・右）
        for (int sign : new int[]{-1, 1}) {
            float x0 = cx + sign * gapLen;
            float x1 = cx + sign * (gapLen + lineLen);
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buf.vertex(m, x0, cy - 0.5f, 0).color(255, 255, 255, alpha);
            buf.vertex(m, x1, cy - 0.5f, 0).color(255, 255, 255, alpha);
            buf.vertex(m, x1, cy + 0.5f, 0).color(255, 255, 255, alpha);
            buf.vertex(m, x0, cy + 0.5f, 0).color(255, 255, 255, alpha);
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }

        // 縦線（上・下）
        for (int sign : new int[]{-1, 1}) {
            float y0 = cy + sign * gapLen;
            float y1 = cy + sign * (gapLen + lineLen);
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buf.vertex(m, cx - 0.5f, y0, 0).color(255, 255, 255, alpha);
            buf.vertex(m, cx + 0.5f, y0, 0).color(255, 255, 255, alpha);
            buf.vertex(m, cx + 0.5f, y1, 0).color(255, 255, 255, alpha);
            buf.vertex(m, cx - 0.5f, y1, 0).color(255, 255, 255, alpha);
            BufferRenderer.drawWithGlobalProgram(buf.end());
        }
    }

    private TrionHud() {}
}
