package com.geometryrunner.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

public class Player {

    private float x, y;
    private float velocityY;
    private float size;
    private float rotation;
    private float rotationSpeed;
    private boolean onGround;
    private boolean alive;
    private int colorIndex;

    private final float[] trailX;
    private final float[] trailY;
    private final float[] trailRot;
    private int trailIndex;

    private final Paint paint;
    private final Paint glowPaint;
    private final Paint trailPaint;
    private final Path path;
    private final RectF hitbox;

    private float groundY;

    public Player(float startX, float startY, float size) {
        this.x = startX;
        this.y = startY;
        this.size = size;
        this.velocityY = 0;
        this.rotation = 0;
        this.rotationSpeed = 0;
        this.onGround = true;
        this.alive = true;
        this.colorIndex = 0;

        trailX = new float[GameConstants.TRAIL_LENGTH];
        trailY = new float[GameConstants.TRAIL_LENGTH];
        trailRot = new float[GameConstants.TRAIL_LENGTH];
        trailIndex = 0;

        for (int i = 0; i < GameConstants.TRAIL_LENGTH; i++) {
            trailX[i] = startX;
            trailY[i] = startY;
            trailRot[i] = 0;
        }

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        path = new Path();
        hitbox = new RectF();
    }

    public void setGroundY(float groundY) {
        this.groundY = groundY;
    }

    public void jump() {
        if (onGround && alive) {
            velocityY = GameConstants.JUMP_VELOCITY;
            onGround = false;
            rotationSpeed = 360f;
        }
    }

    public void update(float dt) {
        if (!alive) return;

        trailX[trailIndex] = x;
        trailY[trailIndex] = y;
        trailRot[trailIndex] = rotation;
        trailIndex = (trailIndex + 1) % GameConstants.TRAIL_LENGTH;

        if (!onGround) {
            velocityY += GameConstants.GRAVITY * dt;
            y += velocityY * dt;
            rotation += rotationSpeed * dt;

            if (y + size / 2 >= groundY) {
                y = groundY - size / 2;
                velocityY = 0;
                onGround = true;
                rotation = Math.round(rotation / 90f) * 90f;
                rotationSpeed = 0;
            }
        }

        if (y - size / 2 < 0) {
            y = size / 2;
            velocityY = 0;
        }
    }

    public void draw(Canvas canvas) {
        if (!alive) return;

        int playerColor = GameConstants.PLAYER_COLORS[colorIndex % GameConstants.PLAYER_COLORS.length];

        for (int i = 0; i < GameConstants.TRAIL_LENGTH; i++) {
            int idx = (trailIndex + i) % GameConstants.TRAIL_LENGTH;
            float alpha = (float) i / GameConstants.TRAIL_LENGTH * 0.5f;
            float trailSize = size * (0.4f + 0.6f * (float) i / GameConstants.TRAIL_LENGTH);

            trailPaint.setColor(playerColor);
            trailPaint.setAlpha((int) (alpha * 255));
            trailPaint.setStyle(Paint.Style.FILL);

            canvas.save();
            canvas.translate(trailX[idx], trailY[idx]);
            canvas.rotate(trailRot[idx]);
            canvas.drawRect(-trailSize / 2, -trailSize / 2, trailSize / 2, trailSize / 2, trailPaint);
            canvas.restore();
        }

        glowPaint.setColor(playerColor);
        glowPaint.setAlpha(60);
        glowPaint.setStyle(Paint.Style.FILL);
        glowPaint.setMaskFilter(null);
        canvas.drawCircle(x, y, size * 1.2f, glowPaint);

        canvas.save();
        canvas.translate(x, y);
        canvas.rotate(rotation);

        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(
                -size / 2, -size / 2, size / 2, size / 2,
                lightenColor(playerColor, 0.3f),
                darkenColor(playerColor, 0.3f),
                Shader.TileMode.CLAMP));
        canvas.drawRect(-size / 2, -size / 2, size / 2, size / 2, paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.5f);
        paint.setColor(Color.WHITE);
        paint.setAlpha(180);
        canvas.drawRect(-size / 2, -size / 2, size / 2, size / 2, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAlpha(200);
        float iconSize = size * 0.3f;
        path.reset();
        path.moveTo(-iconSize, -iconSize * 0.7f);
        path.lineTo(iconSize, 0);
        path.lineTo(-iconSize, iconSize * 0.7f);
        path.close();
        canvas.drawPath(path, paint);

        canvas.restore();
    }

    public RectF getHitbox() {
        float shrink = size * 0.15f;
        hitbox.set(x - size / 2 + shrink, y - size / 2 + shrink,
                x + size / 2 - shrink, y + size / 2 - shrink);
        return hitbox;
    }

    public void die() {
        alive = false;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getSize() { return size; }
    public int getColorIndex() { return colorIndex; }

    public void setColorIndex(int idx) {
        this.colorIndex = idx;
    }

    public void reset(float startX, float startY) {
        this.x = startX;
        this.y = startY;
        this.velocityY = 0;
        this.rotation = 0;
        this.rotationSpeed = 0;
        this.onGround = true;
        this.alive = true;
        trailIndex = 0;
        for (int i = 0; i < GameConstants.TRAIL_LENGTH; i++) {
            trailX[i] = startX;
            trailY[i] = startY;
            trailRot[i] = 0;
        }
    }

    private static int lightenColor(int color, float factor) {
        int r = Math.min(255, (int) (Color.red(color) + (255 - Color.red(color)) * factor));
        int g = Math.min(255, (int) (Color.green(color) + (255 - Color.green(color)) * factor));
        int b = Math.min(255, (int) (Color.blue(color) + (255 - Color.blue(color)) * factor));
        return Color.rgb(r, g, b);
    }

    private static int darkenColor(int color, float factor) {
        int r = (int) (Color.red(color) * (1 - factor));
        int g = (int) (Color.green(color) * (1 - factor));
        int b = (int) (Color.blue(color) * (1 - factor));
        return Color.rgb(r, g, b);
    }
}
