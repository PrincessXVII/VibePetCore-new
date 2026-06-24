package dev.li2fox.vibepetcore.egg;

import dev.li2fox.vibepetcore.player.OwnedPetData;
import java.util.Optional;

final class PetEggRecallSupport {
    private PetEggRecallSupport() {
    }

    static boolean canReceiveActivePet(Optional<OwnedPetData> coreData, OwnedPetData activePet) {
        if (activePet == null) {
            return false;
        }
        return coreData.isEmpty() || coreData.get().petId().equals(activePet.petId());
    }
}
