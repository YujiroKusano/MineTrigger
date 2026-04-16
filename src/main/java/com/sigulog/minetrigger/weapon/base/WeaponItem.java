package com.sigulog.minetrigger.weapon.base;

import com.sigulog.minetrigger.MineTriggerMod;
import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.CooldownManager;
import com.sigulog.minetrigger.core.TrionSystem;
import com.sigulog.minetrigger.weapon.WeaponType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * 全武器トリガーの基底アイテムクラス。
 *
 * 右クリック（use）でトリガー発動。
 * シフト + 右クリックで特殊技発動。
 *
 * Phase 2以降で各武器がこのクラスを継承し、
 * activateNormal / activateSpecial をオーバーライドする。
 */
public class WeaponItem extends Item {

    protected final WeaponType weaponType;

    public WeaponItem(WeaponType type, Settings settings) {
        super(settings);
        this.weaponType = type;
    }

    public WeaponType getWeaponType() {
        return weaponType;
    }

    // ──────────────────────────────────────────────────────────────
    // 発動ロジック（右クリック）
    // ──────────────────────────────────────────────────────────────

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.fail(stack);
        }

        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        boolean fired = tryActivate(serverPlayer, hand);
        return fired ? TypedActionResult.success(stack) : TypedActionResult.fail(stack);
    }

    /**
     * 指定の手の武器を発動する。成功すれば true を返す。
     */
    public boolean tryActivate(ServerPlayerEntity player, Hand hand) {
        // クールダウン確認
        if (CooldownManager.isOnCooldown(player, weaponType.configKey)) {
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.5f);
            return false;
        }

        // トリオン残量確認
        WeaponParams params = ModConfig.get().getWeaponParams(weaponType.configKey);
        int cost = (params != null) ? params.trionUse : 5;

        if (!TrionSystem.consumeTrion(player, cost)) {
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 0.5f, 0.3f);
            return false;
        }

        // 特殊技 or 通常発動
        if (player.isSneaking()) {
            activateSpecial(player, hand);
        } else {
            activateNormal(player, hand);
        }

        // クールダウン開始
        int cd = (params != null) ? params.cooldownTicks : 10;
        CooldownManager.startCooldown(player, weaponType.configKey, cd);

        return true;
    }

    /**
     * 通常発動（左クリック相当）。
     * Phase 2以降でオーバーライドして具体的な攻撃処理を実装する。
     */
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        MineTriggerMod.LOGGER.debug("[{}] 通常発動: {}", player.getName().getString(), weaponType);
        // TODO Phase 2: 各武器の攻撃処理
    }

    /**
     * 特殊技発動（シフト + 右クリック相当）。
     * Phase 2以降でオーバーライド。
     */
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        MineTriggerMod.LOGGER.debug("[{}] 特殊技: {}", player.getName().getString(), weaponType);
        // TODO Phase 2: 各武器の特殊技処理
    }

    // ──────────────────────────────────────────────────────────────
    // ツールチップ
    // ──────────────────────────────────────────────────────────────

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        WeaponParams params = ModConfig.get().getWeaponParams(weaponType.configKey);
        if (params != null) {
            tooltip.add(Text.literal("装備コスト: " + params.trionEquipCost + " / 消費: " + params.trionUse));
            tooltip.add(Text.literal("CD: " + params.cooldownTicks + " tick"));
        }
    }
}
