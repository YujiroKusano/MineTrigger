package com.sigulog.minetrigger;

import com.sigulog.minetrigger.weapon.WeaponType;
import com.sigulog.minetrigger.weapon.attacker.KogetsuItem;
import com.sigulog.minetrigger.weapon.attacker.RaygustItem;
import com.sigulog.minetrigger.weapon.attacker.ScorpionItem;
import com.sigulog.minetrigger.weapon.base.WeaponItem;
import com.sigulog.minetrigger.weapon.defense.EscudoItem;
import com.sigulog.minetrigger.weapon.defense.ShieldItem;
import com.sigulog.minetrigger.weapon.option.BagwormItem;
import com.sigulog.minetrigger.weapon.option.GrasshopperItem;
import com.sigulog.minetrigger.weapon.option.SpiderItem;
import com.sigulog.minetrigger.weapon.option.SwitchboxItem;
import com.sigulog.minetrigger.weapon.ranged.AsteroidItem;
import com.sigulog.minetrigger.weapon.ranged.AssaultRifleItem;
import com.sigulog.minetrigger.weapon.ranged.GatlingItem;
import com.sigulog.minetrigger.weapon.ranged.GrenadeGunItem;
import com.sigulog.minetrigger.weapon.ranged.HandgunItem;
import com.sigulog.minetrigger.weapon.ranged.HoundItem;
import com.sigulog.minetrigger.weapon.ranged.MeteoraItem;
import com.sigulog.minetrigger.weapon.ranged.RedBulletItem;
import com.sigulog.minetrigger.weapon.ranged.ShotgunItem;
import com.sigulog.minetrigger.weapon.ranged.ViperItem;
import com.sigulog.minetrigger.weapon.sniper.EagletItem;
import com.sigulog.minetrigger.weapon.sniper.IbisItem;
import com.sigulog.minetrigger.weapon.sniper.LightningItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

/**
 * 全武器トリガーのアイテム登録。
 */
public final class ModItems {

    private static final Map<WeaponType, WeaponItem> WEAPON_ITEMS = new EnumMap<>(WeaponType.class);

    // ── クリエイティブタブキー（カテゴリ別） ─────────────────────────
    public static final RegistryKey<ItemGroup> GROUP_ATTACKER =
        RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MineTriggerMod.MOD_ID, "attacker"));
    public static final RegistryKey<ItemGroup> GROUP_GUNNER =
        RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MineTriggerMod.MOD_ID, "gunner"));
    public static final RegistryKey<ItemGroup> GROUP_SHOOTER =
        RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MineTriggerMod.MOD_ID, "shooter"));
    public static final RegistryKey<ItemGroup> GROUP_SNIPER =
        RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MineTriggerMod.MOD_ID, "sniper"));
    public static final RegistryKey<ItemGroup> GROUP_DEFENSE =
        RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MineTriggerMod.MOD_ID, "defense"));
    public static final RegistryKey<ItemGroup> GROUP_OPTION =
        RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MineTriggerMod.MOD_ID, "option"));

    public static void register() {
        // ── アイテム登録 ──────────────────────────────────────────
        for (WeaponType type : WeaponType.values()) {
            WeaponItem item = createWeaponItem(type);
            Registry.register(Registries.ITEM,
                Identifier.of(MineTriggerMod.MOD_ID, type.configKey),
                item);
            WEAPON_ITEMS.put(type, item);
        }
        MineTriggerMod.LOGGER.info("[MineTrigger] {} weapon items registered.", WEAPON_ITEMS.size());

        // ── クリエイティブタブ登録（カテゴリ別） ─────────────────────
        Registry.register(Registries.ITEM_GROUP, GROUP_ATTACKER,
            FabricItemGroup.builder()
                .icon(() -> new ItemStack(WEAPON_ITEMS.get(WeaponType.KOGETSU)))
                .displayName(Text.literal("MT: アタッカー"))
                .entries((ctx, entries) ->
                    addIfPresent(entries, WeaponType.KOGETSU, WeaponType.SCORPION, WeaponType.RAYGUST))
                .build());

        Registry.register(Registries.ITEM_GROUP, GROUP_GUNNER,
            FabricItemGroup.builder()
                .icon(() -> new ItemStack(WEAPON_ITEMS.get(WeaponType.HANDGUN)))
                .displayName(Text.literal("MT: ガンナー"))
                .entries((ctx, entries) ->
                    addIfPresent(entries, WeaponType.HANDGUN, WeaponType.ASSAULT_RIFLE,
                        WeaponType.SHOTGUN, WeaponType.GATLING, WeaponType.GRENADE_GUN))
                .build());

        Registry.register(Registries.ITEM_GROUP, GROUP_SHOOTER,
            FabricItemGroup.builder()
                .icon(() -> new ItemStack(WEAPON_ITEMS.get(WeaponType.METEORA)))
                .displayName(Text.literal("MT: シューター"))
                .entries((ctx, entries) ->
                    addIfPresent(entries, WeaponType.ASTEROID, WeaponType.METEORA,
                        WeaponType.HOUND, WeaponType.VIPER, WeaponType.RED_BULLET))
                .build());

        Registry.register(Registries.ITEM_GROUP, GROUP_SNIPER,
            FabricItemGroup.builder()
                .icon(() -> new ItemStack(WEAPON_ITEMS.get(WeaponType.IBIS)))
                .displayName(Text.literal("MT: スナイパー"))
                .entries((ctx, entries) ->
                    addIfPresent(entries, WeaponType.EAGLET, WeaponType.LIGHTNING, WeaponType.IBIS))
                .build());

        Registry.register(Registries.ITEM_GROUP, GROUP_DEFENSE,
            FabricItemGroup.builder()
                .icon(() -> new ItemStack(WEAPON_ITEMS.get(WeaponType.SHIELD)))
                .displayName(Text.literal("MT: 防御"))
                .entries((ctx, entries) ->
                    addIfPresent(entries, WeaponType.SHIELD, WeaponType.ESCUDO))
                .build());

        Registry.register(Registries.ITEM_GROUP, GROUP_OPTION,
            FabricItemGroup.builder()
                .icon(() -> new ItemStack(WEAPON_ITEMS.get(WeaponType.BAGWORM)))
                .displayName(Text.literal("MT: オプション"))
                .entries((ctx, entries) ->
                    addIfPresent(entries, WeaponType.BAGWORM, WeaponType.GRASSHOPPER,
                        WeaponType.SPIDER, WeaponType.SWITCHBOX))
                .build());
    }

    private static void addIfPresent(ItemGroup.Entries entries, WeaponType... types) {
        for (WeaponType t : types) {
            WeaponItem item = WEAPON_ITEMS.get(t);
            if (item != null) entries.add(item);
        }
    }

    private static WeaponItem createWeaponItem(WeaponType type) {
        Item.Settings s = new Item.Settings().maxCount(1);
        return switch (type) {
            // ── アタッカー系 ──
            case KOGETSU   -> new KogetsuItem(type, s);
            case SCORPION  -> new ScorpionItem(type, s);
            case RAYGUST   -> new RaygustItem(type, s);

            // ── ガンナー系 ──
            case SHOTGUN      -> new ShotgunItem(type, s);
            case GRENADE_GUN  -> new GrenadeGunItem(type, s);
            case HANDGUN      -> new HandgunItem(type, s);
            case ASSAULT_RIFLE -> new AssaultRifleItem(type, s);
            case GATLING      -> new GatlingItem(type, s);

            // ── シューター系 ──
            case METEORA    -> new MeteoraItem(type, s);
            case HOUND      -> new HoundItem(type, s);
            case RED_BULLET -> new RedBulletItem(type, s);
            case ASTEROID   -> new AsteroidItem(type, s);
            case VIPER      -> new ViperItem(type, s);

            // ── スナイパー系 ──
            case EAGLET    -> new EagletItem(type, s);
            case LIGHTNING -> new LightningItem(type, s);
            case IBIS      -> new IbisItem(type, s);

            // ── 防御系 ──
            case SHIELD  -> new ShieldItem(type, s);
            case ESCUDO  -> new EscudoItem(type, s);

            // ── オプション系 ──
            case GRASSHOPPER -> new GrasshopperItem(type, s);
            case BAGWORM     -> new BagwormItem(type, s);
            case SPIDER      -> new SpiderItem(type, s);
            case SWITCHBOX   -> new SwitchboxItem(type, s);
        };
    }

    public static WeaponItem get(WeaponType type) {
        return WEAPON_ITEMS.get(type);
    }

    private ModItems() {}
}
