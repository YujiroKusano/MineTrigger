package com.sigulog.minetrigger.weapon.option;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.config.WeaponParams;
import com.sigulog.minetrigger.core.TrionSystem;
import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * バッグワーム — トリガーレーダーを遮断するオプション。
 *
 * 通常発動: ON/OFFトグル
 *   ON 中: 透明化 + 毎秒トリオン消費
 * 特殊技  : 即時解除
 */
public class BagwormItem extends WeaponItem {

    /** バッグワーム展開中のプレイヤーUUID */
    private static final Set<UUID> active =
        Collections.synchronizedSet(new HashSet<>());

    /** トリオン消費のティックカウンタ */
    private static final java.util.Map<UUID, Integer> tickCount =
        new java.util.concurrent.ConcurrentHashMap<>();

    /** 透明化効果の持続時間（tickごとに更新するので短くてよい） */
    private static final int INVISIBILITY_REFRESH_TICKS = 40;

    public BagwormItem(WeaponType type, Settings settings) {
        super(type, settings);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            synchronized (active) {
                for (UUID uuid : active) {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                    if (player == null) { active.remove(uuid); tickCount.remove(uuid); return; }

                    // 透明化を維持
                    if (!player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                        player.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.INVISIBILITY, INVISIBILITY_REFRESH_TICKS, 0, false, false));
                    }

                    // 毎20tick（1秒）ごとにトリオン消費
                    int cnt = tickCount.merge(uuid, 1, Integer::sum);
                    if (cnt >= 20) {
                        tickCount.put(uuid, 0);
                        WeaponParams p = ModConfig.get().getWeaponParams("bagworm");
                        boolean ok = TrionSystem.consumeTrion(player, p.trionDrainPerSecond);
                        if (!ok) {
                            // トリオン不足で強制解除
                            deactivate(player);
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void activateNormal(ServerPlayerEntity player, Hand hand) {
        if (active.contains(player.getUuid())) {
            deactivate(player);
        } else {
            activate(player);
        }
    }

    @Override
    protected void activateSpecial(ServerPlayerEntity player, Hand hand) {
        if (active.contains(player.getUuid())) {
            deactivate(player);
        }
    }

    private void activate(ServerPlayerEntity player) {
        active.add(player.getUuid());
        tickCount.put(player.getUuid(), 0);
        player.addStatusEffect(new StatusEffectInstance(
            StatusEffects.INVISIBILITY, INVISIBILITY_REFRESH_TICKS, 0, false, false));
        player.sendMessage(Text.literal("§8[ バッグワーム ]§r 展開"), true);
    }

    public static void deactivate(ServerPlayerEntity player) {
        active.remove(player.getUuid());
        tickCount.remove(player.getUuid());
        player.removeStatusEffect(StatusEffects.INVISIBILITY);
        player.sendMessage(Text.literal("§7[ バッグワーム ]§r 解除"), true);
    }

    public static boolean isActive(UUID uuid) {
        return active.contains(uuid);
    }
}
