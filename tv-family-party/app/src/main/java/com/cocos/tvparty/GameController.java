package com.cocos.tvparty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 核心规则控制器：处理回合、轮换队伍、计分和胜负判定。
 */
public final class GameController {
    public static final int TEAM_COUNT = 2;
    public static final int POINTS_PER_CORRECT = 10;

    private final int[] scores = new int[TEAM_COUNT];
    private final List<Question> roundQuestions = new ArrayList<>();

    private int totalRounds;
    private int currentRoundIndex;
    private boolean roundAnswered;
    private boolean started;

    public void startNewGame(int targetRounds, List<Question> bank, Random random) {
        if (bank == null || bank.isEmpty()) {
            throw new IllegalArgumentException("题库不能为空");
        }
        if (targetRounds <= 0) {
            throw new IllegalArgumentException("回合数必须大于 0");
        }
        totalRounds = targetRounds;
        currentRoundIndex = 0;
        roundAnswered = false;
        started = true;
        scores[0] = 0;
        scores[1] = 0;

        List<Question> shuffled = new ArrayList<>(bank);
        Collections.shuffle(shuffled, random);

        roundQuestions.clear();
        for (int i = 0; i < totalRounds; i++) {
            roundQuestions.add(shuffled.get(i % shuffled.size()));
        }
    }

    public boolean isStarted() {
        return started;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public int getCurrentRoundNumber() {
        return currentRoundIndex + 1;
    }

    public int getCurrentTeamIndex() {
        return currentRoundIndex % TEAM_COUNT;
    }

    public int getScore(int teamIndex) {
        return scores[teamIndex];
    }

    public Question getCurrentQuestion() {
        validateStarted();
        return roundQuestions.get(currentRoundIndex);
    }

    public boolean isRoundAnswered() {
        return roundAnswered;
    }

    public RoundOutcome submitAnswer(int selectedIndex) {
        validateStarted();
        if (roundAnswered) {
            throw new IllegalStateException("当前回合已提交答案");
        }
        Question q = getCurrentQuestion();
        int correctIndex = q.getCorrectIndex();
        int team = getCurrentTeamIndex();
        boolean correct = selectedIndex == correctIndex;
        if (correct) {
            scores[team] += POINTS_PER_CORRECT;
        }
        roundAnswered = true;
        return new RoundOutcome(correct ? OutcomeType.CORRECT : OutcomeType.WRONG, correctIndex, team);
    }

    public RoundOutcome submitTimeout() {
        validateStarted();
        if (roundAnswered) {
            throw new IllegalStateException("当前回合已结束");
        }
        roundAnswered = true;
        return new RoundOutcome(OutcomeType.TIMEOUT, getCurrentQuestion().getCorrectIndex(), getCurrentTeamIndex());
    }

    public boolean moveToNextRound() {
        validateStarted();
        if (!roundAnswered) {
            throw new IllegalStateException("当前回合还未结束，不能进入下一题");
        }
        if (isGameFinished()) {
            return false;
        }
        currentRoundIndex++;
        roundAnswered = false;
        return !isGameFinished();
    }

    public boolean isGameFinished() {
        validateStarted();
        return currentRoundIndex >= totalRounds - 1 && roundAnswered;
    }

    /**
     * @return 0 表示红队赢，1 表示蓝队赢，-1 表示平局
     */
    public int getWinnerTeamIndex() {
        if (scores[0] > scores[1]) {
            return 0;
        }
        if (scores[1] > scores[0]) {
            return 1;
        }
        return -1;
    }

    private void validateStarted() {
        if (!started) {
            throw new IllegalStateException("游戏尚未开始");
        }
    }

    public enum OutcomeType {
        CORRECT,
        WRONG,
        TIMEOUT
    }

    public static final class RoundOutcome {
        private final OutcomeType outcomeType;
        private final int correctIndex;
        private final int teamIndex;

        public RoundOutcome(OutcomeType outcomeType, int correctIndex, int teamIndex) {
            this.outcomeType = outcomeType;
            this.correctIndex = correctIndex;
            this.teamIndex = teamIndex;
        }

        public OutcomeType getOutcomeType() {
            return outcomeType;
        }

        public int getCorrectIndex() {
            return correctIndex;
        }

        public int getTeamIndex() {
            return teamIndex;
        }
    }
}
