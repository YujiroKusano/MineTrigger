package com.sigulog.minetrigger.weapon;

/**
 * 全武器トリガーの種別一覧。
 * configKey は config.yml の weapons.* キーに対応する。
 */
public enum WeaponType {

    // ── アタッカー系 ──
    KOGETSU("kogetsu"),
    SCORPION("scorpion"),
    RAYGUST("raygust"),

    // ── ガンナー系 ──
    HANDGUN("handgun"),
    ASSAULT_RIFLE("assault_rifle"),
    SHOTGUN("shotgun"),
    GATLING("gatling"),
    GRENADE_GUN("grenade_gun"),

    // ── シューター系 ──
    ASTEROID("asteroid"),
    METEORA("meteora"),
    HOUND("hound"),
    VIPER("viper"),
    RED_BULLET("red_bullet"),

    // ── スナイパー系 ──
    EAGLET("eaglet"),
    LIGHTNING("lightning"),
    IBIS("ibis"),

    // ── 防御系 ──
    SHIELD("shield"),
    ESCUDO("escudo"),

    // ── オプション系 ──
    BAGWORM("bagworm"),
    GRASSHOPPER("grasshopper"),
    SPIDER("spider"),
    SWITCHBOX("switchbox"),

    // ── 合成弾 ──
    COMPOSITE_ROUND("composite_round");

    /** config.yml の weapons（またはoptions）下のキー名 */
    public final String configKey;

    WeaponType(String configKey) {
        this.configKey = configKey;
    }

    /** isOption: オプション系（Qキー用スロット）かどうか */
    public boolean isOption() {
        return switch (this) {
            case BAGWORM, GRASSHOPPER, SPIDER, SWITCHBOX -> true;
            default -> false;
        };
    }

    /** isGunnerGun: ガンナー系銃（弾薬設定対象）かどうか */
    public boolean isGunnerGun() {
        return switch (this) {
            case HANDGUN, ASSAULT_RIFLE, SHOTGUN, GATLING, GRENADE_GUN -> true;
            default -> false;
        };
    }

    /** isGunnerAmmo: ガンナー弾薬として設定可能な弾種かどうか */
    public boolean isGunnerAmmo() {
        return switch (this) {
            case ASTEROID, METEORA, VIPER, HOUND -> true;
            default -> false;
        };
    }

    /** isSniperAmmo: スナイパー弾薬として設定可能な弾種かどうか */
    public boolean isSniperAmmo() {
        return this == RED_BULLET;
    }

    /** isGun: スコープ対応の銃系武器かどうか（スナイパーのみ） */
    public boolean isGun() {
        return isSniper();
    }

    /** isSniper: スナイパー系（高倍率スコープ）かどうか */
    public boolean isSniper() {
        return switch (this) {
            case EAGLET, LIGHTNING, IBIS -> true;
            default -> false;
        };
    }

    /** isTwoHanded: 両手武器かどうか */
    public boolean isTwoHanded() {
        return switch (this) {
            case ASSAULT_RIFLE, SHOTGUN, GATLING, GRENADE_GUN,
                 EAGLET, LIGHTNING, IBIS, ESCUDO -> true;
            default -> false;
        };
    }

    /** configKey からWeaponTypeを検索する（null = 未知のキー） */
    @org.jetbrains.annotations.Nullable
    public static WeaponType fromConfigKey(String key) {
        if (key == null || key.isEmpty()) return null;
        for (WeaponType t : values()) {
            if (t.configKey.equals(key)) return t;
        }
        return null;
    }
}
