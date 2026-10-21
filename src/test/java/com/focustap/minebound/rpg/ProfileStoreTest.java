package com.focustap.minebound.rpg;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileStoreTest {
    @TempDir Path folder;
    @Test void roundTripPreservesCharacterAndNeverRegrantsCreationPoint() throws Exception {
        ProfileStore store = new ProfileStore(folder.resolve("world/player.json"));
        assertFalse(store.load().created);
        CharacterProfile p = CharacterRulesTest.character(Race.ELF, CharacterClass.ROGUE);
        p.name = "Fern"; p.victory(0); p.spendSkill(CharacterClass.WIZARD); p.spendAttribute(Attribute.CHA);
        store.save(p);
        CharacterProfile loaded = store.load();
        assertEquals("Fern", loaded.name); assertEquals(p.race, loaded.race); assertEquals(p.startingClass, loaded.startingClass);
        assertArrayEquals(p.attributes, loaded.attributes); assertArrayEquals(p.paths, loaded.paths);
        assertEquals(p.xp, loaded.xp); assertEquals(p.attributePoints, loaded.attributePoints);
        assertEquals(p.skillPoints, loaded.skillPoints); assertEquals(p.clearedMask, loaded.clearedMask);
        assertFalse(loaded.confirm()); store.save(loaded);
        assertTrue(Files.exists(folder.resolve("world/player.json.bak")));
    }
    @Test void corruptAndFutureProfilesAreNotSilentlyReset() throws Exception {
        Path file = folder.resolve("p.json"); ProfileStore store = new ProfileStore(file);
        for (String invalid : new String[]{"not json", "{\"schema\":900}", "{\"schema\":2}"}) {
            Files.writeString(file, invalid); assertThrows(IOException.class, store::load);
            assertEquals(invalid, Files.readString(file));
        }
    }
    @Test void legacyProgressIsBackedUpAndRequiresCreationWithEarnedPoints() throws Exception {
        Path file = folder.resolve("p.json");
        String legacy = "{\"unlockedStage\":2,\"roguePassives\":5,\"magePassives\":3}";
        Files.writeString(file, legacy); CharacterProfile p = new ProfileStore(file).load();
        assertFalse(p.created); assertEquals(3, p.clearedMask); assertEquals(150, p.xp);
        assertEquals(2, p.attributePoints); assertEquals(legacy, Files.readString(folder.resolve("p.json.v1.bak")));
        while (p.creationRemaining() > 0) for (Attribute a : Attribute.values()) if (p.creationRemaining() > 0) p.allocateCreation(a, 1);
        assertTrue(p.confirm()); assertEquals(2, p.skillPoints);
    }
    @Test void worldAndPlayerFilesAreIndependent() throws Exception {
        ProfileStore a = new ProfileStore(folder.resolve("a/player.json"));
        ProfileStore b = new ProfileStore(folder.resolve("b/player.json"));
        a.save(CharacterRulesTest.character(Race.UNDEAD, CharacterClass.WIZARD));
        assertFalse(b.load().created);
    }
}
