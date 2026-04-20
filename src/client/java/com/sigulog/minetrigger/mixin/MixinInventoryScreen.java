package com.sigulog.minetrigger.mixin;

import com.sigulog.minetrigger.MineTriggerNetwork;
import com.sigulog.minetrigger.ModItems;
import com.sigulog.minetrigger.client.MineTriggerClient;
import com.sigulog.minetrigger.client.hud.TrionHud;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * インベントリ画面に以下を追加する。
 *
 * 1. オプションスロット行（ホットバー直下）の背景パネル
 *
 * 2. 銃種別弾薬設定パネル（右側）
 *    - 銃スロットを Shift+クリック でパネルを開閉
 *    - 左クリックは通常通りアイテムを持てる
 *    - パネルのスロットをクリックで弾種サイクル
 */
@Mixin(InventoryScreen.class)
public abstract class MixinInventoryScreen extends HandledScreen<PlayerScreenHandler> {

    protected MixinInventoryScreen(PlayerScreenHandler h, PlayerInventory inv, Text t) {
        super(h, inv, t);
    }

    private int bgX() { return (this.width  - 176) / 2; }
    private int bgY() { return (this.height - 166) / 2; }

    // ── オプションスロット行レイアウト ────────────────────────────
    private static final int OPT_ROW_Y    = 168;
    private static final int OPT_SLOTS    = MineTriggerClient.TRIGGER_SLOT_COUNT;
    private static final int SLOT_SIZE    = 18;
    private static final int OPT_ROW_X    = 8;

    // ── 右パネル（弾薬設定）レイアウト定数 ───────────────────────
    private static final int CFG_DX       = 182;
    private static final int CFG_DY       = 8;
    private static final int CFG_SLOT_W   = 22;
    private static final int CFG_SLOT_H   = 22;
    private static final int CFG_SLOT_GAP = 2;
    private static final int CFG_PANEL_W  = CFG_SLOT_W + 4;
    private static final int CFG_HEADER_H = 12;

    // ── サイクル順序 ─────────────────────────────────────────────
    private static final String[] GUNNER_CYCLE = {"", "asteroid", "meteora", "viper", "hound"};
    private static final String[] SNIPER_CYCLE = {"", "red_bullet"};

    // ── 色 ───────────────────────────────────────────────────────
    private static final int C_KEY        = 0xFFFFDD44;
    private static final int C_OPT_BG    = 0xCC101018;
    private static final int C_CFG_BG    = 0xCC0A1400;
    private static final int C_CFG_G_ACC  = 0xFF44FF88;
    private static final int C_CFG_S_ACC  = 0xFFFF5555;
    private static final int C_SLOT_BG   = 0xAA111122;
    private static final int C_LABEL     = 0xFFCCCCCC;

    /** 現在アモ設定パネルを開いている銃種。null = 非表示。 */
    @Unique
    private WeaponType ammoConfigGun = null;

    @Unique
    private int[] cfgSlotBounds(int slotIndex) {
        int x0 = bgX() + CFG_DX + 2;
        int y0 = bgY() + CFG_DY + CFG_HEADER_H + slotIndex * (CFG_SLOT_H + CFG_SLOT_GAP);
        return new int[]{x0, y0, x0 + CFG_SLOT_W, y0 + CFG_SLOT_H};
    }

    // ──────────────────────────────────────────────────────────────
    // 背景描画
    // ──────────────────────────────────────────────────────────────

    @Inject(method = "drawBackground", at = @At("RETURN"))
    private void drawOptionRowBg(DrawContext ctx, float delta,
                                 int mouseX, int mouseY, CallbackInfo ci) {
        int bx = bgX();
        int by = bgY();

        // ── オプションスロット行 ──────────────────────────────────
        int rowX = bx + OPT_ROW_X - 1;
        int rowY = by + OPT_ROW_Y - 1;
        int rowW = OPT_SLOTS * SLOT_SIZE + 2;
        int rowH = SLOT_SIZE + 2;
        ctx.fill(rowX - 1, rowY - 1, rowX + rowW + 1, rowY + rowH + 1, 0xFF555555);
        ctx.fill(rowX,     rowY,     rowX + rowW,     rowY + rowH,     C_OPT_BG);

        // ── 弾薬設定パネル ────────────────────────────────────────
        WeaponType gun = ammoConfigGun;
        if (gun == null) return;

        int slots  = gun.isGunnerGun() ? 2 : 1;
        int accent = gun.isSniper() ? C_CFG_S_ACC : C_CFG_G_ACC;
        int panelH = CFG_HEADER_H + slots * CFG_SLOT_H + (slots - 1) * CFG_SLOT_GAP + 4;

        int ax = bx + CFG_DX;
        int ay = by + CFG_DY;
        ctx.fill(ax,     ay, ax + CFG_PANEL_W, ay + panelH, C_CFG_BG);
        ctx.fill(ax - 1, ay, ax,               ay + panelH, accent);
    }

    // ──────────────────────────────────────────────────────────────
    // ラベル・スロット描画
    // ──────────────────────────────────────────────────────────────

    @Inject(method = "render", at = @At("RETURN"))
    private void drawPanelLabels(DrawContext ctx, int mouseX, int mouseY,
                                 float delta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int bx = bgX();
        int by = bgY();

        // ── オプション行ラベル ──────────────────────────────────
        ctx.drawText(mc.textRenderer,
            Text.literal("Option"),
            bx + OPT_ROW_X, by + OPT_ROW_Y - 9, 0xFFAAAAAA, false);
        for (int i = 0; i < OPT_SLOTS; i++) {
            ctx.drawText(mc.textRenderer,
                Text.literal(String.valueOf(i + 1)),
                bx + OPT_ROW_X + i * SLOT_SIZE + 1, by + OPT_ROW_Y + 1, C_KEY, false);
        }

        // ── 弾薬設定パネル ────────────────────────────────────────
        WeaponType gun = ammoConfigGun;
        if (gun == null) return;

        int slots  = gun.isGunnerGun() ? 2 : 1;
        int accent = gun.isSniper() ? C_CFG_S_ACC : C_CFG_G_ACC;
        String prefix = gun.isSniper() ? "§cS§r" : "§aG§r";

        // ヘッダー: "Shift+Click to open"ヒント付き
        ctx.drawText(mc.textRenderer,
            Text.literal(prefix + " AMMO"),
            bx + CFG_DX + 2, by + CFG_DY + 2, C_LABEL, false);

        String[] ammos = TrionHud.getGunAmmos(gun.configKey);
        for (int i = 0; i < slots; i++) {
            int[] b = cfgSlotBounds(i);
            ctx.fill(b[0] - 1, b[1] - 1, b[2] + 1, b[3] + 1, accent);
            ctx.fill(b[0],     b[1],      b[2],     b[3],     C_SLOT_BG);

            String ammoKey = i < ammos.length ? ammos[i] : "";
            if (!ammoKey.isEmpty()) {
                WeaponType ammoType = WeaponType.fromConfigKey(ammoKey);
                if (ammoType != null) {
                    var item = ModItems.get(ammoType);
                    if (item != null) ctx.drawItem(new ItemStack(item), b[0] + 3, b[1] + 3);
                }
            } else {
                ctx.drawText(mc.textRenderer, Text.literal("--"),
                    b[0] + 4, b[1] + 7, 0xFF666666, false);
            }
            ctx.drawText(mc.textRenderer, Text.literal(String.valueOf(i + 1)),
                b[2] + 3, b[1] + 7, C_KEY, false);
        }

        for (int i = 0; i < slots; i++) {
            int[] b = cfgSlotBounds(i);
            if (mouseX >= b[0] && mouseX < b[2] && mouseY >= b[1] && mouseY < b[3]) {
                ctx.drawText(mc.textRenderer, Text.literal("§7Click to cycle"),
                    b[0] - 10, b[3] + 2, C_LABEL, false);
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // マウスクリック
    // ──────────────────────────────────────────────────────────────

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button,
                                CallbackInfoReturnable<Boolean> cir) {

        // ── 1. アモパネルのスロットをクリック → サイクル ──────────
        if (ammoConfigGun != null) {
            int slots  = ammoConfigGun.isGunnerGun() ? 2 : 1;
            String[] cycle = ammoConfigGun.isSniper() ? SNIPER_CYCLE : GUNNER_CYCLE;
            for (int i = 0; i < slots; i++) {
                int[] b = cfgSlotBounds(i);
                if (mouseX >= b[0] && mouseX < b[2] && mouseY >= b[1] && mouseY < b[3]) {
                    String[] ammos = TrionHud.getGunAmmos(ammoConfigGun.configKey);
                    String current = i < ammos.length ? ammos[i] : "";
                    String next = cycle[0];
                    for (int j = 0; j < cycle.length; j++) {
                        if (cycle[j].equals(current)) {
                            next = cycle[(j + 1) % cycle.length];
                            break;
                        }
                    }
                    if (ClientPlayNetworking.canSend(MineTriggerNetwork.SetGunAmmoPayload.ID)) {
                        ClientPlayNetworking.send(
                            new MineTriggerNetwork.SetGunAmmoPayload(ammoConfigGun.configKey, i, next));
                    }
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        // ── 2. Shift+クリックで銃スロット → パネル開閉 ───────────
        //       クイックムーブを横取りしてパネルを開く（通常クリックは通す）
        if (Screen.hasShiftDown()) {
            for (Slot slot : this.handler.slots) {
                if (!slot.hasStack()) continue;
                ItemStack stack = slot.getStack();
                if (!(stack.getItem() instanceof WeaponItem wi)) continue;
                WeaponType wt = wi.getWeaponType();
                if (!wt.isGunnerGun() && !wt.isSniper()) continue;

                int sx = bgX() + slot.x;
                int sy = bgY() + slot.y;
                if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                    // 同じ銃を再度 Shift+クリック → 閉じる
                    ammoConfigGun = (ammoConfigGun == wt) ? null : wt;
                    cir.setReturnValue(true); // クイックムーブをキャンセル
                    return;
                }
            }
        }

        // ── 3. 他の場所をクリック → パネルを閉じる（バニラ処理は通す） ──
        ammoConfigGun = null;
    }
}
