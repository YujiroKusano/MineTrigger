package com.sigulog.minetrigger.mixin;

import com.sigulog.minetrigger.core.OptionTriggerInventory;
import com.sigulog.minetrigger.core.TriggerFrameManager;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * オプショントリガースロットの中身をプレイヤーNBTに永続化する。
 *
 * これにより切断・再接続後もオプションスロットのアイテムが保持され、
 * 「実インベントリ」と同等の扱いになる。
 */
@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {

    private static final String NBT_KEY = "MineTriggerOptions";

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void saveOptionTriggers(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity sp)) return;

        OptionTriggerInventory inv = TriggerFrameManager.getInventoryOrNull(sp.getUuid());
        if (inv == null) return;

        DefaultedList<ItemStack> temp = DefaultedList.ofSize(OptionTriggerInventory.SIZE, ItemStack.EMPTY);
        for (int i = 0; i < OptionTriggerInventory.SIZE; i++) {
            temp.set(i, inv.getStack(i));
        }
        NbtCompound optNbt = Inventories.writeNbt(new NbtCompound(), temp, sp.getRegistryManager());
        nbt.put(NBT_KEY, optNbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void loadOptionTriggers(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity sp)) return;
        if (!nbt.contains(NBT_KEY, NbtElement.COMPOUND_TYPE)) return;

        NbtCompound optNbt = nbt.getCompound(NBT_KEY);
        DefaultedList<ItemStack> temp = DefaultedList.ofSize(OptionTriggerInventory.SIZE, ItemStack.EMPTY);
        Inventories.readNbt(optNbt, temp, sp.getRegistryManager());

        OptionTriggerInventory inv = TriggerFrameManager.getOrCreateInventory(sp);
        for (int i = 0; i < OptionTriggerInventory.SIZE; i++) {
            inv.setStack(i, temp.get(i));
        }
    }
}
