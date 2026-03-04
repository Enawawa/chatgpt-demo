/**
 * 星际跳跃 (Star Jump) - 休闲飞行游戏
 * 灵感来自经典游戏，使用纯 Canvas 实现
 */

(function() {
    'use strict';

    // ============ 游戏配置 ============
    const CONFIG = {
        gravity: 0.5,
        jumpForce: -12,
        pipeGap: 180,
        pipeWidth: 70,
        pipeSpeed: 4,
        pipeSpawnInterval: 1800,
        shipWidth: 50,
        shipHeight: 35,
        starGateWidth: 100,
        starGateGap: 150
    };

    // ============ 游戏状态 ============
    let canvas, ctx;
    let gameState = 'start'; // start, playing, gameover
    let score = 0;
    let highScore = parseInt(localStorage.getItem('starJumpHighScore') || '0');
    let lives = 3;
    let lastTime = 0;
    let lastPipeSpawn = 0;

    // 玩家飞船
    const ship = {
        x: 0,
        y: 0,
        vy: 0,
        width: CONFIG.shipWidth,
        height: CONFIG.shipHeight
    };

    // 障碍物/星门
    let obstacles = [];
    let stars = [];

    // ============ 初始化 ============
    function init() {
        canvas = document.getElementById('gameCanvas');
        ctx = canvas.getContext('2d');
        
        resizeCanvas();
        window.addEventListener('resize', resizeCanvas);
        
        // 触摸和点击事件
        canvas.addEventListener('click', handleInput);
        canvas.addEventListener('touchstart', handleTouch, { passive: false });
        
        document.getElementById('start-btn').addEventListener('click', startGame);
        document.getElementById('restart-btn').addEventListener('click', startGame);
        
        initStars();
        updateHighScoreDisplay();
        requestAnimationFrame(gameLoop);
    }

    function resizeCanvas() {
        const container = document.getElementById('game-container');
        const maxW = window.innerWidth;
        const maxH = window.innerHeight;
        const ratio = 16 / 9;
        
        let width = maxW;
        let height = maxW / ratio;
        
        if (height > maxH) {
            height = maxH;
            width = maxH * ratio;
        }
        
        canvas.width = width;
        canvas.height = height;
        canvas.style.width = width + 'px';
        canvas.style.height = height + 'px';
        
        if (gameState === 'start') {
            ship.x = width * 0.2;
            ship.y = height / 2 - ship.height / 2;
            ship.vy = 0;
        }
    }

    // ============ 星空背景 ============
    function initStars() {
        stars = [];
        const w = Math.max(canvas.width, 100);
        const h = Math.max(canvas.height, 100);
        const count = 80;
        for (let i = 0; i < count; i++) {
            stars.push({
                x: Math.random() * w,
                y: Math.random() * h,
                size: Math.random() * 2 + 0.5,
                speed: Math.random() * 1 + 0.3
            });
        }
    }

    function updateStars(dt) {
        stars.forEach(star => {
            star.x -= star.speed;
            if (star.x < 0) {
                star.x = canvas.width;
                star.y = Math.random() * canvas.height;
            }
        });
    }

    function drawStars() {
        stars.forEach(star => {
            ctx.fillStyle = `rgba(255, 255, 255, ${0.5 + star.size / 2})`;
            ctx.beginPath();
            ctx.arc(star.x, star.y, star.size, 0, Math.PI * 2);
            ctx.fill();
        });
    }

    // ============ 输入处理 ============
    function handleInput(e) {
        e.preventDefault();
        if (gameState === 'playing') {
            ship.vy = CONFIG.jumpForce;
        }
    }

    function handleTouch(e) {
        e.preventDefault();
        if (gameState === 'playing') {
            ship.vy = CONFIG.jumpForce;
        }
    }

    // ============ 游戏流程 ============
    function startGame() {
        gameState = 'playing';
        score = 0;
        lives = 3;
        obstacles = [];
        ship.x = canvas.width * 0.2;
        ship.y = canvas.height / 2 - ship.height / 2;
        ship.vy = 0;
        lastPipeSpawn = 0;
        
        document.getElementById('start-screen').style.display = 'none';
        document.getElementById('game-over-screen').style.display = 'none';
        document.getElementById('hud').style.display = 'flex';
        updateHUD();
    }

    function gameOver() {
        gameState = 'gameover';
        if (score > highScore) {
            highScore = score;
            localStorage.setItem('starJumpHighScore', highScore.toString());
        }
        
        document.getElementById('hud').style.display = 'none';
        document.getElementById('game-over-screen').style.display = 'block';
        document.getElementById('final-score').textContent = score;
        document.getElementById('final-high-score').textContent = highScore;
    }

    // ============ 障碍物 ============
    function spawnObstacle(time) {
        if (time - lastPipeSpawn < CONFIG.pipeSpawnInterval) return;
        lastPipeSpawn = time;
        
        const gap = CONFIG.starGateGap;
        const minGapTop = 80;
        const maxGapTop = canvas.height - gap - 80;
        const gapTop = minGapTop + Math.random() * (maxGapTop - minGapTop);
        const gapBottom = gapTop + gap;
        
        obstacles.push({
            x: canvas.width,
            top: 0,
            bottom: gapTop,
            gapTop: gapTop,
            gapBottom: gapBottom,
            topHeight: gapTop,
            bottomY: gapBottom,
            bottomHeight: canvas.height - gapBottom,
            width: CONFIG.starGateWidth,
            passed: false
        });
    }

    function updateObstacles(dt) {
        for (let i = obstacles.length - 1; i >= 0; i--) {
            const obs = obstacles[i];
            obs.x -= CONFIG.pipeSpeed;
            
            if (obs.x + obs.width < 0) {
                obstacles.splice(i, 1);
                continue;
            }
            
            // 检测穿过星门得分
            if (!obs.passed && obs.x + obs.width < ship.x) {
                obs.passed = true;
                score += 10;
                updateHUD();
            }
            
            // 碰撞检测
            if (checkCollision(ship, obs)) {
                lives--;
                updateHUD();
                if (lives <= 0) {
                    gameOver();
                } else {
                    // 短暂无敌时间 - 移除碰撞的障碍
                    obstacles.splice(i, 1);
                }
            }
        }
    }

    function checkCollision(ship, obs) {
        const shipLeft = ship.x;
        const shipRight = ship.x + ship.width;
        const shipTop = ship.y;
        const shipBottom = ship.y + ship.height;
        
        const obsLeft = obs.x;
        const obsRight = obs.x + obs.width;
        
        // 横向重叠检测
        if (shipRight < obsLeft || shipLeft > obsRight) return false;
        
        // 上方障碍
        if (shipTop < obs.gapTop) return true;
        // 下方障碍
        if (shipBottom > obs.gapBottom) return true;
        
        return false;
    }

    function drawObstacles() {
        obstacles.forEach(obs => {
            // 星门 - 上方
            const gradient1 = ctx.createLinearGradient(obs.x, 0, obs.x + obs.width, 0);
            gradient1.addColorStop(0, '#e94560');
            gradient1.addColorStop(0.5, '#0f3460');
            gradient1.addColorStop(1, '#e94560');
            
            ctx.fillStyle = gradient1;
            ctx.fillRect(obs.x, obs.top, obs.width, obs.topHeight);
            
            ctx.strokeStyle = '#a2d2ff';
            ctx.lineWidth = 3;
            ctx.strokeRect(obs.x, obs.top, obs.width, obs.topHeight);
            
            // 星门 - 下方
            ctx.fillStyle = gradient1;
            ctx.fillRect(obs.x, obs.bottomY, obs.width, obs.bottomHeight);
            ctx.strokeRect(obs.x, obs.bottomY, obs.width, obs.bottomHeight);
            
            // 发光效果
            ctx.shadowColor = '#e94560';
            ctx.shadowBlur = 15;
            ctx.fillStyle = 'rgba(233, 69, 96, 0.3)';
            ctx.fillRect(obs.x, obs.top, obs.width, obs.topHeight);
            ctx.fillRect(obs.x, obs.bottomY, obs.width, obs.bottomHeight);
            ctx.shadowBlur = 0;
        });
    }

    // ============ 飞船 ============
    function updateShip(dt) {
        if (gameState !== 'playing') return;
        
        ship.vy += CONFIG.gravity;
        ship.y += ship.vy;
        
        // 边界限制
        if (ship.y < 0) {
            ship.y = 0;
            ship.vy = 0;
        }
        if (ship.y + ship.height > canvas.height) {
            ship.y = canvas.height - ship.height;
            lives--;
            updateHUD();
            if (lives <= 0) {
                gameOver();
            } else {
                ship.vy = CONFIG.jumpForce;
            }
        }
    }

    function drawShip() {
        ctx.save();
        ctx.translate(ship.x + ship.width / 2, ship.y + ship.height / 2);
        const tilt = Math.min(Math.max(ship.vy * 2, -30), 30);
        ctx.rotate((tilt * Math.PI) / 180);
        ctx.translate(-(ship.x + ship.width / 2), -(ship.y + ship.height / 2));
        
        // 飞船主体 - 三角形
        ctx.beginPath();
        ctx.moveTo(ship.x + ship.width, ship.y + ship.height / 2);
        ctx.lineTo(ship.x, ship.y);
        ctx.lineTo(ship.x, ship.y + ship.height);
        ctx.closePath();
        
        const shipGradient = ctx.createLinearGradient(ship.x, ship.y, ship.x + ship.width, ship.y + ship.height);
        shipGradient.addColorStop(0, '#a2d2ff');
        shipGradient.addColorStop(0.5, '#4cc9f0');
        shipGradient.addColorStop(1, '#4361ee');
        ctx.fillStyle = shipGradient;
        ctx.fill();
        
        ctx.strokeStyle = '#e94560';
        ctx.lineWidth = 2;
        ctx.stroke();
        
        // 驾驶舱
        ctx.beginPath();
        ctx.arc(ship.x + ship.width * 0.5, ship.y + ship.height / 2, 8, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
        ctx.fill();
        ctx.strokeStyle = '#e94560';
        ctx.stroke();
        
        // 尾焰
        const flameHeight = 15 + Math.abs(ship.vy) * 2;
        ctx.beginPath();
        ctx.moveTo(ship.x + ship.width * 0.3, ship.y + ship.height / 2);
        ctx.lineTo(ship.x - 10, ship.y + ship.height / 2 - flameHeight / 2);
        ctx.lineTo(ship.x - 5, ship.y + ship.height / 2);
        ctx.lineTo(ship.x - 10, ship.y + ship.height / 2 + flameHeight / 2);
        ctx.closePath();
        ctx.fillStyle = ship.vy < 0 ? '#4cc9f0' : '#e94560';
        ctx.globalAlpha = 0.8;
        ctx.fill();
        ctx.globalAlpha = 1;
        
        ctx.restore();
    }

    // ============ UI ============
    function updateHUD() {
        document.getElementById('score-display').textContent = score;
        document.getElementById('lives-display').textContent = '❤️'.repeat(lives);
    }

    function updateHighScoreDisplay() {
        document.getElementById('high-score').textContent = highScore;
    }

    // ============ 主循环 ============
    function gameLoop(timestamp) {
        const dt = Math.min((timestamp - lastTime) / 16.67, 3);
        lastTime = timestamp;
        
        if (gameState === 'playing') {
            spawnObstacle(timestamp);
            updateShip(dt);
            updateObstacles(dt);
        }
        updateStars(dt);
        
        // 绘制
        ctx.fillStyle = '#0f0f23';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        
        drawStars();
        drawObstacles();
        drawShip();
        
        requestAnimationFrame(gameLoop);
    }

    // 启动
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
