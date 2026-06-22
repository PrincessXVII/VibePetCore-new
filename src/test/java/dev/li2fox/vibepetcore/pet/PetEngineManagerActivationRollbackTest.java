package dev.li2fox.vibepetcore.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.li2fox.vibepetcore.player.OwnedPetData;
import dev.li2fox.vibepetcore.player.PlayerData;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PetEngineManagerActivationRollbackTest {
    @Test
    void rollbackRemovesNewlyAddedActivePetAfterSaveFailure() {
        UUID playerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        PlayerData playerData = new PlayerData(playerId);
        OwnedPetData activatedPet = new OwnedPetData(petId, playerId, "fox", "common");

        playerData.pets().add(activatedPet);
        playerData.setActivePetId(petId);

        PetEngineManager.rollbackFailedActivation(playerData, petId, Optional.empty(), null);

        assertTrue(playerData.activePetId().isEmpty());
        assertTrue(playerData.pets().isEmpty());
    }

    @Test
    void rollbackRestoresPreviousStoredPetAfterSaveFailure() {
        UUID playerId = UUID.randomUUID();
        UUID previousActiveId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        PlayerData playerData = new PlayerData(playerId);
        OwnedPetData storedPet = new OwnedPetData(petId, null, "cat", "rare");
        storedPet.setLevel(4);
        storedPet.setState("STAY");
        storedPet.progress().put("first_summon_done", 0);
        playerData.pets().add(storedPet);
        playerData.setActivePetId(previousActiveId);
        OwnedPetData snapshot = PetEngineManager.snapshotPet(storedPet);

        OwnedPetData incomingPet = new OwnedPetData(petId, playerId, "cat", "legendary");
        incomingPet.setLevel(7);
        incomingPet.setState("FOLLOW");
        incomingPet.progress().put("first_summon_done", 1);
        storedPet.copyProgressionFrom(incomingPet);
        storedPet.setState(incomingPet.state());
        playerData.setActivePetId(petId);

        PetEngineManager.rollbackFailedActivation(playerData, petId, Optional.of(previousActiveId), snapshot);

        OwnedPetData restoredPet = playerData.pets().getFirst();
        assertEquals(Optional.of(previousActiveId), playerData.activePetId());
        assertNull(restoredPet.ownerId());
        assertEquals("rare", restoredPet.rarity());
        assertEquals(4, restoredPet.level());
        assertEquals("STAY", restoredPet.state());
        assertEquals(0, restoredPet.progress().get("first_summon_done"));
    }
}
