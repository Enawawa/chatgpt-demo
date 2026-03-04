package com.geometryrunner.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Random;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private enum State { MENU, PLAYING, DEAD, PAUSED }

    private Thread gameThread;
    private volatile boolean running;
    private volatile State state;

    private int screenW, screenH;
    private float groundY;
    private float scrollSpeed;
    private float distance;
    private int score;
    private int bestScore;
    private int coins;
    private int totalCoins;
    private int attemptCount;
    private float timePlayed;

    private Player player;
    private Background background;
    private ParticleSystem particles;

    private static final int MAX_OBSTACLES = 20;
    private static final int MAX_COINS = 15;
    private final Obstacle[] obstacles = new Obstacle[MAX_OBSTACLES];
    private final Coin[] coinPool = new Coin[MAX_COINS];

    private float nextObstacleX;
    private int obstacleColorIdx;
    private int themeColorIdx;
    private float themeTimer;

    private final Random random = new Random();

    private final Paint textPaint;
    private final Paint subTextPaint;
    private final Paint buttonPaint;
    private final Paint buttonTextPaint;
    private final Paint overlayPaint;
    private final Paint scorePaint;
    private final Paint coinTextPaint;
    private final Paint barBgPaint;
    private final Paint barFillPaint;
    private final Paint borderPaint;

    private final RectF playButton = new RectF();
    private final RectF retryButton = new RectF();
    private final RectF menuButton = new RectF();

    private float menuPulse;
    private float deathTimer;
    private boolean deathParticlesEmitted;
    private float graceTimer;

    private SharedPreferences prefs;
    private Vibrator vibrator;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        prefs = context.getSharedPreferences("geometry_runner", Context.MODE_PRIVATE);
        bestScore = prefs.getInt("best_score", 0);
        totalCoins = prefs.getInt("total_coins", 0);
        attemptCount = prefs.getInt("attempts", 0);

        try {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        } catch (Exception ignored) {}

        for (int i = 0; i < MAX_OBSTACLES; i++) obstacles[i] = new Obstacle();
        for (int i = 0; i < MAX_COINS; i++) coinPool[i] = new Coin();

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(GameConstants.TEXT_COLOR);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        subTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subTextPaint.setColor(GameConstants.TEXT_COLOR);
        subTextPaint.setTextAlign(Paint.Align.CENTER);
        subTextPaint.setAlpha(180);

        buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonPaint.setStyle(Paint.Style.FILL);

        buttonTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonTextPaint.setColor(Color.WHITE);
        buttonTextPaint.setTextAlign(Paint.Align.CENTER);
        buttonTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        overlayPaint = new Paint();
        overlayPaint.setColor(Color.argb(150, 0, 0, 0));

        scorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextAlign(Paint.Align.LEFT);
        scorePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        coinTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        coinTextPaint.setColor(GameConstants.COIN_COLOR);
        coinTextPaint.setTextAlign(Paint.Align.LEFT);
        coinTextPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        barBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barBgPaint.setColor(Color.argb(80, 255, 255, 255));

        barFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setAlpha(100);

        state = State.MENU;
        menuPulse = 0;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenW = getWidth();
        screenH = getHeight();
        groundY = screenH * GameConstants.GROUND_HEIGHT_RATIO;

        float playerSize = Math.max(GameConstants.PLAYER_SIZE, screenH * 0.06f);

        player = new Player(screenW * 0.2f, groundY - playerSize / 2, playerSize);
        player.setGroundY(groundY);

        background = new Background();
        background.init(screenW, screenH, groundY);

        particles = new ParticleSystem();

        scorePaint.setTextSize(screenH * 0.045f);
        coinTextPaint.setTextSize(screenH * 0.035f);
        textPaint.setTextSize(screenH * 0.1f);
        subTextPaint.setTextSize(screenH * 0.035f);
        buttonTextPaint.setTextSize(screenH * 0.05f);

        if (!running) {
            running = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenW = width;
        screenH = height;
        groundY = screenH * GameConstants.GROUND_HEIGHT_RATIO;
        if (player != null) player.setGroundY(groundY);
        if (background != null) background.init(screenW, screenH, groundY);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        try {
            if (gameThread != null) gameThread.join(500);
        } catch (InterruptedException ignored) {}
    }

    @Override
    public void run() {
        while (running) {
            long startTime = System.currentTimeMillis();

            update();
            draw();

            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = GameConstants.FRAME_PERIOD - elapsed;
            if (sleepTime > 0) {
                try { Thread.sleep(sleepTime); } catch (InterruptedException ignored) {}
            }
        }
    }

    private void update() {
        float dt = 1f / GameConstants.TARGET_FPS;

        switch (state) {
            case MENU:
                menuPulse += dt * 3f;
                background.update(GameConstants.BASE_SCROLL_SPEED * 0.3f, dt);
                break;

            case PLAYING:
                timePlayed += dt;
                scrollSpeed = Math.min(GameConstants.MAX_SCROLL_SPEED,
                        GameConstants.BASE_SCROLL_SPEED + distance * GameConstants.SPEED_INCREMENT);
                distance += scrollSpeed * dt;
                score = (int) (distance / 10f);

                themeTimer += dt;
                if (themeTimer > 15f) {
                    themeTimer = 0;
                    themeColorIdx = (themeColorIdx + 1) % GameConstants.PLAYER_COLORS.length;
                }

                player.update(dt);
                background.update(scrollSpeed, dt);
                particles.update(dt);

                if (player.isOnGround()) {
                    int color = GameConstants.PLAYER_COLORS[player.getColorIndex()];
                    particles.emitDirectional(
                            player.getX() - player.getSize() / 2,
                            player.getY() + player.getSize() / 2,
                            color, 1, 150, 210, 80);
                }

                spawnObstacles();
                graceTimer += dt;
                for (int i = 0; i < MAX_OBSTACLES; i++) {
                    if (obstacles[i].isActive()) {
                        obstacles[i].update(scrollSpeed, dt);
                        if (graceTimer > 0.5f
                                && RectF.intersects(player.getHitbox(), obstacles[i].getHitbox())) {
                            onPlayerDeath();
                            break;
                        }
                    }
                }

                for (int i = 0; i < MAX_COINS; i++) {
                    if (coinPool[i].isActive()) {
                        coinPool[i].update(scrollSpeed, dt);
                        if (coinPool[i].checkCollision(player.getX(), player.getY(), player.getSize())) {
                            coins++;
                            int color = GameConstants.COIN_COLOR;
                            particles.emit(player.getX(), player.getY(), color, 8, 15, 150);
                        }
                    }
                }
                break;

            case DEAD:
                deathTimer += dt;
                particles.update(dt);
                background.update(scrollSpeed * Math.max(0, 1f - deathTimer * 2f), dt);

                if (!deathParticlesEmitted) {
                    deathParticlesEmitted = true;
                    int pColor = GameConstants.PLAYER_COLORS[player.getColorIndex()];
                    particles.emit(player.getX(), player.getY(), pColor, 40, 20, 400);
                    particles.emit(player.getX(), player.getY(), Color.WHITE, 20, 10, 300);
                    vibrateShort();
                }
                break;

            case PAUSED:
                break;
        }
    }

    private void draw() {
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas == null) return;
            synchronized (getHolder()) {
                drawFrame(canvas);
            }
        } finally {
            if (canvas != null) {
                try { getHolder().unlockCanvasAndPost(canvas); } catch (Exception ignored) {}
            }
        }
    }

    private void drawFrame(Canvas canvas) {
        int themeColor = GameConstants.PLAYER_COLORS[themeColorIdx % GameConstants.PLAYER_COLORS.length];

        switch (state) {
            case MENU:
                drawMenu(canvas, themeColor);
                break;
            case PLAYING:
                drawGame(canvas, themeColor);
                break;
            case DEAD:
                drawGame(canvas, themeColor);
                drawDeathOverlay(canvas, themeColor);
                break;
            case PAUSED:
                drawGame(canvas, themeColor);
                drawPauseOverlay(canvas);
                break;
        }
    }

    private void drawGame(Canvas canvas, int themeColor) {
        background.draw(canvas, themeColor);

        for (int i = 0; i < MAX_COINS; i++) {
            if (coinPool[i].isActive()) coinPool[i].draw(canvas);
        }
        for (int i = 0; i < MAX_OBSTACLES; i++) {
            if (obstacles[i].isActive()) obstacles[i].draw(canvas);
        }

        if (player.isAlive()) player.draw(canvas);
        particles.draw(canvas);

        drawHUD(canvas, themeColor);
    }

    private void drawHUD(Canvas canvas, int themeColor) {
        scorePaint.setShadowLayer(4, 2, 2, GameConstants.TEXT_SHADOW_COLOR);
        canvas.drawText(String.valueOf(score), screenW * 0.04f, screenH * 0.08f, scorePaint);

        coinTextPaint.setShadowLayer(3, 1, 1, GameConstants.TEXT_SHADOW_COLOR);
        canvas.drawText("★ " + coins, screenW * 0.04f, screenH * 0.14f, coinTextPaint);

        float barW = screenW * 0.15f;
        float barH = screenH * 0.015f;
        float barX = screenW * 0.04f;
        float barY = screenH * 0.17f;
        float speedProgress = (scrollSpeed - GameConstants.BASE_SCROLL_SPEED)
                / (GameConstants.MAX_SCROLL_SPEED - GameConstants.BASE_SCROLL_SPEED);
        speedProgress = Math.max(0, Math.min(1, speedProgress));

        canvas.drawRoundRect(barX, barY, barX + barW, barY + barH, barH / 2, barH / 2, barBgPaint);

        if (speedProgress > 0.01f) {
            barFillPaint.setShader(new LinearGradient(barX, barY, barX + barW * speedProgress, barY,
                    themeColor, Color.RED, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(barX, barY, barX + barW * speedProgress, barY + barH,
                    barH / 2, barH / 2, barFillPaint);
            barFillPaint.setShader(null);
        }
    }

    private void drawMenu(Canvas canvas, int themeColor) {
        background.draw(canvas, themeColor);

        canvas.drawRect(0, 0, screenW, screenH, overlayPaint);

        float pulse = 0.9f + 0.1f * (float) Math.sin(menuPulse);
        textPaint.setTextSize(screenH * 0.12f * pulse);
        textPaint.setColor(themeColor);
        textPaint.setShadowLayer(8, 0, 4, GameConstants.TEXT_SHADOW_COLOR);
        canvas.drawText("几何跑酷", screenW / 2f, screenH * 0.3f, textPaint);

        textPaint.setTextSize(screenH * 0.04f);
        textPaint.setColor(Color.WHITE);
        textPaint.setAlpha(200);
        canvas.drawText("GEOMETRY RUNNER", screenW / 2f, screenH * 0.37f, textPaint);
        textPaint.setAlpha(255);

        float btnW = screenW * 0.25f;
        float btnH = screenH * 0.1f;
        playButton.set(screenW / 2f - btnW / 2, screenH * 0.5f,
                screenW / 2f + btnW / 2, screenH * 0.5f + btnH);

        buttonPaint.setShader(new LinearGradient(
                playButton.left, playButton.top, playButton.right, playButton.bottom,
                themeColor, darken(themeColor, 0.3f), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(playButton, btnH / 3, btnH / 3, buttonPaint);
        buttonPaint.setShader(null);

        canvas.drawRoundRect(playButton, btnH / 3, btnH / 3, borderPaint);

        buttonTextPaint.setTextSize(screenH * 0.05f);
        canvas.drawText("▶  开始", screenW / 2f,
                playButton.centerY() + screenH * 0.018f, buttonTextPaint);

        subTextPaint.setTextSize(screenH * 0.03f);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setAlpha(140);
        canvas.drawText("最高分: " + bestScore, screenW / 2f, screenH * 0.72f, subTextPaint);
        canvas.drawText("总金币: " + totalCoins + "  |  尝试: " + attemptCount,
                screenW / 2f, screenH * 0.78f, subTextPaint);
        canvas.drawText("点击屏幕跳跃，躲避障碍物！",
                screenW / 2f, screenH * 0.88f, subTextPaint);

        subTextPaint.setTextSize(screenH * 0.022f);
        subTextPaint.setAlpha(90);
        canvas.drawText("Powered by Cocos4 Engine Inspired Design",
                screenW / 2f, screenH * 0.95f, subTextPaint);
    }

    private void drawDeathOverlay(Canvas canvas, int themeColor) {
        if (deathTimer < 0.3f) return;

        float alpha = Math.min(1f, (deathTimer - 0.3f) * 3f);
        overlayPaint.setColor(Color.argb((int) (180 * alpha), 0, 0, 0));
        canvas.drawRect(0, 0, screenW, screenH, overlayPaint);

        if (alpha < 0.5f) return;

        float textAlpha = Math.min(1f, (alpha - 0.5f) * 2f);

        textPaint.setTextSize(screenH * 0.09f);
        textPaint.setColor(Color.rgb(255, 80, 80));
        textPaint.setAlpha((int) (textAlpha * 255));
        textPaint.setShadowLayer(6, 0, 3, GameConstants.TEXT_SHADOW_COLOR);
        canvas.drawText("游戏结束", screenW / 2f, screenH * 0.25f, textPaint);

        subTextPaint.setTextSize(screenH * 0.045f);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setAlpha((int) (textAlpha * 220));
        canvas.drawText("得分: " + score, screenW / 2f, screenH * 0.36f, subTextPaint);

        if (score >= bestScore && score > 0) {
            subTextPaint.setTextSize(screenH * 0.035f);
            subTextPaint.setColor(GameConstants.COIN_COLOR);
            subTextPaint.setAlpha((int) (textAlpha * 255));
            canvas.drawText("★ 新纪录! ★", screenW / 2f, screenH * 0.42f, subTextPaint);
        }

        subTextPaint.setTextSize(screenH * 0.033f);
        subTextPaint.setColor(GameConstants.COIN_COLOR);
        subTextPaint.setAlpha((int) (textAlpha * 200));
        canvas.drawText("金币: ★ " + coins, screenW / 2f, screenH * 0.48f, subTextPaint);

        float btnW = screenW * 0.22f;
        float btnH = screenH * 0.09f;
        float gap = screenW * 0.05f;

        retryButton.set(screenW / 2f - btnW - gap / 2, screenH * 0.57f,
                screenW / 2f - gap / 2, screenH * 0.57f + btnH);
        menuButton.set(screenW / 2f + gap / 2, screenH * 0.57f,
                screenW / 2f + gap / 2 + btnW, screenH * 0.57f + btnH);

        buttonPaint.setShader(new LinearGradient(
                retryButton.left, retryButton.top, retryButton.right, retryButton.bottom,
                themeColor, darken(themeColor, 0.3f), Shader.TileMode.CLAMP));
        canvas.drawRoundRect(retryButton, btnH / 3, btnH / 3, buttonPaint);
        buttonPaint.setShader(null);

        buttonPaint.setColor(Color.rgb(80, 80, 100));
        canvas.drawRoundRect(menuButton, btnH / 3, btnH / 3, buttonPaint);

        buttonTextPaint.setTextSize(screenH * 0.04f);
        buttonTextPaint.setAlpha((int) (textAlpha * 255));
        canvas.drawText("重试", retryButton.centerX(),
                retryButton.centerY() + screenH * 0.015f, buttonTextPaint);
        canvas.drawText("菜单", menuButton.centerX(),
                menuButton.centerY() + screenH * 0.015f, buttonTextPaint);

        subTextPaint.setTextSize(screenH * 0.028f);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setAlpha((int) (textAlpha * 120));
        canvas.drawText("最高分: " + bestScore, screenW / 2f, screenH * 0.75f, subTextPaint);
    }

    private void drawPauseOverlay(Canvas canvas) {
        overlayPaint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(0, 0, screenW, screenH, overlayPaint);

        textPaint.setTextSize(screenH * 0.1f);
        textPaint.setColor(Color.WHITE);
        textPaint.setAlpha(255);
        canvas.drawText("暂停", screenW / 2f, screenH * 0.4f, textPaint);

        subTextPaint.setTextSize(screenH * 0.04f);
        subTextPaint.setColor(Color.WHITE);
        subTextPaint.setAlpha(180);
        canvas.drawText("点击继续", screenW / 2f, screenH * 0.55f, subTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;

        float tx = event.getX();
        float ty = event.getY();

        switch (state) {
            case MENU:
                if (playButton.contains(tx, ty)) {
                    startGame();
                }
                break;

            case PLAYING:
                player.jump();
                break;

            case DEAD:
                if (deathTimer > 0.5f) {
                    if (retryButton.contains(tx, ty)) {
                        startGame();
                    } else if (menuButton.contains(tx, ty)) {
                        state = State.MENU;
                    }
                }
                break;

            case PAUSED:
                state = State.PLAYING;
                break;
        }
        return true;
    }

    private void startGame() {
        state = State.PLAYING;
        score = 0;
        coins = 0;
        distance = 0;
        scrollSpeed = GameConstants.BASE_SCROLL_SPEED;
        timePlayed = 0;
        deathTimer = 0;
        deathParticlesEmitted = false;
        themeTimer = 0;
        obstacleColorIdx = 0;
        nextObstacleX = screenW + 200;
        graceTimer = 0;

        attemptCount++;
        prefs.edit().putInt("attempts", attemptCount).apply();

        float playerSize = Math.max(GameConstants.PLAYER_SIZE, screenH * 0.06f);
        player.reset(screenW * 0.2f, groundY - playerSize / 2);
        player.setColorIndex(random.nextInt(GameConstants.PLAYER_COLORS.length));

        for (int i = 0; i < MAX_OBSTACLES; i++) obstacles[i] = new Obstacle();
        for (int i = 0; i < MAX_COINS; i++) coinPool[i] = new Coin();

        particles.clear();
    }

    private void onPlayerDeath() {
        if (state != State.PLAYING) return;
        player.die();
        state = State.DEAD;
        deathTimer = 0;
        deathParticlesEmitted = false;

        if (score > bestScore) {
            bestScore = score;
            prefs.edit().putInt("best_score", bestScore).apply();
        }
        totalCoins += coins;
        prefs.edit().putInt("total_coins", totalCoins).apply();
    }

    private void spawnObstacles() {
        float farthestX = 0;
        for (int i = 0; i < MAX_OBSTACLES; i++) {
            if (obstacles[i].isActive()) {
                farthestX = Math.max(farthestX, obstacles[i].getX() + obstacles[i].getWidth());
            }
        }

        float minGap = GameConstants.MIN_OBSTACLE_GAP;
        float maxGap = GameConstants.MAX_OBSTACLE_GAP;
        float difficultyFactor = Math.min(1f, distance / 20000f);
        minGap = minGap * (1f - difficultyFactor * 0.3f);
        maxGap = maxGap * (1f - difficultyFactor * 0.25f);

        if (farthestX < screenW + 100) {
            float spawnX = Math.max(screenW + 50, farthestX + minGap + random.nextFloat() * (maxGap - minGap));

            Obstacle.Type type = pickObstacleType(difficultyFactor);
            int slotIdx = findFreeObstacle();
            if (slotIdx >= 0) {
                obstacles[slotIdx].init(type, spawnX, groundY,
                        obstacleColorIdx % GameConstants.OBSTACLE_COLORS.length, screenH);
                obstacleColorIdx++;

                if (random.nextFloat() < 0.4f) {
                    int coinSlot = findFreeCoin();
                    if (coinSlot >= 0) {
                        float coinX = spawnX - minGap * 0.5f;
                        float coinY = groundY - player.getSize() * 2.5f - random.nextFloat() * player.getSize();
                        coinPool[coinSlot].init(coinX, coinY);
                    }
                }
            }

            if (difficultyFactor > 0.3f && random.nextFloat() < difficultyFactor * 0.5f) {
                float extraX = spawnX + 80 + random.nextFloat() * 100;
                Obstacle.Type extraType = pickObstacleType(difficultyFactor);
                int extraSlot = findFreeObstacle();
                if (extraSlot >= 0) {
                    obstacles[extraSlot].init(extraType, extraX, groundY,
                            obstacleColorIdx % GameConstants.OBSTACLE_COLORS.length, screenH);
                    obstacleColorIdx++;
                }
            }
        }
    }

    private Obstacle.Type pickObstacleType(float difficulty) {
        float r = random.nextFloat();
        if (difficulty < 0.2f) {
            return r < 0.7f ? Obstacle.Type.GROUND_SPIKE : Obstacle.Type.LOW_BLOCK;
        } else if (difficulty < 0.5f) {
            if (r < 0.35f) return Obstacle.Type.GROUND_SPIKE;
            if (r < 0.55f) return Obstacle.Type.DOUBLE_SPIKE;
            if (r < 0.75f) return Obstacle.Type.LOW_BLOCK;
            return Obstacle.Type.TALL_SPIKE;
        } else {
            if (r < 0.2f) return Obstacle.Type.GROUND_SPIKE;
            if (r < 0.4f) return Obstacle.Type.DOUBLE_SPIKE;
            if (r < 0.55f) return Obstacle.Type.TALL_SPIKE;
            if (r < 0.7f) return Obstacle.Type.PILLAR;
            if (r < 0.85f) return Obstacle.Type.FLOATING_BLOCK;
            return Obstacle.Type.LOW_BLOCK;
        }
    }

    private int findFreeObstacle() {
        for (int i = 0; i < MAX_OBSTACLES; i++) {
            if (!obstacles[i].isActive()) return i;
        }
        return -1;
    }

    private int findFreeCoin() {
        for (int i = 0; i < MAX_COINS; i++) {
            if (!coinPool[i].isActive()) return i;
        }
        return -1;
    }

    public void resume() {
        if (state == State.PAUSED) {
            state = State.PLAYING;
        }
    }

    public void pause() {
        if (state == State.PLAYING) {
            state = State.PAUSED;
        }
    }

    private void vibrateShort() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(50);
            }
        } catch (Exception ignored) {}
    }

    private static int darken(int color, float factor) {
        int r = (int) (Color.red(color) * (1 - factor));
        int g = (int) (Color.green(color) * (1 - factor));
        int b = (int) (Color.blue(color) * (1 - factor));
        return Color.rgb(r, g, b);
    }
}
