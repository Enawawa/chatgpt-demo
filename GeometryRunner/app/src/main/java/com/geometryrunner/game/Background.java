package com.geometryrunner.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

import java.util.Random;

public class Background {

    private static final int NUM_STARS = 60;
    private static final int NUM_GRID_LINES_V = 20;
    private static final int NUM_GRID_LINES_H = 8;

    private final float[] starX, starY, starSize, starBrightness;
    private float gridOffset;
    private float pulsePhase;

    private final Paint bgPaint;
    private final Paint starPaint;
    private final Paint gridPaint;
    private final Paint groundPaint;
    private final Paint groundLinePaint;

    private int screenW, screenH;
    private float groundY;

    public Background() {
        starX = new float[NUM_STARS];
        starY = new float[NUM_STARS];
        starSize = new float[NUM_STARS];
        starBrightness = new float[NUM_STARS];

        bgPaint = new Paint();
        starPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        groundPaint = new Paint();
        groundLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);

        groundLinePaint.setStyle(Paint.Style.STROKE);
        groundLinePaint.setStrokeWidth(2.5f);
    }

    public void init(int screenW, int screenH, float groundY) {
        this.screenW = screenW;
        this.screenH = screenH;
        this.groundY = groundY;

        Random rand = new Random(42);
        for (int i = 0; i < NUM_STARS; i++) {
            starX[i] = rand.nextFloat() * screenW;
            starY[i] = rand.nextFloat() * (groundY * 0.8f);
            starSize[i] = 1f + rand.nextFloat() * 2.5f;
            starBrightness[i] = 0.3f + rand.nextFloat() * 0.7f;
        }

        gridOffset = 0;
        pulsePhase = 0;
    }

    public void update(float scrollSpeed, float dt) {
        gridOffset += scrollSpeed * dt * 0.3f;
        float gridSpacing = (float) screenW / NUM_GRID_LINES_V;
        if (gridOffset > gridSpacing) {
            gridOffset -= gridSpacing;
        }

        pulsePhase += dt * 2f;
        if (pulsePhase > Math.PI * 2) {
            pulsePhase -= (float) (Math.PI * 2);
        }

        for (int i = 0; i < NUM_STARS; i++) {
            starX[i] -= scrollSpeed * dt * 0.05f * (1f + starSize[i] * 0.3f);
            if (starX[i] < -5) {
                starX[i] = screenW + 5;
            }
        }
    }

    public void draw(Canvas canvas, int themeColor) {
        bgPaint.setShader(new LinearGradient(0, 0, 0, screenH,
                Color.rgb(10, 10, 30), Color.rgb(25, 25, 50), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, screenW, screenH, bgPaint);
        bgPaint.setShader(null);

        float pulse = 0.5f + 0.5f * (float) Math.sin(pulsePhase);
        int gridAlpha = (int) (20 + pulse * 15);
        gridPaint.setColor(themeColor);
        gridPaint.setAlpha(gridAlpha);

        float gridSpacingV = (float) screenW / NUM_GRID_LINES_V;
        for (int i = -1; i <= NUM_GRID_LINES_V + 1; i++) {
            float gx = i * gridSpacingV - gridOffset;
            canvas.drawLine(gx, 0, gx, groundY, gridPaint);
        }

        float gridSpacingH = groundY / NUM_GRID_LINES_H;
        for (int i = 0; i <= NUM_GRID_LINES_H; i++) {
            float gy = i * gridSpacingH;
            canvas.drawLine(0, gy, screenW, gy, gridPaint);
        }

        for (int i = 0; i < NUM_STARS; i++) {
            float twinkle = 0.6f + 0.4f * (float) Math.sin(pulsePhase * 3 + i);
            int alpha = (int) (starBrightness[i] * twinkle * 255);
            starPaint.setColor(GameConstants.STAR_COLOR);
            starPaint.setAlpha(alpha);
            canvas.drawCircle(starX[i], starY[i], starSize[i], starPaint);
        }

        groundPaint.setShader(new LinearGradient(0, groundY, 0, screenH,
                GameConstants.GROUND_COLOR, Color.rgb(20, 20, 40), Shader.TileMode.CLAMP));
        canvas.drawRect(0, groundY, screenW, screenH, groundPaint);
        groundPaint.setShader(null);

        groundLinePaint.setColor(themeColor);
        groundLinePaint.setAlpha((int) (100 + pulse * 80));
        canvas.drawLine(0, groundY, screenW, groundY, groundLinePaint);

        groundLinePaint.setAlpha((int) (30 + pulse * 20));
        groundLinePaint.setStrokeWidth(1f);
        float perspectiveSpacing = gridSpacingV;
        for (int i = -1; i <= NUM_GRID_LINES_V + 1; i++) {
            float gx = i * perspectiveSpacing - gridOffset;
            canvas.drawLine(gx, groundY, gx, screenH, groundLinePaint);
        }
        groundLinePaint.setStrokeWidth(2.5f);
    }
}
