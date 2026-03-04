package com.geometryrunner.game;

import android.graphics.Color;

public final class GameConstants {

    private GameConstants() {}

    public static final int TARGET_FPS = 60;
    public static final long FRAME_PERIOD = 1000L / TARGET_FPS;

    public static final float GRAVITY = 2200f;
    public static final float JUMP_VELOCITY = -850f;
    public static final float BASE_SCROLL_SPEED = 450f;
    public static final float MAX_SCROLL_SPEED = 900f;
    public static final float SPEED_INCREMENT = 0.15f;

    public static final float PLAYER_SIZE = 50f;
    public static final float GROUND_HEIGHT_RATIO = 0.75f;

    public static final float SPIKE_WIDTH = 40f;
    public static final float SPIKE_HEIGHT = 50f;
    public static final float BLOCK_SIZE = 50f;

    public static final float MIN_OBSTACLE_GAP = 250f;
    public static final float MAX_OBSTACLE_GAP = 500f;

    public static final int TRAIL_LENGTH = 12;
    public static final int MAX_PARTICLES = 80;

    public static final int BG_COLOR = Color.rgb(20, 20, 35);
    public static final int GROUND_COLOR = Color.rgb(40, 40, 70);
    public static final int GRID_COLOR = Color.rgb(50, 50, 90);

    public static final int[] PLAYER_COLORS = {
            Color.rgb(0, 230, 255),
            Color.rgb(0, 255, 150),
            Color.rgb(255, 100, 200),
            Color.rgb(255, 200, 0),
            Color.rgb(150, 100, 255),
    };

    public static final int[] OBSTACLE_COLORS = {
            Color.rgb(255, 60, 60),
            Color.rgb(255, 100, 50),
            Color.rgb(200, 50, 200),
    };

    public static final int COIN_COLOR = Color.rgb(255, 215, 0);
    public static final int STAR_COLOR = Color.rgb(200, 200, 255);
    public static final int TEXT_COLOR = Color.WHITE;
    public static final int TEXT_SHADOW_COLOR = Color.argb(120, 0, 0, 0);
}
