package com.sigulog.minetrigger.mixin;

import com.sigulog.minetrigger.core.OptionTriggerInventory;
import com.sigulog.minetrigger.core.TriggerFrameManager;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PlayerScreenHandler にオプショントリガー専用スロット（6個）を追加する。
 *
 * 座標はインベントリ背景左側パネルに対応（slot.x = -28, slot.y = 26 + i*20）。
 *
 * またシフトクリックでオプション系武器をオプションスロット↔インベントリ間で移動できる。
 * オプションスロット = バニラ46スロット(0-45)に続くindex 46-51。
 */
@Mixin(PlayerScreenHandler.class)
public class MixinPlayerScreenHandler {

    /** オプショントリガースロットの開始インデックス（バニラは0-45の46スロット） */
    private static final int OPTION_SLOT_START = 46;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void addOptionTriggerSlots(PlayerInventory inventory, boolean onServer,
                                       PlayerEntity player, CallbackInfo ci) {
        ScreenHandlerInvoker invoker = (ScreenHandlerInvoker) (Object) this;

        // ── オプショントリガースロット（左側 6 枠） ─────────────────
        // ── オプショントリガースロット（ホットバー直下に横1列） ──────
        // InventoryScreen の背景は 176×166。ホットバー行は y=142。
        // オプション行は y=168（ホットバーの 26px 下）に横並びで配置。
        OptionTriggerInventory optInv = TriggerFrameManager.getOrCreateInventory(player);
        for (int i = 0; i < OptionTriggerInventory.SIZE; i++) {
            final int sx = 8 + i * 18;   // ホットバーと同じ左端 x=8 から 18px 間隔
            final int sy = 168;           // ホットバー(y=142) + 18(スロット高) + 8(余白)
            invoker.invokeAddSlot(new Slot(optInv, i, sx, sy) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return stack.isEmpty() || stack.getItem() instanceof WeaponItem;
                }
            });
        }
    }

    /**
     * シフトクリックでのアイテム移動を拡張する。
     *
     * ・オプションスロット(46-50)をシフトクリック → メインインベントリ(9-35)→ホットバー(36-44)に戻す
     * ・メインインベントリ/ホットバー(9-44)のWeaponItemをシフトクリック
     *   → 空きオプションスロットに移動
     *
     * insertItemに頼らず直接Slot.setStackで操作し、sendContentUpdatesで即座にクライアントへ同期する。
     */
    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void onQuickMove(PlayerEntity player, int index,
                             CallbackInfoReturnable<ItemStack> cir) {
        ScreenHandler sh = (ScreenHandler)(Object)this;

        int optEnd = OPTION_SLOT_START + OptionTriggerInventory.SIZE; // 51

        // スロット数が足りなければスキップ（セーフガード）
        if (sh.slots.size() < optEnd) return;

        // クライアント側の予測実行はスキップ。サーバーが正確に処理してsendContentUpdatesで同期する。
        if (player.getWorld().isClient()) return;

        if (index >= OPTION_SLOT_START && index < optEnd) {
            // ── オプションスロット → インベントリに戻す ─────────────
            Slot optSlot = sh.slots.get(index);
            ItemStack stack = optSlot.getStack();
            if (!stack.isEmpty()) {
                outer:
                for (int[] range : new int[][]{{9, 36}, {36, 45}}) {
                    for (int i = range[0]; i < range[1]; i++) {
                        Slot target = sh.slots.get(i);
                        if (target.getStack().isEmpty() && target.canInsert(stack)) {
                            target.setStack(stack.copy());
                            optSlot.setStack(ItemStack.EMPTY);
                            break outer;
                        }
                    }
                }
            }
            sh.sendContentUpdates();
            cir.setReturnValue(ItemStack.EMPTY);

        } else if (index >= 9 && index < 45) {
            // ── インベントリ/ホットバーのWeaponItem → オプションスロットへ ──
            Slot srcSlot = sh.slots.get(index);
            ItemStack stack = srcSlot.getStack();
            if (!stack.isEmpty() && stack.getItem() instanceof WeaponItem) {
                for (int i = OPTION_SLOT_START; i < optEnd; i++) {
                    Slot optSlot = sh.slots.get(i);
                    if (optSlot.getStack().isEmpty() && optSlot.canInsert(stack)) {
                        optSlot.setStack(stack.copy());
                        srcSlot.setStack(ItemStack.EMPTY);
                        sh.sendContentUpdates();
                        cir.setReturnValue(ItemStack.EMPTY);
                        return;
                    }
                }
                // 空きなし → バニラ処理に委譲
            }
        }
    }
}
