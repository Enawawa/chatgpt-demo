package com.cocos.tvparty;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GameControllerTest {

    @Test
    public void correctAnswer_shouldAddPointsToCurrentTeam() {
        GameController controller = new GameController();
        controller.startNewGame(2, fixedQuestions(), new Random(42));

        GameController.RoundOutcome outcome = controller.submitAnswer(controller.getCurrentQuestion().getCorrectIndex());

        assertEquals(GameController.OutcomeType.CORRECT, outcome.getOutcomeType());
        assertEquals(10, controller.getScore(0));
        assertEquals(0, controller.getScore(1));
    }

    @Test
    public void wrongAnswer_shouldNotAddPoints() {
        GameController controller = new GameController();
        controller.startNewGame(2, fixedQuestions(), new Random(7));

        int correct = controller.getCurrentQuestion().getCorrectIndex();
        int wrong = (correct + 1) % 4;
        GameController.RoundOutcome outcome = controller.submitAnswer(wrong);

        assertEquals(GameController.OutcomeType.WRONG, outcome.getOutcomeType());
        assertEquals(0, controller.getScore(0));
    }

    @Test
    public void moveToNextRound_shouldAlternateTeamTurns() {
        GameController controller = new GameController();
        controller.startNewGame(2, fixedQuestions(), new Random(11));
        assertEquals(0, controller.getCurrentTeamIndex());

        controller.submitTimeout();
        assertTrue(controller.moveToNextRound());

        assertEquals(1, controller.getCurrentTeamIndex());
    }

    @Test
    public void finishGame_shouldSupportWinnerAndTie() {
        GameController controller = new GameController();
        controller.startNewGame(2, fixedQuestions(), new Random(3));

        controller.submitAnswer(controller.getCurrentQuestion().getCorrectIndex());
        controller.moveToNextRound();
        controller.submitTimeout();

        assertTrue(controller.isGameFinished());
        assertEquals(0, controller.getWinnerTeamIndex());

        GameController tieController = new GameController();
        tieController.startNewGame(2, fixedQuestions(), new Random(5));
        tieController.submitTimeout();
        tieController.moveToNextRound();
        tieController.submitTimeout();

        assertTrue(tieController.isGameFinished());
        assertFalse(tieController.getWinnerTeamIndex() >= 0);
    }

    private List<Question> fixedQuestions() {
        return Arrays.asList(
                new Question("Q1", new String[]{"A1", "B1", "C1", "D1"}, 1),
                new Question("Q2", new String[]{"A2", "B2", "C2", "D2"}, 2)
        );
    }
}
