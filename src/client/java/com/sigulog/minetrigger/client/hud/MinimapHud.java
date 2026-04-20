package com.sigulog.minetrigger.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/**
 * ミニマップ HUD（画面右上）。
 *
 * ・地形カラー: 最上位ブロックの MapColor を使用
 * ・自分 = 白点（中央）
 * ・他プレイヤー = 青点（通常）、赤点（GLOWING 状態 = 索敵系スキルに引っかかっている）
 *
 * 表示範囲: MAP_SCALE ブロック/ピクセル で MAP_SIZE px の正方形エリア。
 */
public final class MinimapHud {

    private static final int MAP_SIZE   = 80;    // ピクセルサイズ
    private static final int MAP_SCALE  = 2;     // 1px = 2ブロック
    private static final int MARGIN     = 6;     // 画面端からの余白

    // 色
    private static final int C_BG       = 0xAA000A12;
    private static final int C_BORDER   = 0xFF0055AA;
    private static final int C_SELF     = 0xFFFFFFFF;
    private static final int C_ALLY     = 0xFF4488FF;
    private static final int C_ENEMY    = 0xFFFF3333;
    private static final int C_LABEL    = 0xFF88AACC;

    public static void register() {
        HudRenderCallback.EVENT.register((ctx, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) return;
            if (client.currentScreen != null) return;

            int sw = client.getWindow().getScaledWidth();
            renderMinimap(ctx, client, sw - MAP_SIZE - MARGIN, MARGIN);
        });
    }

    private static void renderMinimap(DrawContext ctx, MinecraftClient client, int ox, int oy) {
        // ── 背景 ──────────────────────────────────────────────────
        ctx.fill(ox - 1, oy - 1, ox + MAP_SIZE + 1, oy + MAP_SIZE + 1, C_BORDER);
        ctx.fill(ox,     oy,     ox + MAP_SIZE,     oy + MAP_SIZE,     C_BG);

        ClientPlayerEntity self = client.player;
        if (self == null || client.world == null) return;

        int cx = ox + MAP_SIZE / 2;
        int cy = oy + MAP_SIZE / 2;

        // ── 地形描画（最上位ブロックの MapColor） ─────────────────
        int selfX = (int) self.getX();
        int selfZ = (int) self.getZ();

        for (int px = 0; px < MAP_SIZE; px++) {
            for (int pz = 0; pz < MAP_SIZE; pz++) {
                int relX = px - MAP_SIZE / 2;
                int relZ = pz - MAP_SIZE / 2;
                int wx = selfX + relX * MAP_SCALE;
                int wz = selfZ + relZ * MAP_SCALE;

                try {
                    int wy = client.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, wx, wz);
                    BlockPos pos = new BlockPos(wx, wy - 1, wz);
                    MapColor mc = client.world.getBlockState(pos).getMapColor(client.world, pos);
                    if (mc != MapColor.CLEAR) {
                        // 高さに応じた明度調整（高いほど明るく）
                        MapColor.Brightness brightness = wy > 80 ? MapColor.Brightness.HIGH
                            : wy > 50 ? MapColor.Brightness.NORMAL
                            : wy > 30 ? MapColor.Brightness.LOW
                            : MapColor.Brightness.LOWEST;
                        int color = mc.getRenderColor(brightness);
                        ctx.fill(ox + px, oy + pz, ox + px + 1, oy + pz + 1, color);
                    }
                } catch (Exception ignored) {
                    // チャンク未ロード等は無視
                }
            }
        }

        // ── 中央の十字線（薄く） ───────────────────────────────────
        ctx.fill(cx,     oy,     cx + 1, oy + MAP_SIZE, 0x22FFFFFF);
        ctx.fill(ox,     cy,     ox + MAP_SIZE, cy + 1, 0x22FFFFFF);

        // ── 他プレイヤー ──────────────────────────────────────────
        for (PlayerEntity entity : client.world.getPlayers()) {
            if (entity == self) continue;

            double dx = entity.getX() - self.getX();
            double dz = entity.getZ() - self.getZ();

            int px = cx + (int) (dx / MAP_SCALE);
            int pz = cy + (int) (dz / MAP_SCALE);

            // マップ範囲内のみ描画（範囲外は端にクランプ）
            px = Math.max(ox + 2, Math.min(ox + MAP_SIZE - 3, px));
            pz = Math.max(oy + 2, Math.min(oy + MAP_SIZE - 3, pz));

            // グロウイング（索敵系スキルに引っかかっている）→ 赤、そうでなければ青
            int color = entity.isGlowing() ? C_ENEMY : C_ALLY;
            ctx.fill(px - 1, pz - 1, px + 2, pz + 2, color);
        }

        // ── 自分（中央・白い十字） ────────────────────────────────
        ctx.fill(cx - 2, cy - 2, cx + 3, cy + 3, C_SELF);

        // ── ラベル ────────────────────────────────────────────────
        ctx.drawText(client.textRenderer,
            Text.literal("MAP"),
            ox + 2, oy + 2, C_LABEL, false);
    }

    private MinimapHud() {}
}
