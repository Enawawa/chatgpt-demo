# 跳跃方块 - Jump Blocks

基于 [Cocos4](https://github.com/cocos/cocos4) 开源生态和 [Mind Your Step](https://github.com/cocos/cocos-tutorial-mind-your-step) 教程灵感开发的经典跑酷游戏。

## 🎮 游戏玩法

- **目标**：在随机生成的方块道路上跳跃前进，别掉下去！
- **操作**：点击或触摸屏幕即可跳跃到下一个方块
- **计分**：每成功跳到一个方块得 1 分，挑战你的最高纪录！

## 🚀 快速开始

### 网页版
```bash
npm install
npm run dev    # 开发模式
npm run build  # 构建
npm run preview # 预览构建结果
```

### Android APK
APK 文件已构建完成，位于工作区根目录：`跳跃方块.apk`

安装方法：将 APK 传输到 Android 手机后直接安装即可。

## 📁 项目结构

```
cocos4-game/
├── index.html      # 游戏入口
├── game.js         # 游戏核心逻辑
├── style.css       # 样式
├── dist/           # 构建输出
└── android/        # Capacitor Android 项目
```

## 🛠 技术栈

- 纯 HTML5 Canvas + JavaScript（无框架依赖）
- Vite 构建
- Capacitor 打包为原生 Android 应用

## 📜 许可

MIT License - 基于 Cocos 开源精神
