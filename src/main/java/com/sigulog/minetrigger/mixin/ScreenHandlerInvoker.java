package com.sigulog.minetrigger.mixin;

import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.item.ItemStack;

/**
 * ScreenHandler の protected メソッドを Mixin 外から呼び出せるようにするアクセサ。
 */
@Mixin(ScreenHandler.class)
public interface ScreenHandlerInvoker {
    @Invoker("addSlot")
    Slot invokeAddSlot(Slot slot);

    @Invoker("insertItem")
    boolean invokeInsertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast);
}
