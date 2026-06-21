package dev.li2fox.vibepetcore.pet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

final class RuntimePetPerformanceTest {
    @Test
    void comfortThreatCheckDelayIsStableAndJittered() {
        UUID petId = UUID.fromString("00000000-0000-0000-0000-000000000123");
        long first = RuntimePet.comfortThreatCheckDelayMillis(petId);
        long second = RuntimePet.comfortThreatCheckDelayMillis(petId);

        assertEquals(first, second);
        assertTrue(first >= 850L);
        assertTrue(first < 1_200L);
    }
}
