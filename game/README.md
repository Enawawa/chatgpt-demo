# 飞鸟闯关 - 跳跃大师

一款基于经典 Flappy Bird 玩法的休闲益智游戏，使用 HTML5 Canvas 开发，通过 Capacitor 打包为 Android APK。

## 游戏玩法

- **操作方式**：点击或触摸屏幕让小鸟向上飞翔
- **目标**：穿越绿色管道之间的空隙，每穿越一个管道得 1 分
- **规则**：撞到管道、地面或天花板即游戏结束
- **特色**：最高分会自动保存，挑战你的极限！

## 技术栈

- HTML5 Canvas 原生渲染
- 纯 JavaScript（无框架依赖）
- Capacitor 6 打包 Android
- 响应式设计，适配各种屏幕尺寸

## 本地运行

```bash
# 在浏览器中测试
cd game/www
python3 -m http.server 8765
# 访问 http://localhost:8765
```

## 构建 APK

```bash
cd game
npm install
npx cap add android   # 首次需要
npx cap sync android

# 需要配置 ANDROID_HOME 指向 Android SDK
export ANDROID_HOME=/path/to/android-sdk
cd android && ./gradlew assembleDebug
```

APK 输出路径：`android/app/build/outputs/apk/debug/app-debug.apk`

## 安装说明

将 `飞鸟闯关.apk` 传输到 Android 手机后，需要允许"未知来源"安装才能安装此应用。

---

灵感来源于 Cocos 引擎生态，采用轻量级 Web 技术实现，让经典玩法触手可及！
