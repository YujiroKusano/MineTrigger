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
        ScreenHandlerInvoker invoker = (ScreenHandlerInvoker)(Object)this;
        OptionTriggerInventory optInv = TriggerFrameManager.getOrCreateInventory(player);
        for (int i = 0; i < OptionTriggerInventory.SIZE; i++) {
            final int sx    = -28;
            final int sy    = 26 + i * 20;
            final int index = i;
            invoker.invokeAddSlot(new Slot(optInv, index, sx, sy) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return stack.isEmpty() ||
                           (stack.getItem() instanceof WeaponItem wi && wi.getWeaponType().isOption());
                }
            });
        }
    }

    /**
     * シフトクリックでのアイテム移動を拡張する。
     *
     * ・オプションスロット(46-51)をシフトクリック → メインインベントリ(9-44)に戻す
     * ・メインインベントリ/ホットバー(9-44)のオプション系武器をシフトクリック
     *   → 空きオプションスロットに移動
     */
    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void onQuickMove(PlayerEntity player, int index,
                             CallbackInfoReturnable<ItemStack> cir) {
        ScreenHandlerInvoker invoker = (ScreenHandlerInvoker)(Object)this;
        ScreenHandler sh = (ScreenHandler)(Object)this;

        int optEnd = OPTION_SLOT_START + OptionTriggerInventory.SIZE; // 52

        // スロット数が足りなければスキップ（セーフガード）
        if (sh.slots.size() < optEnd) return;

        if (index >= OPTION_SLOT_START && index < optEnd) {
            // ── オプションスロット → インベントリに戻す ─────────────
            Slot optSlot = sh.slots.get(index);
            ItemStack stack = optSlot.getStack();
            if (!stack.isEmpty()) {
                ItemStack copy = stack.copy();
                // 9-35: メインインベントリ、36-44: ホットバー
                invoker.invokeInsertItem(copy, 9, 36, false);
                invoker.invokeInsertItem(copy, 36, 45, false);
                optSlot.setStack(ItemStack.EMPTY);
            }
            cir.setReturnValue(ItemStack.EMPTY);

        } else if (index >= 9 && index < 45) {
            // ── インベントリ/ホットバーのオプション武器 → オプションスロットへ ──
            Slot srcSlot = sh.slots.get(index);
            ItemStack stack = srcSlot.getStack();
            if (!stack.isEmpty()
                    && stack.getItem() instanceof WeaponItem wi
                    && wi.getWeaponType().isOption()) {
                ItemStack copy = stack.copy();
                boolean moved = invoker.invokeInsertItem(copy, OPTION_SLOT_START, optEnd, false);
                if (moved) {
                    int movedCount = stack.getCount() - copy.getCount();
                    stack.decrement(movedCount);
                    if (stack.isEmpty()) srcSlot.setStack(ItemStack.EMPTY);
                    cir.setReturnValue(ItemStack.EMPTY);
                }
                // 移動できなければバニラ処理に任せる（キャンセルしない）
            }
        }
    }
}
