# Cocos4 Meteor Rush (Android)

这是一个基于 `cocos/cocos4` 项目思路实现的 Android 小游戏：

- 包名沿用了 Cocos 模板常见命名风格：`com.cocos.game`
- 游戏循环使用“每帧 update + render”的引擎式结构
- 玩法是触控躲避类：左右滑动飞船，躲开陨石并累计分数

## 控制方式

- 按住屏幕左右滑动：移动飞船
- 被陨石撞到：游戏结束
- 点击“重新开始”按钮：重开一局

## 本地构建

1. 配置 `ANDROID_SDK_ROOT`
2. 在项目根目录执行：

```bash
./gradlew test
./gradlew assembleDebug
```

## 产物路径

`app/build/outputs/apk/debug/app-debug.apk`
