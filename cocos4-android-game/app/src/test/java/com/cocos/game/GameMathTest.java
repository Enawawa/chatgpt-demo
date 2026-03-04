package com.cocos.game;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GameMathTest {

    @Test
    public void clamp_shouldStayInRange() {
        assertEquals(5f, GameMath.clamp(5f, 0f, 10f), 0.0001f);
        assertEquals(0f, GameMath.clamp(-3f, 0f, 10f), 0.0001f);
        assertEquals(10f, GameMath.clamp(88f, 0f, 10f), 0.0001f);
    }

    @Test
    public void rectsOverlap_shouldMatchCollisionRules() {
        assertTrue(GameMath.rectsOverlap(0f, 0f, 5f, 5f, 4f, 4f, 8f, 8f));
        assertFalse(GameMath.rectsOverlap(0f, 0f, 5f, 5f, 5f, 5f, 8f, 8f));
        assertFalse(GameMath.rectsOverlap(0f, 0f, 3f, 3f, 6f, 6f, 9f, 9f));
    }

    @Test
    public void computeSpawnInterval_shouldDecreaseButNeverLowerThanMin() {
        float early = GameMath.computeSpawnInterval(1f, 0.8f, 0.3f, 0.05f);
        float mid = GameMath.computeSpawnInterval(5f, 0.8f, 0.3f, 0.05f);
        float late = GameMath.computeSpawnInterval(100f, 0.8f, 0.3f, 0.05f);

        assertTrue(mid < early);
        assertEquals(0.3f, late, 0.0001f);
    }
}
