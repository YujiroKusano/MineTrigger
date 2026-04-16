package com.sigulog.minetrigger;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Identifier;

/**
 * プレイヤーに紐づくカスタムデータ（トリオン値）の定義。
 * NBTに永続化され、サーバー再起動後もデータが維持される。
 */
public final class TrionDataAttachments {

    /** 現在のトリオン量（0以下でベイルアウト） */
    public static AttachmentType<Float> TRION_CURRENT;

    /** 実効トリオン最大量（基礎トリオン − 装備コスト合計） */
    public static AttachmentType<Float> TRION_MAX;

    public static void register() {
        TRION_CURRENT = AttachmentRegistry.createPersistent(
            Identifier.of(MineTriggerMod.MOD_ID, "trion_current"),
            Codec.FLOAT
        );

        TRION_MAX = AttachmentRegistry.createPersistent(
            Identifier.of(MineTriggerMod.MOD_ID, "trion_max"),
            Codec.FLOAT
        );
    }

    private TrionDataAttachments() {}
}
