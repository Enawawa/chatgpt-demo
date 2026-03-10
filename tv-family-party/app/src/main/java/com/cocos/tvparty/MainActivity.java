package com.cocos.tvparty;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Random;

/**
 * Android TV 家庭问答主界面。
 * 交互重点：只依赖方向键 + 确定键，保证遥控器可完整操作。
 */
public class MainActivity extends AppCompatActivity {
    private static final int TOTAL_ROUNDS = 10;
    private static final int ROUND_SECONDS = 20;

    private final GameController gameController = new GameController();
    private final Random random = new Random();

    private LinearLayout startContainer;
    private LinearLayout gameContainer;
    private LinearLayout resultContainer;

    private TextView roundText;
    private TextView teamTurnText;
    private TextView scoreText;
    private TextView timerText;
    private TextView questionText;
    private TextView feedbackText;

    private Button startButton;
    private Button rulesButton;
    private Button continueButton;
    private Button playAgainButton;
    private Button[] optionButtons;

    private TextView resultSummary;
    private TextView resultScore;

    private CountDownTimer roundTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        bindActions();
        startButton.requestFocus();
    }

    private void bindViews() {
        startContainer = findViewById(R.id.startContainer);
        gameContainer = findViewById(R.id.gameContainer);
        resultContainer = findViewById(R.id.resultContainer);

        roundText = findViewById(R.id.roundText);
        teamTurnText = findViewById(R.id.teamTurnText);
        scoreText = findViewById(R.id.scoreText);
        timerText = findViewById(R.id.timerText);
        questionText = findViewById(R.id.questionText);
        feedbackText = findViewById(R.id.feedbackText);

        startButton = findViewById(R.id.startButton);
        rulesButton = findViewById(R.id.rulesButton);
        continueButton = findViewById(R.id.continueButton);
        playAgainButton = findViewById(R.id.playAgainButton);

        resultSummary = findViewById(R.id.resultSummary);
        resultScore = findViewById(R.id.resultScore);

        optionButtons = new Button[]{
                findViewById(R.id.optionA),
                findViewById(R.id.optionB),
                findViewById(R.id.optionC),
                findViewById(R.id.optionD)
        };
    }

    private void bindActions() {
        startButton.setOnClickListener(v -> startMatch());
        rulesButton.setOnClickListener(v -> showRulesDialog());
        continueButton.setOnClickListener(v -> onContinueClicked());
        playAgainButton.setOnClickListener(v -> startMatch());

        for (int i = 0; i < optionButtons.length; i++) {
            final int index = i;
            optionButtons[i].setOnClickListener(v -> onOptionSelected(index));
        }
    }

    private void startMatch() {
        gameController.startNewGame(TOTAL_ROUNDS, QuestionBank.defaultQuestions(), random);
        showGameContainer();
        loadRound();
    }

    private void showRulesDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.rules_title)
                .setMessage(R.string.rules_content)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showGameContainer() {
        startContainer.setVisibility(View.GONE);
        resultContainer.setVisibility(View.GONE);
        gameContainer.setVisibility(View.VISIBLE);
    }

    private void showResultContainer() {
        startContainer.setVisibility(View.GONE);
        gameContainer.setVisibility(View.GONE);
        resultContainer.setVisibility(View.VISIBLE);
    }

    private void loadRound() {
        cancelRoundTimer();
        updateHeaderTexts();

        Question question = gameController.getCurrentQuestion();
        questionText.setText(question.getPrompt());
        String[] options = question.getOptions();
        for (int i = 0; i < optionButtons.length; i++) {
            optionButtons[i].setEnabled(true);
            optionButtons[i].setText(getString(
                    R.string.option_format,
                    optionPrefix(i),
                    options[i]
            ));
        }

        feedbackText.setText("");
        feedbackText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        continueButton.setVisibility(View.GONE);
        optionButtons[0].requestFocus();
        startRoundTimer();
    }

    private void updateHeaderTexts() {
        roundText.setText(getString(
                R.string.round_format,
                gameController.getCurrentRoundNumber(),
                gameController.getTotalRounds()
        ));
        teamTurnText.setText(getString(R.string.team_turn_format, teamName(gameController.getCurrentTeamIndex())));
        scoreText.setText(getString(
                R.string.score_format,
                teamName(0), gameController.getScore(0),
                teamName(1), gameController.getScore(1)
        ));
    }

    private void startRoundTimer() {
        timerText.setText(getString(R.string.timer_format, ROUND_SECONDS));
        roundTimer = new CountDownTimer(ROUND_SECONDS * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                int sec = (int) Math.ceil(millisUntilFinished / 1000.0);
                timerText.setText(getString(R.string.timer_format, sec));
            }

            @Override
            public void onFinish() {
                handleTimeout();
            }
        };
        roundTimer.start();
    }

    private void onOptionSelected(int selectedIndex) {
        if (!gameController.isStarted() || gameController.isRoundAnswered()) {
            return;
        }
        cancelRoundTimer();
        GameController.RoundOutcome outcome = gameController.submitAnswer(selectedIndex);
        showRoundOutcome(outcome);
    }

    private void handleTimeout() {
        if (!gameController.isStarted() || gameController.isRoundAnswered()) {
            return;
        }
        GameController.RoundOutcome outcome = gameController.submitTimeout();
        showRoundOutcome(outcome);
    }

    private void showRoundOutcome(GameController.RoundOutcome outcome) {
        for (Button optionButton : optionButtons) {
            optionButton.setEnabled(false);
        }
        timerText.setText(R.string.timer_stopped);
        updateHeaderTexts();

        Question current = gameController.getCurrentQuestion();
        String correctOption = getString(
                R.string.option_format,
                optionPrefix(outcome.getCorrectIndex()),
                current.getOptions()[outcome.getCorrectIndex()]
        );

        if (outcome.getOutcomeType() == GameController.OutcomeType.CORRECT) {
            feedbackText.setText(R.string.correct_feedback);
            feedbackText.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else if (outcome.getOutcomeType() == GameController.OutcomeType.WRONG) {
            feedbackText.setText(getString(R.string.wrong_feedback, correctOption));
            feedbackText.setTextColor(ContextCompat.getColor(this, R.color.error));
        } else {
            feedbackText.setText(getString(R.string.timeout_feedback, correctOption));
            feedbackText.setTextColor(ContextCompat.getColor(this, R.color.error));
        }

        continueButton.setVisibility(View.VISIBLE);
        continueButton.requestFocus();
    }

    private void onContinueClicked() {
        if (!gameController.isStarted() || !gameController.isRoundAnswered()) {
            return;
        }
        if (gameController.isGameFinished()) {
            showResult();
            return;
        }
        gameController.moveToNextRound();
        loadRound();
    }

    private void showResult() {
        cancelRoundTimer();
        showResultContainer();

        int winnerIndex = gameController.getWinnerTeamIndex();
        if (winnerIndex == -1) {
            resultSummary.setText(R.string.result_tie);
        } else {
            resultSummary.setText(getString(R.string.result_win_format, teamName(winnerIndex)));
        }

        resultScore.setText(getString(
                R.string.score_format,
                teamName(0), gameController.getScore(0),
                teamName(1), gameController.getScore(1)
        ));
        playAgainButton.requestFocus();
    }

    private String optionPrefix(int idx) {
        switch (idx) {
            case 0:
                return "A";
            case 1:
                return "B";
            case 2:
                return "C";
            default:
                return "D";
        }
    }

    private String teamName(int index) {
        return index == 0 ? getString(R.string.team_red) : getString(R.string.team_blue);
    }

    private void cancelRoundTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer = null;
        }
    }

    @Override
    protected void onPause() {
        cancelRoundTimer();
        super.onPause();
    }
}
