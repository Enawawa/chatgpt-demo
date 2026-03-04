package com.cocos.game;

/**
 * 这个类集中管理游戏参数，方便后续统一调参。
 */
public final class GameConstants {
    private GameConstants() {
        // 工具类不需要被实例化。
    }

    public static final float PLAYER_WIDTH_RATIO = 0.16f;
    public static final float PLAYER_HEIGHT_RATIO = 0.045f;
    public static final float PLAYER_BOTTOM_MARGIN_RATIO = 0.10f;
    public static final float PLAYER_MOVE_SPEED_PX_PER_SEC = 1400f;

    public static final float METEOR_MIN_RADIUS = 26f;
    public static final float METEOR_MAX_RADIUS = 54f;
    public static final float METEOR_BASE_MIN_SPEED = 420f;
    public static final float METEOR_BASE_MAX_SPEED = 860f;

    public static final float BASE_SPAWN_INTERVAL_SEC = 0.85f;
    public static final float MIN_SPAWN_INTERVAL_SEC = 0.26f;
    public static final float SPAWN_ACCELERATION_SEC_PER_SEC = 0.02f;

    public static final float MAX_DELTA_SEC = 0.033f;
    public static final int TARGET_FRAME_SLEEP_MS = 6;
}
