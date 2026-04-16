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
    SWITCHBOX("switchbox");

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

    /** isTwoHanded: 両手武器かどうか */
    public boolean isTwoHanded() {
        return switch (this) {
            case ASSAULT_RIFLE, SHOTGUN, GATLING, GRENADE_GUN,
                 EAGLET, LIGHTNING, IBIS, ESCUDO -> true;
            default -> false;
        };
    }
}
