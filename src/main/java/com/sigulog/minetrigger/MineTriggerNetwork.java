package com.sigulog.minetrigger;

import com.sigulog.minetrigger.core.TriggerFrameManager;
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
                                          String s3, String s4, String s5)
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
                PacketCodecs.STRING, TriggerFrameSyncPayload::s5,
                TriggerFrameSyncPayload::new
            );
        @Override public Id<? extends CustomPayload> getId() { return ID; }

        public String slot(int i) {
            return switch (i) {
                case 0 -> s0; case 1 -> s1; case 2 -> s2;
                case 3 -> s3; case 4 -> s4; default -> s5;
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
    // 登録
    // ──────────────────────────────────────────────────────────────

    private static volatile boolean registered = false;

    public static void registerServer() {
        if (registered) return;
        registered = true;
        // S2C 登録
        PayloadTypeRegistry.playS2C().register(TrionSyncPayload.ID,         TrionSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TriggerFrameSyncPayload.ID,  TriggerFrameSyncPayload.CODEC);

        // C2S 登録
        PayloadTypeRegistry.playC2S().register(TriggerActivatePayload.ID, TriggerActivatePayload.CODEC);

        // 数字キー → トリガー発動
        ServerPlayNetworking.registerGlobalReceiver(TriggerActivatePayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            int slot = payload.slot();
            if (slot < 0 || slot >= TriggerFrameManager.SLOT_COUNT) return;
            context.server().execute(() -> TriggerFrameManager.activate(player, slot));
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
            keys[0], keys[1], keys[2], keys[3], keys[4], keys[5]));
    }

    private MineTriggerNetwork() {}
}
