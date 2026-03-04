package com.geometryrunner.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

public class Obstacle {

    public enum Type {
        GROUND_SPIKE,
        TALL_SPIKE,
        FLOATING_BLOCK,
        DOUBLE_SPIKE,
        LOW_BLOCK,
        PILLAR,
    }

    private float x, y;
    private float width, height;
    private Type type;
    private int colorIndex;
    private boolean active;

    private final Paint paint;
    private final Paint outlinePaint;
    private final Path path;
    private final RectF hitbox;

    public Obstacle() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(2f);
        path = new Path();
        hitbox = new RectF();
        active = false;
    }

    public void init(Type type, float x, float groundY, int colorIndex, float screenHeight) {
        this.type = type;
        this.x = x;
        this.colorIndex = colorIndex;
        this.active = true;

        switch (type) {
            case GROUND_SPIKE:
                this.width = GameConstants.SPIKE_WIDTH;
                this.height = GameConstants.SPIKE_HEIGHT;
                this.y = groundY - height;
                break;
            case TALL_SPIKE:
                this.width = GameConstants.SPIKE_WIDTH;
                this.height = GameConstants.SPIKE_HEIGHT * 1.8f;
                this.y = groundY - height;
                break;
            case FLOATING_BLOCK:
                this.width = GameConstants.BLOCK_SIZE;
                this.height = GameConstants.BLOCK_SIZE;
                this.y = groundY - GameConstants.BLOCK_SIZE * 3.5f;
                break;
            case DOUBLE_SPIKE:
                this.width = GameConstants.SPIKE_WIDTH * 2.2f;
                this.height = GameConstants.SPIKE_HEIGHT;
                this.y = groundY - height;
                break;
            case LOW_BLOCK:
                this.width = GameConstants.BLOCK_SIZE * 2f;
                this.height = GameConstants.BLOCK_SIZE * 0.8f;
                this.y = groundY - height;
                break;
            case PILLAR:
                this.width = GameConstants.BLOCK_SIZE * 0.7f;
                this.height = GameConstants.BLOCK_SIZE * 3f;
                this.y = groundY - height;
                break;
        }
    }

    public void update(float scrollSpeed, float dt) {
        if (!active) return;
        x -= scrollSpeed * dt;
        if (x + width < -50) {
            active = false;
        }
    }

    public void draw(Canvas canvas) {
        if (!active) return;

        int baseColor = GameConstants.OBSTACLE_COLORS[colorIndex % GameConstants.OBSTACLE_COLORS.length];

        switch (type) {
            case GROUND_SPIKE:
            case TALL_SPIKE:
                drawSpike(canvas, baseColor);
                break;
            case FLOATING_BLOCK:
            case LOW_BLOCK:
                drawBlock(canvas, baseColor);
                break;
            case DOUBLE_SPIKE:
                drawDoubleSpike(canvas, baseColor);
                break;
            case PILLAR:
                drawPillar(canvas, baseColor);
                break;
        }
    }

    private void drawSpike(Canvas canvas, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(x, y, x + width, y + height,
                lighten(color, 0.2f), darken(color, 0.3f), Shader.TileMode.CLAMP));
        path.reset();
        path.moveTo(x, y + height);
        path.lineTo(x + width / 2, y);
        path.lineTo(x + width, y + height);
        path.close();
        canvas.drawPath(path, paint);
        paint.setShader(null);

        outlinePaint.setColor(lighten(color, 0.5f));
        outlinePaint.setAlpha(150);
        canvas.drawPath(path, outlinePaint);
    }

    private void drawDoubleSpike(Canvas canvas, int color) {
        float halfW = width / 2.2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);

        path.reset();
        path.moveTo(x, y + height);
        path.lineTo(x + halfW / 2, y);
        path.lineTo(x + halfW, y + height);
        path.close();
        paint.setShader(new LinearGradient(x, y, x + halfW, y + height,
                lighten(color, 0.2f), darken(color, 0.3f), Shader.TileMode.CLAMP));
        canvas.drawPath(path, paint);

        path.reset();
        path.moveTo(x + halfW * 0.8f, y + height);
        path.lineTo(x + halfW * 0.8f + halfW / 2, y + height * 0.15f);
        path.lineTo(x + halfW * 0.8f + halfW, y + height);
        path.close();
        canvas.drawPath(path, paint);
        paint.setShader(null);

        outlinePaint.setColor(lighten(color, 0.5f));
        outlinePaint.setAlpha(120);
        canvas.drawPath(path, outlinePaint);
    }

    private void drawBlock(Canvas canvas, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(x, y, x + width, y + height,
                lighten(color, 0.15f), darken(color, 0.25f), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(x, y, x + width, y + height, 4, 4, paint);
        paint.setShader(null);

        outlinePaint.setColor(lighten(color, 0.5f));
        outlinePaint.setAlpha(150);
        canvas.drawRoundRect(x, y, x + width, y + height, 4, 4, outlinePaint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAlpha(40);
        canvas.drawRect(x + 3, y + 3, x + width - 3, y + height / 2, paint);
    }

    private void drawPillar(Canvas canvas, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(x, y, x + width, y + height,
                lighten(color, 0.2f), darken(color, 0.4f), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(x, y, x + width, y + height, 3, 3, paint);
        paint.setShader(null);

        outlinePaint.setColor(lighten(color, 0.5f));
        outlinePaint.setAlpha(130);
        canvas.drawRoundRect(x, y, x + width, y + height, 3, 3, outlinePaint);

        paint.setColor(lighten(color, 0.3f));
        paint.setAlpha(60);
        canvas.drawRect(x + 2, y + 2, x + width / 2, y + height - 2, paint);
    }

    public RectF getHitbox() {
        float shrinkX, shrinkY;
        switch (type) {
            case GROUND_SPIKE:
            case TALL_SPIKE:
                shrinkX = width * 0.2f;
                shrinkY = height * 0.25f;
                hitbox.set(x + shrinkX, y + shrinkY, x + width - shrinkX, y + height);
                break;
            case DOUBLE_SPIKE:
                shrinkX = width * 0.15f;
                shrinkY = height * 0.2f;
                hitbox.set(x + shrinkX, y + shrinkY, x + width - shrinkX, y + height);
                break;
            default:
                shrinkX = width * 0.05f;
                shrinkY = height * 0.05f;
                hitbox.set(x + shrinkX, y + shrinkY, x + width - shrinkX, y + height - shrinkY);
                break;
        }
        return hitbox;
    }

    public boolean isActive() { return active; }
    public float getX() { return x; }
    public float getWidth() { return width; }

    private static int lighten(int color, float factor) {
        int r = Math.min(255, (int) (Color.red(color) + (255 - Color.red(color)) * factor));
        int g = Math.min(255, (int) (Color.green(color) + (255 - Color.green(color)) * factor));
        int b = Math.min(255, (int) (Color.blue(color) + (255 - Color.blue(color)) * factor));
        return Color.rgb(r, g, b);
    }

    private static int darken(int color, float factor) {
        int r = (int) (Color.red(color) * (1 - factor));
        int g = (int) (Color.green(color) * (1 - factor));
        int b = (int) (Color.blue(color) * (1 - factor));
        return Color.rgb(r, g, b);
    }
}
