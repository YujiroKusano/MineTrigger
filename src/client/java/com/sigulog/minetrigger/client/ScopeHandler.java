package com.sigulog.minetrigger.client;

/**
 * スコープ（ADS）の状態管理。
 *
 * isScoping()  : 現在スコープ中かどうか
 * zoomFactor() : FOVに掛ける倍率（1.0 = ノーマル、0.2 = 5倍スナイパー）
 *
 * tick() を毎tick呼び、滑らかにzoomを補間する。
 */
public final class ScopeHandler {

    /** ガンナー系ズーム倍率（FOV × この値） */
    public static final float ZOOM_GUN    = 0.42f;  // ≈ 2.4倍
    /** スナイパー系ズーム倍率 */
    public static final float ZOOM_SNIPER = 0.18f;  // ≈ 5.5倍

    private static boolean scoping = false;
    private static float targetZoom  = 1.0f;
    private static float currentZoom = 1.0f;

    /** スコープを開始する */
    public static void activate(boolean sniper) {
        scoping     = true;
        targetZoom  = sniper ? ZOOM_SNIPER : ZOOM_GUN;
    }

    /** スコープを解除する */
    public static void deactivate() {
        scoping    = false;
        targetZoom = 1.0f;
    }

    /** 毎tick呼ぶ。zoomを補間する */
    public static void tick() {
        float speed = 0.3f;
        currentZoom += (targetZoom - currentZoom) * speed;
        // 十分近ければスナップ
        if (Math.abs(currentZoom - targetZoom) < 0.001f) currentZoom = targetZoom;
    }

    public static boolean isScoping()    { return scoping || currentZoom < 0.99f; }
    public static float   zoomFactor()   { return currentZoom; }

    /** スコープオーバーレイを表示する目安（ほぼズームが完了している）か */
    public static boolean showOverlay()  { return currentZoom < 0.7f; }

    private ScopeHandler() {}
}
