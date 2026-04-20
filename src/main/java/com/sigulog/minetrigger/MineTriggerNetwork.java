package com.sigulog.minetrigger;

import com.sigulog.minetrigger.core.GunnerAmmoManager;
import com.sigulog.minetrigger.core.TriggerFrameManager;
import com.sigulog.minetrigger.weapon.WeaponType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class MineTriggerNetwork {

    // ──────────────────────────────────────────────────────────────
    // S2C: トリオン値同期
    // ──────────────────────────────────────────────────────────────
    public record TrionSyncPayload(float current, float max) implements CustomPayload {
        public static final Id<TrionSyncPayload> ID =
            new Id<>(Identifier.of(MineTriggerMod.MOD_ID, "trion_sync"));
        public static final PacketCodec<PacketByteBuf, TrionSyncPayload> CODEC =
            PacketCodec.tuple(
                PacketCodecs.FLOAT, TrionSyncPayload::current,
                PacketCodecs.FLOAT, TrionSyncPayload::max,
                TrionSyncPayload::new
            );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ──────────────────────────────────────────────────────────────
    // S2C: トリガー枠の内容をクライアントに同期
    // ──────────────────────────────────────────────────────────────
    public record TriggerFrameSyncPayload(String s0, String s1, String s2,
                                          String s3, String s4)
        implements CustomPayload {
        public static final Id<TriggerFrameSyncPayload> ID =
            new Id<>(Identifier.of(MineTriggerMod.MOD_ID, "trigger_frame_sync"));
        public static final PacketCodec<PacketByteBuf, TriggerFrameSyncPayload> CODEC =
            PacketCodec.tuple(
                PacketCodecs.STRING, TriggerFrameSyncPayload::s0,
                PacketCodecs.STRING, TriggerFrameSyncPayload::s1,
                PacketCodecs.STRING, TriggerFrameSyncPayload::s2,
                PacketCodecs.STRING, TriggerFrameSyncPayload::s3,
                PacketCodecs.STRING, TriggerFrameSyncPayload::s4,
                TriggerFrameSyncPayload::new
            );
        @Override public Id<? extends CustomPayload> getId() { return ID; }

        public String slot(int i) {
            return switch (i) {
                case 0 -> s0; case 1 -> s1; case 2 -> s2;
                case 3 -> s3; default -> s4;
            };
        }
    }

    // ──────────────────────────────────────────────────────────────
    // C2S: 数字キーでトリガー発動
    // ──────────────────────────────────────────────────────────────
    public record TriggerActivatePayload(int slot) implements CustomPayload {
        public static final Id<TriggerActivatePayload> ID =
            new Id<>(Identifier.of(MineTriggerMod.MOD_ID, "trigger_activate"));
        public static final PacketCodec<PacketByteBuf, TriggerActivatePayload> CODEC =
            PacketCodec.tuple(
                PacketCodecs.INTEGER, TriggerActivatePayload::slot,
                TriggerActivatePayload::new
            );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ──────────────────────────────────────────────────────────────
    // S2C: 銃種別弾薬スロット同期（1銃種ずつ送信）
    // ──────────────────────────────────────────────────────────────
    public record GunAmmoUpdatePayload(
        String gunKey, String ammo0, String ammo1, int mode
    ) implements CustomPayload {
        public static final Id<GunAmmoUpdatePayload> ID =
            new Id<>(Identifier.of(MineTriggerMod.MOD_ID, "gun_ammo_update"));
        public static final PacketCodec<PacketByteBuf, GunAmmoUpdatePayload> CODEC =
            PacketCodec.tuple(
                PacketCodecs.STRING,  GunAmmoUpdatePayload::gunKey,
                PacketCodecs.STRING,  GunAmmoUpdatePayload::ammo0,
                PacketCodecs.STRING,  GunAmmoUpdatePayload::ammo1,
                PacketCodecs.INTEGER, GunAmmoUpdatePayload::mode,
                GunAmmoUpdatePayload::new
            );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ──────────────────────────────────────────────────────────────
    // C2S: 銃のアモスロットを設定（インベントリ設定UIから）
    // ──────────────────────────────────────────────────────────────
    public record SetGunAmmoPayload(
        String gunKey, int slot, String ammoKey
    ) implements CustomPayload {
        public static final Id<SetGunAmmoPayload> ID =
            new Id<>(Identifier.of(MineTriggerMod.MOD_ID, "set_gun_ammo"));
        public static final PacketCodec<PacketByteBuf, SetGunAmmoPayload> CODEC =
            PacketCodec.tuple(
                PacketCodecs.STRING,  SetGunAmmoPayload::gunKey,
                PacketCodecs.INTEGER, SetGunAmmoPayload::slot,
                PacketCodecs.STRING,  SetGunAmmoPayload::ammoKey,
                SetGunAmmoPayload::new
            );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ──────────────────────────────────────────────────────────────
    // C2S: 合成弾ラジアルメニューの選択
    // ──────────────────────────────────────────────────────────────
    public record CompositeSelectPayload(int sector) implements CustomPayload {
        public static final Id<CompositeSelectPayload> ID =
            new Id<>(Identifier.of(MineTriggerMod.MOD_ID, "composite_select"));
        public static final PacketCodec<PacketByteBuf, CompositeSelectPayload> CODEC =
            PacketCodec.tuple(
                PacketCodecs.INTEGER, CompositeSelectPayload::sector,
                CompositeSelectPayload::new
            );
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ──────────────────────────────────────────────────────────────
    // 登録
    // ──────────────────────────────────────────────────────────────

    private static volatile boolean registered = false;

    public static void registerServer() {
        if (registered) return;
        registered = true;
        // S2C 登録
        PayloadTypeRegistry.playS2C().register(TrionSyncPayload.ID,         TrionSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TriggerFrameSyncPayload.ID,  TriggerFrameSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GunAmmoUpdatePayload.ID,     GunAmmoUpdatePayload.CODEC);

        // C2S 登録
        PayloadTypeRegistry.playC2S().register(TriggerActivatePayload.ID,  TriggerActivatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CompositeSelectPayload.ID,  CompositeSelectPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SetGunAmmoPayload.ID,        SetGunAmmoPayload.CODEC);

        // 数字キー → トリガー発動
        ServerPlayNetworking.registerGlobalReceiver(TriggerActivatePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            int slot = payload.slot();
            if (slot < 0 || slot >= TriggerFrameManager.SLOT_COUNT) return;
            context.server().execute(() -> TriggerFrameManager.activate(player, slot));
        });

        // 銃ごとのアモスロット設定
        ServerPlayNetworking.registerGlobalReceiver(SetGunAmmoPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            context.server().execute(() -> {
                WeaponType gun  = WeaponType.fromConfigKey(payload.gunKey());
                WeaponType ammo = WeaponType.fromConfigKey(payload.ammoKey()); // null = クリア
                if (gun == null) return;
                if (ammo != null) {
                    if (gun.isGunnerGun() && !ammo.isGunnerAmmo()) return;
                    if (gun.isSniper()    && !ammo.isSniperAmmo())  return;
                }
                int slot = payload.slot();
                GunnerAmmoManager.setAmmo(player, gun, slot, ammo);
            });
        });

        // 合成弾モード選択
        ServerPlayNetworking.registerGlobalReceiver(CompositeSelectPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            int sector = payload.sector();
            context.server().execute(() -> {
                com.sigulog.minetrigger.weapon.composite.CompositeRoundItem.setMode(
                    player.getUuid(), sector);
                var mode = com.sigulog.minetrigger.weapon.composite.CompositeRoundItem
                    .getMode(player.getUuid());
                player.sendMessage(
                    net.minecraft.text.Text.literal(
                        "§e[ 合成弾 ]§r " + mode.color + mode.displayName), true);
            });
        });
    }

    // ──────────────────────────────────────────────────────────────
    // 送信ヘルパー
    // ──────────────────────────────────────────────────────────────

    public static void sendTrionSync(ServerPlayerEntity player, float current, float max) {
        ServerPlayNetworking.send(player, new TrionSyncPayload(current, max));
    }

    public static void sendTriggerFrameSync(ServerPlayerEntity player, String[] keys) {
        ServerPlayNetworking.send(player, new TriggerFrameSyncPayload(
            keys[0], keys[1], keys[2], keys[3], keys[4]));
    }

    private MineTriggerNetwork() {}
}
