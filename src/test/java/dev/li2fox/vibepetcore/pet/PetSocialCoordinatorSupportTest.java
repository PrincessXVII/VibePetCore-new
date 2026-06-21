package dev.li2fox.vibepetcore.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class PetSocialCoordinatorSupportTest {
    @Test
    void socialPairBudgetScansAllPairsForSmallOnline() {
        assertEquals(45, PetSocialCoordinatorSupport.socialPairBudget(10));
        assertEquals(190, PetSocialCoordinatorSupport.socialPairBudget(20));
    }

    @Test
    void socialPairBudgetCapsLargeOnlineScan() {
        int allPairsAtHundredPets = 100 * 99 / 2;
        int budget = PetSocialCoordinatorSupport.socialPairBudget(100);

        assertEquals(800, budget);
        assertTrue(budget < allPairsAtHundredPets);
    }
}
