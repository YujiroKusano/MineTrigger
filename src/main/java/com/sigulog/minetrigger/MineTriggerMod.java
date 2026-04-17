package com.sigulog.minetrigger;

import com.sigulog.minetrigger.config.ModConfig;
import com.sigulog.minetrigger.core.BlockInteractionSystem;
import com.sigulog.minetrigger.core.BulletManager;
import com.sigulog.minetrigger.core.CooldownManager;
import com.sigulog.minetrigger.core.LoadoutManager;
import com.sigulog.minetrigger.core.PassiveEffectSystem;
import com.sigulog.minetrigger.core.ShieldManager;
import com.sigulog.minetrigger.core.TriggerFrameManager;
import com.sigulog.minetrigger.core.TrionSystem;
import com.sigulog.minetrigger.input.InputHandler;
import com.sigulog.minetrigger.command.TrionCommand;
import com.sigulog.minetrigger.weapon.option.BagwormItem;
import com.sigulog.minetrigger.weapon.ranged.AsteroidItem;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MineTriggerMod implements ModInitializer {

    public static final String MOD_ID = "minetrigger";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[MineTrigger] Initializing...");

        // 1. 設定ファイル読み込み
        ModConfig.load();

        // 2. データアタッチメント登録
        TrionDataAttachments.register();

        // 3. アイテム登録
        ModItems.register();

        // 4. ネットワーク登録（S2C/C2S パケット）
        MineTriggerNetwork.registerServer();

        // 5. コアシステム登録
        TrionSystem.register();
        LoadoutManager.register();
        CooldownManager.register();
        BulletManager.register();
        ShieldManager.register();
        PassiveEffectSystem.register();
        BlockInteractionSystem.register();

        // 6. 入力ルーティング登録
        InputHandler.register();

        // 7. オプショントリガー登録
        BagwormItem.register();
        AsteroidItem.register();

        // 8. トリガー枠管理登録
        TriggerFrameManager.register();

        // 9. コマンド登録
        TrionCommand.register();

        LOGGER.info("[MineTrigger] Ready.");
    }
}
