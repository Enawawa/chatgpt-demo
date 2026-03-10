package com.cocos.game;

/**
 * 纯数学方法放在这里，便于单元测试。
 */
public final class GameMath {
    private GameMath() {
        // 工具类不需要被实例化。
    }

    /**
     * 把值限制在 [min, max] 区间内。
     */
    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * 计算矩形是否重叠（用于碰撞检测）。
     */
    public static boolean rectsOverlap(float leftA, float topA, float rightA, float bottomA,
                                       float leftB, float topB, float rightB, float bottomB) {
        return leftA < rightB && rightA > leftB && topA < bottomB && bottomA > topB;
    }

    /**
     * 计算当前应使用的刷怪间隔。
     *
     * @param elapsedSec 从开局到现在的总秒数
     * @param baseInterval 初始间隔（秒）
     * @param minInterval 最小间隔（秒）
     * @param acceleration 每秒降低的间隔（秒）
     */
    public static float computeSpawnInterval(float elapsedSec, float baseInterval,
                                             float minInterval, float acceleration) {
        float reduced = baseInterval - elapsedSec * acceleration;
        return Math.max(minInterval, reduced);
    }
}
