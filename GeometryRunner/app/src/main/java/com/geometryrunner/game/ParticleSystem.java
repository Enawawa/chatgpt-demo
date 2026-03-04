package com.geometryrunner.game;

import android.graphics.Canvas;
import android.graphics.Paint;

import java.util.Random;

public class ParticleSystem {

    private static final int MAX = GameConstants.MAX_PARTICLES;

    private final float[] px, py, vx, vy;
    private final float[] life, maxLife, size;
    private final int[] color;
    private final boolean[] alive;
    private int count;
    private final Paint paint;
    private final Random random;

    public ParticleSystem() {
        px = new float[MAX];
        py = new float[MAX];
        vx = new float[MAX];
        vy = new float[MAX];
        life = new float[MAX];
        maxLife = new float[MAX];
        size = new float[MAX];
        color = new int[MAX];
        alive = new boolean[MAX];
        count = 0;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        random = new Random();
    }

    public void emit(float x, float y, int particleColor, int num, float spread, float speed) {
        for (int i = 0; i < num; i++) {
            int idx = findSlot();
            if (idx < 0) break;

            float angle = random.nextFloat() * 360f;
            float sp = speed * (0.3f + random.nextFloat() * 0.7f);
            float rad = (float) Math.toRadians(angle);

            px[idx] = x + (random.nextFloat() - 0.5f) * spread;
            py[idx] = y + (random.nextFloat() - 0.5f) * spread;
            vx[idx] = (float) Math.cos(rad) * sp;
            vy[idx] = (float) Math.sin(rad) * sp;
            life[idx] = 0;
            maxLife[idx] = 0.3f + random.nextFloat() * 0.7f;
            size[idx] = 2f + random.nextFloat() * 6f;
            color[idx] = particleColor;
            alive[idx] = true;
            count = Math.max(count, idx + 1);
        }
    }

    public void emitDirectional(float x, float y, int particleColor, int num,
                                float minAngle, float maxAngle, float speed) {
        for (int i = 0; i < num; i++) {
            int idx = findSlot();
            if (idx < 0) break;

            float angle = minAngle + random.nextFloat() * (maxAngle - minAngle);
            float sp = speed * (0.4f + random.nextFloat() * 0.6f);
            float rad = (float) Math.toRadians(angle);

            px[idx] = x + (random.nextFloat() - 0.5f) * 8f;
            py[idx] = y + (random.nextFloat() - 0.5f) * 8f;
            vx[idx] = (float) Math.cos(rad) * sp;
            vy[idx] = (float) Math.sin(rad) * sp;
            life[idx] = 0;
            maxLife[idx] = 0.2f + random.nextFloat() * 0.5f;
            size[idx] = 2f + random.nextFloat() * 5f;
            color[idx] = particleColor;
            alive[idx] = true;
            count = Math.max(count, idx + 1);
        }
    }

    public void update(float dt) {
        int maxAlive = 0;
        for (int i = 0; i < count; i++) {
            if (!alive[i]) continue;
            life[i] += dt;
            if (life[i] >= maxLife[i]) {
                alive[i] = false;
                continue;
            }
            px[i] += vx[i] * dt;
            py[i] += vy[i] * dt;
            vy[i] += 200f * dt;
            vx[i] *= 0.99f;
            maxAlive = i + 1;
        }
        count = maxAlive;
    }

    public void draw(Canvas canvas) {
        for (int i = 0; i < count; i++) {
            if (!alive[i]) continue;
            float progress = life[i] / maxLife[i];
            float alpha = 1f - progress;
            float s = size[i] * (1f - progress * 0.5f);

            paint.setColor(color[i]);
            paint.setAlpha((int) (alpha * 255));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(px[i] - s / 2, py[i] - s / 2, px[i] + s / 2, py[i] + s / 2, paint);
        }
    }

    public void clear() {
        for (int i = 0; i < MAX; i++) {
            alive[i] = false;
        }
        count = 0;
    }

    private int findSlot() {
        for (int i = 0; i < MAX; i++) {
            if (!alive[i]) return i;
        }
        return -1;
    }
}
