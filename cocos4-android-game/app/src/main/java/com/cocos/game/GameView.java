package com.cocos.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * 一个简化版的“场景 + 节点更新循环”实现，思路接近常见游戏引擎（如 Cocos）的帧驱动模式。
 */
public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {
    private final SurfaceHolder holder;
    private final Object stateLock = new Object();
    private final Random random = new Random();

    private final Paint backgroundPaint = new Paint();
    private final Paint playerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint playerCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint flamePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint meteorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint helperTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint();
    private final Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF playerRect = new RectF();
    private final RectF restartButtonRect = new RectF();
    private final List<Meteor> meteors = new ArrayList<>();

    private Thread gameThread;
    private volatile boolean running;
    private long previousFrameNanos;

    private float sceneWidth;
    private float sceneHeight;
    private float playerWidth;
    private float playerHeight;
    private float playerTop;
    private float targetPlayerCenterX;
    private boolean touchActive;

    private float elapsedSec;
    private float spawnTimerSec;
    private int score;
    private boolean gameOver;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        initPaints();
    }

    private void initPaints() {
        float density = getResources().getDisplayMetrics().density;

        backgroundPaint.setColor(Color.rgb(10, 14, 26));

        playerPaint.setColor(Color.rgb(62, 173, 255));
        playerCorePaint.setColor(Color.rgb(210, 245, 255));
        flamePaint.setColor(Color.rgb(255, 182, 66));
        meteorPaint.setColor(Color.rgb(191, 112, 58));

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24f * density);
        textPaint.setFakeBoldText(true);

        helperTextPaint.setColor(Color.argb(220, 255, 255, 255));
        helperTextPaint.setTextSize(16f * density);

        overlayPaint.setColor(Color.argb(170, 0, 0, 0));

        buttonPaint.setColor(Color.rgb(58, 145, 255));
        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setTextSize(22f * density);
        buttonTextPaint.setFakeBoldText(true);
    }

    @Override
    public void run() {
        while (running) {
            if (!holder.getSurface().isValid()) {
                sleepQuietly(16);
                continue;
            }

            long now = System.nanoTime();
            if (previousFrameNanos == 0L) {
                previousFrameNanos = now;
            }
            float deltaSec = (now - previousFrameNanos) / 1_000_000_000f;
            previousFrameNanos = now;
            deltaSec = Math.min(deltaSec, GameConstants.MAX_DELTA_SEC);

            update(deltaSec);

            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                synchronized (stateLock) {
                    drawScene(canvas);
                }
                holder.unlockCanvasAndPost(canvas);
            }

            sleepQuietly(GameConstants.TARGET_FRAME_SLEEP_MS);
        }
    }

    private void update(float deltaSec) {
        synchronized (stateLock) {
            if (sceneWidth <= 0f || sceneHeight <= 0f || gameOver) {
                return;
            }

            elapsedSec += deltaSec;
            spawnTimerSec += deltaSec;

            float desiredCenterX = GameMath.clamp(
                    targetPlayerCenterX,
                    playerWidth * 0.5f,
                    sceneWidth - playerWidth * 0.5f
            );
            float distance = desiredCenterX - playerRect.centerX();
            float maxStep = GameConstants.PLAYER_MOVE_SPEED_PX_PER_SEC * deltaSec;
            float step = Math.signum(distance) * Math.min(Math.abs(distance), maxStep);
            playerRect.offset(step, 0f);

            float spawnInterval = GameMath.computeSpawnInterval(
                    elapsedSec,
                    GameConstants.BASE_SPAWN_INTERVAL_SEC,
                    GameConstants.MIN_SPAWN_INTERVAL_SEC,
                    GameConstants.SPAWN_ACCELERATION_SEC_PER_SEC
            );
            while (spawnTimerSec >= spawnInterval) {
                spawnTimerSec -= spawnInterval;
                spawnMeteor();
            }

            Iterator<Meteor> iterator = meteors.iterator();
            while (iterator.hasNext()) {
                Meteor meteor = iterator.next();
                meteor.y += meteor.speed * deltaSec;
                meteor.syncBounds();

                boolean collided = GameMath.rectsOverlap(
                        playerRect.left, playerRect.top, playerRect.right, playerRect.bottom,
                        meteor.bounds.left, meteor.bounds.top, meteor.bounds.right, meteor.bounds.bottom
                );
                if (collided) {
                    gameOver = true;
                    updateRestartButtonRect();
                    break;
                }

                if (meteor.y - meteor.radius > sceneHeight) {
                    iterator.remove();
                    score++;
                }
            }
        }
    }

    private void drawScene(Canvas canvas) {
        canvas.drawRect(0f, 0f, sceneWidth, sceneHeight, backgroundPaint);
        drawStars(canvas);

        for (Meteor meteor : meteors) {
            canvas.drawCircle(meteor.x, meteor.y, meteor.radius, meteorPaint);
        }

        canvas.drawRoundRect(playerRect, 20f, 20f, playerPaint);
        canvas.drawCircle(playerRect.centerX(), playerRect.centerY(), playerHeight * 0.20f, playerCorePaint);

        // 触控状态下给一个尾焰反馈，让手感更直观。
        if (touchActive && !gameOver) {
            float flameWidth = playerWidth * 0.14f;
            float flameHeight = playerHeight * 0.36f;
            canvas.drawRect(
                    playerRect.centerX() - flameWidth,
                    playerRect.bottom,
                    playerRect.centerX() + flameWidth,
                    playerRect.bottom + flameHeight,
                    flamePaint
            );
        }

        canvas.drawText("分数: " + score, 28f, 52f, textPaint);
        if (!gameOver && elapsedSec < 4f) {
            canvas.drawText("按住并左右滑动，躲开陨石", 28f, 92f, helperTextPaint);
        }

        if (gameOver) {
            drawGameOverOverlay(canvas);
        }
    }

    private void drawStars(Canvas canvas) {
        Paint starPaint = helperTextPaint;
        int starCount = 28;
        for (int i = 0; i < starCount; i++) {
            float x = ((i * 127f) % Math.max(sceneWidth, 1f));
            float speedFactor = 14f + (i % 5) * 6f;
            float y = (float) ((i * 217f + elapsedSec * speedFactor) % Math.max(sceneHeight, 1f));
            starPaint.setAlpha(90 + (i % 3) * 60);
            canvas.drawCircle(x, y, 1.6f + (i % 2), starPaint);
        }
    }

    private void drawGameOverOverlay(Canvas canvas) {
        canvas.drawRect(0f, 0f, sceneWidth, sceneHeight, overlayPaint);

        String title = "游戏结束";
        String subtitle = "最终分数: " + score;
        String action = "点击这里重新开始";

        float titleX = sceneWidth * 0.5f - textPaint.measureText(title) * 0.5f;
        float subtitleX = sceneWidth * 0.5f - helperTextPaint.measureText(subtitle) * 0.5f;

        canvas.drawText(title, titleX, sceneHeight * 0.42f, textPaint);
        canvas.drawText(subtitle, subtitleX, sceneHeight * 0.49f, helperTextPaint);

        updateRestartButtonRect();
        canvas.drawRoundRect(restartButtonRect, 24f, 24f, buttonPaint);

        Paint.FontMetrics metrics = buttonTextPaint.getFontMetrics();
        float centerTextY = restartButtonRect.centerY() - (metrics.ascent + metrics.descent) * 0.5f;
        float textX = restartButtonRect.centerX() - buttonTextPaint.measureText(action) * 0.5f;
        canvas.drawText(action, textX, centerTextY, buttonTextPaint);
    }

    private void updateRestartButtonRect() {
        float width = sceneWidth * 0.62f;
        float height = Math.max(84f, sceneHeight * 0.08f);
        float left = (sceneWidth - width) * 0.5f;
        float top = sceneHeight * 0.57f;
        restartButtonRect.set(left, top, left + width, top + height);
    }

    private void spawnMeteor() {
        float radius = GameConstants.METEOR_MIN_RADIUS
                + random.nextFloat() * (GameConstants.METEOR_MAX_RADIUS - GameConstants.METEOR_MIN_RADIUS);
        float minX = radius;
        float maxX = Math.max(radius + 1f, sceneWidth - radius);
        float x = minX + random.nextFloat() * (maxX - minX);

        float difficultyMultiplier = Math.min(2.8f, 1.0f + elapsedSec * 0.06f);
        float baseSpeed = GameConstants.METEOR_BASE_MIN_SPEED
                + random.nextFloat() * (GameConstants.METEOR_BASE_MAX_SPEED - GameConstants.METEOR_BASE_MIN_SPEED);
        float speed = baseSpeed * difficultyMultiplier;
        meteors.add(new Meteor(x, -radius, radius, speed));
    }

    private void configureScene(int width, int height) {
        synchronized (stateLock) {
            sceneWidth = width;
            sceneHeight = height;

            playerWidth = GameMath.clamp(width * GameConstants.PLAYER_WIDTH_RATIO, 120f, 280f);
            playerHeight = GameMath.clamp(height * GameConstants.PLAYER_HEIGHT_RATIO, 36f, 108f);
            playerTop = height * (1f - GameConstants.PLAYER_BOTTOM_MARGIN_RATIO) - playerHeight;

            placePlayerAtCenter(width * 0.5f);
            resetRound();
        }
    }

    private void placePlayerAtCenter(float centerX) {
        float left = GameMath.clamp(centerX - playerWidth * 0.5f, 0f, sceneWidth - playerWidth);
        playerRect.set(left, playerTop, left + playerWidth, playerTop + playerHeight);
        targetPlayerCenterX = playerRect.centerX();
    }

    private void resetRound() {
        meteors.clear();
        score = 0;
        elapsedSec = 0f;
        spawnTimerSec = 0f;
        gameOver = false;
        touchActive = false;
        placePlayerAtCenter(sceneWidth * 0.5f);
    }

    public void resumeGameLoop() {
        if (running) {
            return;
        }
        running = true;
        previousFrameNanos = 0L;
        gameThread = new Thread(this, "MeteorRushGameLoop");
        gameThread.start();
    }

    public void pauseGameLoop() {
        running = false;
        if (gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                gameThread = null;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        synchronized (stateLock) {
            if (sceneWidth <= 0f || sceneHeight <= 0f) {
                return true;
            }

            float x = event.getX();
            float y = event.getY();
            int action = event.getActionMasked();

            if (gameOver) {
                if (action == MotionEvent.ACTION_DOWN && restartButtonRect.contains(x, y)) {
                    resetRound();
                    performClick();
                }
                return true;
            }

            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                targetPlayerCenterX = x;
                touchActive = true;
                return true;
            }

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                touchActive = false;
                performClick();
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        configureScene(getWidth(), getHeight());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        configureScene(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        pauseGameLoop();
    }

    private void sleepQuietly(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 陨石实体。这里只保留最关键的数据，保持结构简单。
     */
    private static final class Meteor {
        float x;
        float y;
        final float radius;
        final float speed;
        final RectF bounds = new RectF();

        Meteor(float x, float y, float radius, float speed) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.speed = speed;
            syncBounds();
        }

        void syncBounds() {
            bounds.set(x - radius, y - radius, x + radius, y + radius);
        }
    }
}
