/**
 * 跳跃方块 - 基于 Cocos Mind Your Step 灵感的经典跑酷游戏
 * 点击/触摸屏幕跳跃，避免掉入空隙！
 */

// ============ 游戏配置 ============
const CONFIG = {
  blockWidth: 60,
  blockHeight: 30,
  blockGap: 15,
  playerSize: 24,
  jumpSpeed: 18,
  gravity: 0.8,
  moveSpeed: 4,
  roadLength: 50,
  colors: {
    block: ['#e94560', '#ff6b6b', '#ffa07a'],
    blockStroke: '#c73e54',
    player: '#e94560',
    playerStroke: '#fff',
    ground: '#16213e',
  },
};

// ============ 游戏状态 ============
let canvas, ctx;
let gameState = 'menu'; // menu | playing | gameover
let score = 0;
let highScore = parseInt(localStorage.getItem('jumpBlocksHighScore') || '0');
let blocks = [];
let player = { x: 0, y: 0, vy: 0, targetIndex: 0 };
let road = [];
let animationId = null;

// ============ 初始化 ============
function init() {
  canvas = document.getElementById('game-canvas');
  ctx = canvas.getContext('2d');

  // 响应式画布
  resizeCanvas();
  window.addEventListener('resize', resizeCanvas);

  // 输入事件
  canvas.addEventListener('click', handleInput);
  canvas.addEventListener('touchstart', (e) => {
    e.preventDefault();
    handleInput(e);
  }, { passive: false });

  document.getElementById('start-btn').addEventListener('click', startGame);
  document.getElementById('restart-btn').addEventListener('click', startGame);

  updateHighScoreDisplay();
  drawMenu();
}

// ============ 画布尺寸 ============
function resizeCanvas() {
  const container = canvas.parentElement;
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  const rect = container.getBoundingClientRect();

  canvas.width = rect.width * dpr;
  canvas.height = rect.height * dpr;
  canvas.style.width = rect.width + 'px';
  canvas.style.height = rect.height + 'px';
  ctx.scale(dpr, dpr);

  if (gameState === 'menu') {
    drawMenu();
  } else if (gameState === 'playing' || gameState === 'gameover') {
    render();
  }
}

// ============ 生成道路 ============
function generateRoad() {
  road = [1]; // 1 = 有方块, 0 = 空隙
  for (let i = 1; i < CONFIG.roadLength; i++) {
    if (road[i - 1] === 0) {
      road.push(1); // 空隙后必须有方块
    } else {
      road.push(Math.random() < 0.35 ? 0 : 1);
    }
  }
}

// ============ 生成方块 ============
function generateBlocks() {
  blocks = [];
  let blockCount = 0;
  let startX = 0;

  for (let i = 0; i < road.length; i++) {
    if (road[i] === 1) {
      blockCount++;
    } else {
      if (blockCount > 0) {
        const blockWidth = blockCount * (CONFIG.blockWidth + CONFIG.blockGap) - CONFIG.blockGap;
        blocks.push({
          x: startX,
          y: 0,
          width: blockWidth,
          height: CONFIG.blockHeight,
          count: blockCount,
        });
      }
      blockCount = 0;
      startX = (i + 1) * (CONFIG.blockWidth + CONFIG.blockGap);
    }
  }
  if (blockCount > 0) {
    const blockWidth = blockCount * (CONFIG.blockWidth + CONFIG.blockGap) - CONFIG.blockGap;
    blocks.push({
      x: startX,
      y: 0,
      width: blockWidth,
      height: CONFIG.blockHeight,
      count: blockCount,
    });
  }
}

// ============ 游戏流程 ============
function startGame() {
  gameState = 'playing';
  score = 0;
  document.getElementById('start-screen').classList.add('hidden');
  document.getElementById('game-over-screen').classList.remove('visible');
  document.getElementById('score-display').textContent = '0';

  generateRoad();
  generateBlocks();

  const firstBlock = blocks[0];
  player = {
    x: firstBlock.x + firstBlock.width / 2,
    y: -CONFIG.blockHeight - CONFIG.playerSize / 2,
    vy: 0,
    targetIndex: 0,
  };

  gameLoop();
}

function handleInput(e) {
  if (gameState !== 'playing') return;
  if (player.vy !== 0) return; // 跳跃中不能再次跳跃

  const targetIndex = player.targetIndex + 1;
  if (targetIndex >= blocks.length) {
    // 已在最后一格，过关！
    if (player.targetIndex === blocks.length - 1) {
      gameState = 'gameover';
      document.getElementById('final-score').textContent = `恭喜过关！得分: ${score}`;
      document.getElementById('game-over-screen').classList.add('visible');
    }
    return;
  }

  const targetBlock = blocks[targetIndex];
  const targetX = targetBlock.x + targetBlock.width / 2;

  // 计算跳跃
  const dx = targetX - player.x;
  const distance = Math.abs(dx);
  const direction = dx > 0 ? 1 : -1;

  // 跳跃物理
  player.vy = -CONFIG.jumpSpeed;
  player.targetIndex = targetIndex;

  // 存储目标位置用于水平移动
  player.targetX = targetX;
  player.moveDirection = direction;
}

function checkGameOver() {
  const currentBlock = blocks[player.targetIndex];
  if (!currentBlock) return true;

  const blockLeft = currentBlock.x - CONFIG.blockWidth / 2;
  const blockRight = currentBlock.x + currentBlock.width + CONFIG.blockWidth / 2;
  const blockTop = -CONFIG.blockHeight - CONFIG.playerSize;

  // 玩家是否在方块上
  if (player.y >= blockTop - 5 && player.y <= blockTop + 20) {
    if (player.x >= blockLeft && player.x <= blockRight) {
      return false; // 安全着陆
    }
  }

  // 掉落检测
  if (player.y > 100) {
    return true;
  }

  return false;
}

function gameLoop() {
  if (gameState !== 'playing') return;

  const currentBlock = blocks[player.targetIndex];
  const targetX = currentBlock ? currentBlock.x + currentBlock.width / 2 : player.x;

  // 重力
  player.vy += CONFIG.gravity;
  player.y += player.vy;

  // 水平移动（跳跃时）
  if (player.vy !== 0 && player.targetX !== undefined) {
    const dx = player.targetX - player.x;
    const moveAmount = CONFIG.moveSpeed * 2;
    if (Math.abs(dx) > moveAmount) {
      player.x += Math.sign(dx) * moveAmount;
    } else {
      player.x = player.targetX;
    }
  }

  // 着陆检测
  if (currentBlock && player.vy > 0) {
    const blockTop = -CONFIG.blockHeight - CONFIG.playerSize / 2;
    const blockLeft = currentBlock.x - 10;
    const blockRight = currentBlock.x + currentBlock.width + 10;

    if (player.y >= blockTop - 5 && player.y <= blockTop + 15) {
      if (player.x >= blockLeft && player.x <= blockRight) {
        player.y = blockTop;
        player.vy = 0;
        player.targetX = undefined;
        score = player.targetIndex + 1;
        document.getElementById('score-display').textContent = score;

        if (score > highScore) {
          highScore = score;
          localStorage.setItem('jumpBlocksHighScore', highScore);
          updateHighScoreDisplay();
        }
      }
    }
  }

  // 游戏结束检测
  if (checkGameOver()) {
    gameState = 'gameover';
    document.getElementById('final-score').textContent = `得分: ${score}`;
    document.getElementById('game-over-screen').classList.add('visible');
    return;
  }

  render();
  animationId = requestAnimationFrame(gameLoop);
}

// ============ 渲染 ============
function render() {
  const w = canvas.width / (window.devicePixelRatio || 1);
  const h = canvas.height / (window.devicePixelRatio || 1);
  const centerY = h / 2;

  ctx.clearRect(0, 0, w, h);

  // 视口偏移（跟随玩家）
  const offsetX = w / 2 - player.x;
  const offsetY = centerY - player.y;

  ctx.save();
  ctx.translate(offsetX, offsetY);

  // 绘制方块
  blocks.forEach((block, i) => {
    const colorIndex = i % CONFIG.colors.block.length;
    const gradient = ctx.createLinearGradient(
      block.x, 0, block.x + block.width, 0
    );
    gradient.addColorStop(0, CONFIG.colors.block[colorIndex]);
    gradient.addColorStop(1, CONFIG.colors.block[(colorIndex + 1) % CONFIG.colors.block.length]);

    ctx.fillStyle = gradient;
    ctx.strokeStyle = CONFIG.colors.blockStroke;
    ctx.lineWidth = 2;

    ctx.beginPath();
    if (ctx.roundRect) {
      ctx.roundRect(block.x, -CONFIG.blockHeight, block.width, CONFIG.blockHeight, 4);
    } else {
      ctx.rect(block.x, -CONFIG.blockHeight, block.width, CONFIG.blockHeight);
    }
    ctx.fill();
    ctx.stroke();
  });

  // 绘制玩家
  ctx.fillStyle = CONFIG.colors.player;
  ctx.strokeStyle = CONFIG.colors.playerStroke;
  ctx.lineWidth = 3;

  ctx.beginPath();
  ctx.arc(player.x, player.y, CONFIG.playerSize / 2, 0, Math.PI * 2);
  ctx.fill();
  ctx.stroke();

  ctx.restore();
}

function drawMenu() {
  const w = canvas.width / (window.devicePixelRatio || 1);
  const h = canvas.height / (window.devicePixelRatio || 1);

  ctx.clearRect(0, 0, w, h);

  // 背景装饰
  const gradient = ctx.createLinearGradient(0, 0, w, h);
  gradient.addColorStop(0, '#1a1a2e');
  gradient.addColorStop(1, '#16213e');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, w, h);

  // 装饰性方块
  ctx.fillStyle = 'rgba(233, 69, 96, 0.2)';
  for (let i = 0; i < 5; i++) {
    ctx.fillRect(50 + i * 70, h / 2 - 50 + i * 15, 50, 20);
  }
}

function updateHighScoreDisplay() {
  document.getElementById('high-score').textContent = `最高: ${highScore}`;
}

// ============ 启动 ============
init();
