package dev.li2fox.vibepetcore.pet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PetStuckSupportTest {
    @Test
    void resetTrackingForUsefulMovementButNotWhenMovingAway() {
        assertTrue(PetStuckSupport.shouldResetTracking(true, true, false));
        assertTrue(PetStuckSupport.shouldResetTracking(true, false, false));
        assertFalse(PetStuckSupport.shouldResetTracking(true, false, true));
        assertFalse(PetStuckSupport.shouldResetTracking(false, false, false));
    }
}
