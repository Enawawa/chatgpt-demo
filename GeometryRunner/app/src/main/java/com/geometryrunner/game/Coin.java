package com.geometryrunner.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

public class Coin {

    private float x, y;
    private float radius;
    private boolean active;
    private boolean collected;
    private float animAngle;

    private final Paint paint;
    private final Paint innerPaint;
    private final Paint glowPaint;
    private final Path starPath;

    public Coin() {
        radius = 18f;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starPath = new Path();
        active = false;
        collected = false;
    }

    public void init(float x, float y) {
        this.x = x;
        this.y = y;
        this.active = true;
        this.collected = false;
        this.animAngle = 0;
    }

    public void update(float scrollSpeed, float dt) {
        if (!active) return;
        x -= scrollSpeed * dt;
        animAngle += 180f * dt;
        if (x + radius < -20) {
            active = false;
        }
    }

    public void draw(Canvas canvas) {
        if (!active || collected) return;

        float bobY = y + (float) Math.sin(Math.toRadians(animAngle)) * 6f;
        float scaleX = (float) Math.abs(Math.cos(Math.toRadians(animAngle * 0.7f)));
        scaleX = 0.6f + scaleX * 0.4f;

        glowPaint.setColor(GameConstants.COIN_COLOR);
        glowPaint.setAlpha(40);
        canvas.drawCircle(x, bobY, radius * 1.5f, glowPaint);

        canvas.save();
        canvas.translate(x, bobY);
        canvas.scale(scaleX, 1f);

        paint.setColor(GameConstants.COIN_COLOR);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(0, 0, radius, paint);

        paint.setColor(Color.rgb(218, 165, 32));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        canvas.drawCircle(0, 0, radius - 3, paint);

        innerPaint.setColor(Color.rgb(255, 235, 100));
        innerPaint.setStyle(Paint.Style.FILL);
        innerPaint.setTextSize(radius * 1.2f);
        innerPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("★", 0, radius * 0.4f, innerPaint);

        canvas.restore();
    }

    public boolean checkCollision(float px, float py, float pSize) {
        if (!active || collected) return false;
        float dx = x - px;
        float dy = y - py;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < radius + pSize / 2) {
            collected = true;
            active = false;
            return true;
        }
        return false;
    }

    public boolean isActive() { return active && !collected; }
    public float getX() { return x; }
}
