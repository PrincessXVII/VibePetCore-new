package dev.li2fox.vibepetcore.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ModuleManagerTest {
    @Test
    void reloadAllDisablesAllModulesBeforeEnablingAnyModule() {
        List<String> calls = new ArrayList<>();
        ModuleManager manager = new ModuleManager(null);
        manager.register(new RecordingModule("data", calls));
        manager.register(new RecordingModule("engine", calls));
        manager.register(new RecordingModule("egg", calls));

        manager.reloadAll();

        assertEquals(List.of(
            "disable:egg",
            "disable:engine",
            "disable:data",
            "enable:data",
            "enable:engine",
            "enable:egg"
        ), calls);
    }

    private record RecordingModule(String name, List<String> calls) implements CoreModule {
        @Override
        public void enable() {
            calls.add("enable:" + name);
        }

        @Override
        public void disable() {
            calls.add("disable:" + name);
        }
    }
}
