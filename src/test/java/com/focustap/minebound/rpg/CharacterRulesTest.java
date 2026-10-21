package com.focustap.minebound.rpg;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CharacterRulesTest {
    static CharacterProfile character(Race race, CharacterClass path) {
        CharacterProfile p = new CharacterProfile(); p.race = race; p.startingClass = path;
        // Spread points rather than optimizing a particular combat build.
        while (p.creationRemaining() > 0) for (Attribute a : Attribute.values()) if (p.creationRemaining() > 0) p.allocateCreation(a, 1);
        assertTrue(p.confirm()); return p;
    }
    @Test void creationGatesStagesAndPointsCannotBeOverspentOrGrantedTwice() {
        CharacterProfile p = new CharacterProfile();
        assertFalse(p.unlocked(0)); assertFalse(p.confirm()); assertFalse(p.spendSkill(CharacterClass.WIZARD));
        assertThrows(IllegalArgumentException.class, () -> new Battle(p, 0));
        assertFalse(p.allocateCreation(Attribute.STR, -1));
        for (int i = 0; i < 6; i++) assertTrue(p.allocateCreation(Attribute.STR, 1));
        assertFalse(p.allocateCreation(Attribute.STR, 1));
        while (p.creationRemaining() > 1) assertTrue(p.allocateCreation(Attribute.DEX, 1));
        assertTrue(p.allocateCreation(Attribute.WIS, 1));
        assertTrue(p.confirm()); assertEquals(1, p.skillPoints); assertFalse(p.confirm());
        assertFalse(p.allocateCreation(Attribute.CON, 1)); assertEquals(1, p.skillPoints);
        assertTrue(p.unlocked(0)); assertFalse(p.unlocked(1));
    }
    @Test void allRaceClassCombinationsHaveBoundedResourcesAndWorkingStartingAbilities() {
        for (Race race : Race.values()) for (CharacterClass path : CharacterClass.values()) {
            CharacterProfile p = character(race, path);
            assertTrue(p.maxHp() >= 25 && p.maxHp() <= 40, race + "/" + path + " HP " + p.maxHp());
            assertTrue(p.maxPp() >= 8 && p.maxPp() <= ppCeiling(path), race + "/" + path + " PP " + p.maxPp());
            assertTrue(p.abilities().stream().anyMatch(a -> a.path == path));
            Battle battle = new Battle(p, 0);
            Ability a = p.abilities().getFirst();
            int pp = battle.pp;
            assertTrue(battle.ability(a)); assertEquals(pp - p.cost(a), battle.pp);
            assertEquals(Battle.State.DODGE, battle.state);
        }
    }
    @Test void everyLegalPointBuyStaysInResourceRange() {
        for (Race race : Race.values()) for (CharacterClass path : CharacterClass.values()) {
            CharacterProfile p = new CharacterProfile(); p.race = race; p.startingClass = path;
            checkAllocations(p, 0, race.budget());
        }
    }
    private void checkAllocations(CharacterProfile p, int index, int remaining) {
        if (index == 6) {
            if (remaining != 0) return;
            assertTrue(p.maxHp() >= 25 && p.maxHp() <= 40, p.startingClass + " HP " + p.maxHp());
            assertTrue(p.maxPp() >= 8 && p.maxPp() <= ppCeiling(p.startingClass), p.startingClass + " PP " + p.maxPp());
            return;
        }
        for (int spent = 0; spent <= Math.min(6, remaining); spent++) {
            p.attributes[index] = 8 + spent; checkAllocations(p, index + 1, remaining - spent);
        }
        p.attributes[index] = 8;
    }
    private int ppCeiling(CharacterClass path) { return path == CharacterClass.WIZARD ? 44 : path == CharacterClass.WARLOCK ? 22 : 18; }
    @Test void racesAffectActualRules() {
        CharacterProfile human = character(Race.HUMAN, CharacterClass.DRUID);
        CharacterProfile elf = character(Race.ELF, CharacterClass.DRUID);
        CharacterProfile gnome = character(Race.GNOME, CharacterClass.DRUID);
        CharacterProfile undead = character(Race.UNDEAD, CharacterClass.DRUID);
        assertEquals(13, human.race.budget());
        assertTrue(elf.speed() > human.speed());
        assertEquals(3, gnome.cost(Ability.BRAMBLE));
        assertTrue(CombatRules.healing(undead, Ability.MEND) < CombatRules.healing(human, Ability.MEND));
        Battle battle = new Battle(undead, 0); battle.bash(); battle.hp = 1;
        Projectile hit = new Projectile(battle.heartX, battle.heartY, 0, 0, 3, 0, 0, 50);
        battle.bullets.add(hit); battle.update(0, 0, false);
        assertEquals(1, battle.hp); assertTrue(battle.unyieldingUsed);
        battle.iframe = 0; battle.update(0, 0, false); assertEquals(Battle.State.LOST, battle.state);
    }
    @Test void attributesAndClassPassivesChangeCombat() {
        CharacterProfile p = character(Race.HUMAN, CharacterClass.WARRIOR);
        int bash = CombatRules.bash(p, 1); p.attributes[Attribute.STR.ordinal()] += 2;
        assertEquals(bash + 1, CombatRules.bash(p, 1));
        int magic = CombatRules.abilityDamage(p, Ability.SPARK); p.attributes[Attribute.INT.ordinal()] += 2;
        assertEquals(magic + 2, CombatRules.abilityDamage(p, Ability.SPARK));
        int shield = CombatRules.rallyShield(p); p.attributes[Attribute.CHA.ordinal()] += 2;
        assertEquals(shield + 1, CombatRules.rallyShield(p));
        int heal = CombatRules.healing(p, Ability.MEND); p.attributes[Attribute.WIS.ordinal()] += 2;
        assertEquals(heal + 1, CombatRules.healing(p, Ability.MEND));
        CharacterProfile rogue = character(Race.HUMAN, CharacterClass.ROGUE);
        assertTrue(CombatRules.bash(rogue, 3) > CombatRules.bash(rogue, 2));
        assertTrue(CombatRules.incoming(p, 8, true) < CombatRules.incoming(p, 8, false));
        CharacterProfile paladin = character(Race.HUMAN, CharacterClass.PALADIN);
        assertEquals(3, CombatRules.incoming(paladin, 8, true));
    }
    @Test void firstClearsGiveLevelsAndRepeatClearsCannotFarmPoints() {
        CharacterProfile p = character(Race.HUMAN, CharacterClass.WARRIOR);
        for (int stage = 0; stage < 5; stage++) {
            assertTrue(p.unlocked(stage)); assertTrue(p.victory(stage) > 0);
            int xp = p.xp, points = p.skillPoints;
            assertEquals(0, p.victory(stage)); assertEquals(xp, p.xp); assertEquals(points, p.skillPoints);
        }
        assertEquals(5, p.level()); assertEquals(5, p.skillPoints); assertEquals(8, p.attributePoints);
        assertEquals(31, p.clearedMask);
        assertTrue(p.spendAttribute(Attribute.WIS)); assertEquals(7, p.attributePoints);
        assertTrue(p.spendSkill(CharacterClass.WIZARD));
        assertFalse(p.spendSkill(CharacterClass.WIZARD)); // connect through common root first
        assertTrue(p.spendSkill(CharacterClass.DRUID));
        assertTrue(p.spendSkill(CharacterClass.WIZARD));
        assertTrue(p.abilities().contains(Ability.SPARK));
        assertTrue(p.spendSkill(CharacterClass.DRUID));
        assertTrue(p.abilities().contains(Ability.MEND));
    }
    @Test void spellCostsPreventInfiniteHealingAndFailedCommandsDoNotSpendTurns() {
        CharacterProfile p = character(Race.GNOME, CharacterClass.DRUID);
        Battle b = new Battle(p, 0); b.pp = 0;
        assertFalse(b.ability(Ability.MEND)); assertEquals(Battle.State.COMMAND, b.state);
        assertFalse(b.ability(Ability.CLEAVE));
        assertTrue(b.defend()); assertFalse(b.defend());
        b.bullets.add(new Projectile(b.heartX, b.heartY, 0, 0, 3, 0, 0, 5));
        int before = b.hp; b.update(0, 0, false);
        assertTrue(b.hp > before - 6); assertEquals(0, b.pp);
    }
}
