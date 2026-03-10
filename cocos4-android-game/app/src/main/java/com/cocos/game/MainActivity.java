package com.cocos.game;

import android.app.Activity;
import android.os.Bundle;

/**
 * 入口 Activity。职责很单一：托管 GameView 的生命周期。
 */
public class MainActivity extends Activity {
    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resumeGameLoop();
    }

    @Override
    protected void onPause() {
        gameView.pauseGameLoop();
        super.onPause();
    }
}
