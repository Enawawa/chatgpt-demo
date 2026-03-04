#!/bin/bash
# 飞鸟闯关 - APK 构建脚本
# 使用前请设置 ANDROID_HOME 环境变量指向 Android SDK

set -e
cd "$(dirname "$0")"

echo "📦 同步游戏资源..."
# 确保 www 目录有最新文件
[ -f game.js ] && cp game.js www/
[ -f index.html ] && cp index.html www/
[ -f styles.css ] && cp styles.css www/
npx cap sync android

echo "🔨 构建 Android APK..."
cd android
if [ -z "$ANDROID_HOME" ]; then
  echo "⚠️  请设置 ANDROID_HOME 环境变量"
  echo "例如: export ANDROID_HOME=/workspace/android-sdk"
  exit 1
fi
./gradlew assembleDebug

echo "✅ 构建完成!"
echo "APK 位置: android/app/build/outputs/apk/debug/app-debug.apk"
cp app/build/outputs/apk/debug/app-debug.apk ../飞鸟闯关.apk
echo "已复制到: $(pwd)/../飞鸟闯关.apk"
