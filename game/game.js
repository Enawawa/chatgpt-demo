/**
 * 飞鸟闯关 - 跳跃大师
 * 基于经典 Flappy Bird 玩法的休闲游戏
 * 点击/触摸让小鸟飞翔，穿越管道得分
 */

(function() {
  'use strict';

  // ============ 游戏配置 ============
  const CONFIG = {
    GRAVITY: 0.5,
    FLAP_STRENGTH: -9,
    PIPE_GAP: 150,
    PIPE_WIDTH: 60,
    PIPE_SPEED: 3,
    PIPE_SPAWN_INTERVAL: 1800,
    BIRD_SIZE: 30,
    GROUND_HEIGHT: 80,
    STORAGE_KEY: 'flybird_best_score'
  };

  // ============ DOM 元素 ============
  let canvas, ctx;
  let startScreen, gameoverScreen, scoreDisplay;
  let startBtn, restartBtn;

  // ============ 游戏状态 ============
  let gameState = 'start'; // 'start' | 'playing' | 'gameover'
  let bird = { x: 0, y: 0, velocity: 0, rotation: 0 };
  let pipes = [];
  let score = 0;
  let bestScore = 0;
  let lastPipeSpawn = 0;
  let animationId = null;
  let lastTapTime = 0;
  let gameWidth = 0;
  let gameHeight = 0;

  // ============ 初始化 ============
  function init() {
    canvas = document.getElementById('game-canvas');
    ctx = canvas.getContext('2d');
    
    startScreen = document.getElementById('start-screen');
    gameoverScreen = document.getElementById('gameover-screen');
    scoreDisplay = document.getElementById('score-display');
    startBtn = document.getElementById('start-btn');
    restartBtn = document.getElementById('restart-btn');

    // 加载最高分
    bestScore = parseInt(localStorage.getItem(CONFIG.STORAGE_KEY) || '0', 10);

    // 设置画布尺寸
    resizeCanvas();

    // 事件监听
    window.addEventListener('resize', resizeCanvas);
    window.addEventListener('orientationchange', resizeCanvas);
    
    // 触摸和点击事件
    canvas.addEventListener('touchstart', handleInput, { passive: false });
    canvas.addEventListener('mousedown', handleInput);
    startBtn.addEventListener('click', startGame);
    restartBtn.addEventListener('click', startGame);

    // 防止双击缩放
    document.addEventListener('touchstart', preventDoubleTap, { passive: false });

    // 绘制初始画面
    drawStartScreen();
  }

  function preventDoubleTap(e) {
    const now = Date.now();
    if (now - lastTapTime < 300) {
      e.preventDefault();
    }
    lastTapTime = now;
  }

  function resizeCanvas() {
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const rect = canvas.getBoundingClientRect();
    gameWidth = rect.width;
    gameHeight = rect.height;
    
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.scale(dpr, dpr);
    
    canvas.style.width = rect.width + 'px';
    canvas.style.height = rect.height + 'px';

    if (gameState === 'start') {
      drawStartScreen();
    }
  }

  function handleInput(e) {
    e.preventDefault();
    if (gameState === 'playing') {
      flap();
    }
  }

  // ============ 游戏逻辑 ============
  function startGame() {
    // 确保画布尺寸已正确获取
    resizeCanvas();
    if (gameWidth <= 0 || gameHeight <= 0) {
      gameWidth = canvas.getBoundingClientRect().width || 375;
      gameHeight = canvas.getBoundingClientRect().height || 667;
    }

    gameState = 'playing';
    score = 0;
    pipes = [];
    lastPipeSpawn = 0;

    // 重置小鸟位置
    bird.x = gameWidth / 4;
    bird.y = gameHeight / 2 - CONFIG.GROUND_HEIGHT;
    bird.velocity = 0;
    bird.rotation = 0;

    // 切换界面
    startScreen.classList.add('hidden');
    gameoverScreen.classList.add('hidden');
    scoreDisplay.classList.remove('hidden');
    scoreDisplay.textContent = '0';

    // 开始游戏循环
    lastPipeSpawn = performance.now();
    gameLoop(performance.now());
  }

  function flap() {
    if (gameState !== 'playing') return;
    bird.velocity = CONFIG.FLAP_STRENGTH;
    bird.rotation = -25;
  }

  function spawnPipe(timestamp) {
    const gap = CONFIG.PIPE_GAP;
    const minHeight = 80;
    const maxHeight = gameHeight - CONFIG.GROUND_HEIGHT - gap - minHeight;
    const topHeight = minHeight + Math.random() * (maxHeight - minHeight);
    
    pipes.push({
      x: gameWidth,
      topHeight: topHeight,
      bottomY: topHeight + gap,
      passed: false
    });
  }

  function update(timestamp) {
    if (gameState !== 'playing') return;

    // 重力
    bird.velocity += CONFIG.GRAVITY;
    bird.y += bird.velocity;
    bird.rotation = Math.min(bird.velocity * 3, 90);

    // 生成管道
    if (timestamp - lastPipeSpawn > CONFIG.PIPE_SPAWN_INTERVAL) {
      spawnPipe(timestamp);
      lastPipeSpawn = timestamp;
    }

    // 更新管道
    for (let i = pipes.length - 1; i >= 0; i--) {
      pipes[i].x -= CONFIG.PIPE_SPEED;

      // 检测得分
      if (!pipes[i].passed && pipes[i].x + CONFIG.PIPE_WIDTH < bird.x) {
        pipes[i].passed = true;
        score++;
        scoreDisplay.textContent = score;
        if (score > bestScore) {
          bestScore = score;
          localStorage.setItem(CONFIG.STORAGE_KEY, bestScore.toString());
        }
      }

      // 移除屏幕外的管道
      if (pipes[i].x + CONFIG.PIPE_WIDTH < 0) {
        pipes.splice(i, 1);
      }
    }

    // 碰撞检测
    if (checkCollision()) {
      gameOver();
    }
  }

  function checkCollision() {
    // 撞到地面或天花板
    if (bird.y - CONFIG.BIRD_SIZE/2 < 0 || 
        bird.y + CONFIG.BIRD_SIZE/2 > gameHeight - CONFIG.GROUND_HEIGHT) {
      return true;
    }

    // 与管道碰撞
    const birdLeft = bird.x - CONFIG.BIRD_SIZE/2;
    const birdRight = bird.x + CONFIG.BIRD_SIZE/2;
    const birdTop = bird.y - CONFIG.BIRD_SIZE/2;
    const birdBottom = bird.y + CONFIG.BIRD_SIZE/2;

    for (const pipe of pipes) {
      const pipeLeft = pipe.x;
      const pipeRight = pipe.x + CONFIG.PIPE_WIDTH;

      // 上下管道
      if (birdRight > pipeLeft && birdLeft < pipeRight) {
        if (birdTop < pipe.topHeight || birdBottom > pipe.bottomY) {
          return true;
        }
      }
    }

    return false;
  }

  function gameOver() {
    gameState = 'gameover';
    cancelAnimationFrame(animationId);

    // 显示结束界面
    document.getElementById('final-score').textContent = score;
    document.getElementById('best-score').textContent = bestScore;
    scoreDisplay.classList.add('hidden');
    gameoverScreen.classList.remove('hidden');
  }

  // ============ 渲染 ============
  function drawStartScreen() {
    if (gameWidth === 0 || gameHeight === 0) return;
    ctx.clearRect(0, 0, gameWidth, gameHeight);
    drawBackground();
    drawGround();
    // 绘制示例小鸟
    const centerX = gameWidth / 2;
    const centerY = gameHeight / 2 - 50;
    ctx.save();
    ctx.translate(centerX, centerY);
    drawBird(0);
    ctx.restore();
  }

  function drawBackground() {
    const gradient = ctx.createLinearGradient(0, 0, 0, gameHeight);
    gradient.addColorStop(0, '#87CEEB');
    gradient.addColorStop(1, '#E0F6FF');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, gameWidth, gameHeight);

    // 云朵
    ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
    drawCloud(gameWidth * 0.2, gameHeight * 0.2, 40);
    drawCloud(gameWidth * 0.6, gameHeight * 0.35, 30);
    drawCloud(gameWidth * 0.8, gameHeight * 0.15, 35);
  }

  function drawCloud(x, y, size) {
    ctx.beginPath();
    ctx.arc(x, y, size * 0.5, 0, Math.PI * 2);
    ctx.arc(x + size * 0.5, y - size * 0.2, size * 0.6, 0, Math.PI * 2);
    ctx.arc(x + size, y, size * 0.5, 0, Math.PI * 2);
    ctx.fill();
  }

  function drawGround() {
    const groundY = gameHeight - CONFIG.GROUND_HEIGHT;
    
    // 草地
    ctx.fillStyle = '#8B7355';
    ctx.fillRect(0, groundY, gameWidth, CONFIG.GROUND_HEIGHT);
    
    ctx.fillStyle = '#27AE60';
    ctx.fillRect(0, groundY, gameWidth, 20);
    
    // 草地纹理
    ctx.strokeStyle = '#2ECC71';
    ctx.lineWidth = 2;
    for (let i = 0; i < gameWidth; i += 30) {
      ctx.beginPath();
      ctx.moveTo(i, groundY + 25);
      ctx.lineTo(i + 15, groundY + 15);
      ctx.stroke();
    }
  }

  function drawBird(rotation) {
    ctx.save();
    ctx.rotate(rotation * Math.PI / 180);

    // 小鸟身体 - 黄色
    ctx.fillStyle = '#F1C40F';
    ctx.beginPath();
    ctx.ellipse(0, 0, CONFIG.BIRD_SIZE/2, CONFIG.BIRD_SIZE * 0.6, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#D4AC0D';
    ctx.lineWidth = 2;
    ctx.stroke();

    // 眼睛 - 白色
    ctx.fillStyle = 'white';
    ctx.beginPath();
    ctx.arc(8, -5, 6, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();

    // 瞳孔
    ctx.fillStyle = '#2C3E50';
    ctx.beginPath();
    ctx.arc(10, -5, 3, 0, Math.PI * 2);
    ctx.fill();

    // 嘴巴
    ctx.strokeStyle = '#E67E22';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(12, 0);
    ctx.lineTo(22, 2);
    ctx.stroke();

    ctx.restore();
  }

  function drawPipes() {
    for (const pipe of pipes) {
      // 管道 - 绿色
      const pipeGradient = ctx.createLinearGradient(pipe.x, 0, pipe.x + CONFIG.PIPE_WIDTH, 0);
      pipeGradient.addColorStop(0, '#27AE60');
      pipeGradient.addColorStop(0.5, '#2ECC71');
      pipeGradient.addColorStop(1, '#229954');

      // 上管道
      ctx.fillStyle = pipeGradient;
      ctx.fillRect(pipe.x, 0, CONFIG.PIPE_WIDTH, pipe.topHeight);
      ctx.strokeStyle = '#1E8449';
      ctx.lineWidth = 3;
      ctx.strokeRect(pipe.x, 0, CONFIG.PIPE_WIDTH, pipe.topHeight);

      // 上管道帽子
      ctx.fillRect(pipe.x - 5, pipe.topHeight - 25, CONFIG.PIPE_WIDTH + 10, 25);
      ctx.strokeRect(pipe.x - 5, pipe.topHeight - 25, CONFIG.PIPE_WIDTH + 10, 25);

      // 下管道
      ctx.fillStyle = pipeGradient;
      ctx.fillRect(pipe.x, pipe.bottomY, CONFIG.PIPE_WIDTH, gameHeight - pipe.bottomY - CONFIG.GROUND_HEIGHT);
      ctx.strokeRect(pipe.x, pipe.bottomY, CONFIG.PIPE_WIDTH, gameHeight - pipe.bottomY - CONFIG.GROUND_HEIGHT);

      // 下管道帽子
      ctx.fillRect(pipe.x - 5, pipe.bottomY, CONFIG.PIPE_WIDTH + 10, 25);
      ctx.strokeRect(pipe.x - 5, pipe.bottomY, CONFIG.PIPE_WIDTH + 10, 25);
    }
  }

  function gameLoop(timestamp) {
    update(timestamp);

    // 渲染
    drawBackground();
    drawPipes();
    drawGround();
    
    ctx.save();
    ctx.translate(bird.x, bird.y);
    drawBird(bird.rotation);
    ctx.restore();

    if (gameState === 'playing') {
      animationId = requestAnimationFrame(gameLoop);
    }
  }

  // ============ 启动 ============
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
