package dev.li2fox.vibepetcore.egg;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.li2fox.vibepetcore.player.OwnedPetData;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PetEggRecallSupportTest {
    @Test
    void matchingEmptyCoreCanReceiveActivePet() {
        OwnedPetData activePet = pet("wolf");

        assertTrue(PetEggRecallSupport.canReceiveActivePet(Optional.of(activePet), activePet));
    }

    @Test
    void genericUnreadableCoreCanReceiveActivePet() {
        assertTrue(PetEggRecallSupport.canReceiveActivePet(Optional.empty(), pet("wolf")));
    }

    @Test
    void differentPetIdOfSameTypeCannotReceiveActivePet() {
        OwnedPetData activePet = pet("wolf");
        OwnedPetData otherWolf = pet("wolf");

        assertFalse(PetEggRecallSupport.canReceiveActivePet(Optional.of(otherWolf), activePet));
    }

    @Test
    void differentPetTypeCannotReceiveActivePet() {
        OwnedPetData activePet = pet("wolf");
        OwnedPetData cat = pet("cat");

        assertFalse(PetEggRecallSupport.canReceiveActivePet(Optional.of(cat), activePet));
    }

    private OwnedPetData pet(String type) {
        return new OwnedPetData(UUID.randomUUID(), UUID.randomUUID(), type, "common");
    }
}
