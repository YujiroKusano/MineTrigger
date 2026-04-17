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
        if (world.isClient) return TypedActionResult.fail(stack);
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        boolean fired = tryActivate(serverPlayer, hand, serverPlayer.isSneaking());
        return fired ? TypedActionResult.success(stack) : TypedActionResult.fail(stack);
    }

    /**
     * 通常発動のショートカット（オプショントリガー枠・後方互換用）。
     */
    public boolean tryActivate(ServerPlayerEntity player, Hand hand) {
        return tryActivate(player, hand, false);
    }

    /**
     * 指定の手の武器を発動する。
     *
     * @param special true = 特殊発動（Shift＋右クリック）、false = 通常発動
     */
    public boolean tryActivate(ServerPlayerEntity player, Hand hand, boolean special) {
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

        // 特殊発動 or 通常発動
        if (special) {
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
     * 通常発動 — 持っている手のクリックで発動。
     * 左手武器: 左クリック / 右手武器: 右クリック
     */
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        MineTriggerMod.LOGGER.debug("[{}] 通常発動: {}", player.getName().getString(), weaponType);
    }

    /**
     * 特殊発動 — Shift＋右クリックで発動（手に関わらず右クリック固定）。
     */
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        MineTriggerMod.LOGGER.debug("[{}] 特殊発動: {}", player.getName().getString(), weaponType);
    }

    /**
     * 特殊スキル（パッシブ） — どちらかの手に持っているだけで毎tick適用される。
     * サブクラスでオーバーライドしてバフ/デバフを実装する。
     */
    public void passiveEffect(ServerPlayerEntity player) {
        // デフォルト: 効果なし
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
