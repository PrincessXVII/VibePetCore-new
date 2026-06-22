package dev.li2fox.vibepetcore.box;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.li2fox.vibepetcore.player.PlayerData;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class BoxManagerBasicOpenRollbackTest {
    @Test
    void rollbackRestoresPaidOpenStateAfterSaveFailure() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        data.addPoints(100L);
        data.setFreeBoxNextAtMillis(10_000L);
        data.addExtraBoxAttempt();
        data.addExtraBoxAttempt();
        data.boxPity().put("basic", 4);
        BoxManager.BasicOpenStateSnapshot snapshot = BoxManager.snapshotBasicOpenState(data);

        data.takePoints(25L);
        data.setFreeBoxNextAtMillis(20_000L);
        data.takeExtraBoxAttempt();
        data.boxPity().put("basic", 0);

        BoxManager.rollbackBasicOpenState(data, snapshot);

        assertEquals(100L, data.points());
        assertEquals(10_000L, data.freeBoxNextAtMillis());
        assertEquals(2, data.extraBoxAttempts());
        assertEquals(4, data.boxPity().get("basic"));
    }

    @Test
    void rollbackRemovesNewPityEntryAfterSaveFailure() {
        PlayerData data = new PlayerData(UUID.randomUUID());
        BoxManager.BasicOpenStateSnapshot snapshot = BoxManager.snapshotBasicOpenState(data);

        data.setFreeBoxNextAtMillis(20_000L);
        data.boxPity().put("basic", 1);

        BoxManager.rollbackBasicOpenState(data, snapshot);

        assertEquals(0L, data.freeBoxNextAtMillis());
        assertFalse(data.boxPity().containsKey("basic"));
    }
}
