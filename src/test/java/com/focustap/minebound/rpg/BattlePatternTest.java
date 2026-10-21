package com.focustap.minebound.rpg;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class BattlePatternTest {
    @Test void stationaryHeartsAreHitInEveryStageAndCanLose() {
        for (int stage = 0; stage < 5; stage++) {
            CharacterProfile p = CharacterRulesTest.character(Race.HUMAN, CharacterClass.WARRIOR); p.clearedMask = 31;
            Battle b = new Battle(p, stage); int initialHp = b.hp;
            for (int frame = 0; frame < 2500 && b.state != Battle.State.LOST && b.state != Battle.State.WON; frame++) {
                if (b.state == Battle.State.COMMAND) b.bash();
                else b.update(0, 0, false);
            }
            assertTrue(b.hp < initialHp, "Stationary heart escaped stage " + stage);
            assertEquals(Battle.State.LOST, b.state, "Stationary heart won stage " + stage);
        }
    }
    @Test void aimedShotsLockAtLaunchAndPredictiveShotsDoNotHome() {
        List<Projectile> bullets = new ArrayList<>();
        Patterns.aimed(bullets, -8, 30, 120, 50, 3, 3);
        Projectile center = bullets.get(1);
        float vx = center.vx, vy = center.vy;
        assertEquals((50f - 30) / (120 + 8), vy / vx, .0001f);
        for (int i = 0; i < 30; i++) center.tick();
        assertEquals(vx, center.vx); assertEquals(vy, center.vy);
        List<Projectile> still = new ArrayList<>(), moving = new ArrayList<>();
        // Oracle segment 0, even volley: prediction uses recent heart velocity.
        Patterns.spawn(still, 3, 2, 1, 24, 100, 50, 0, 0);
        Patterns.spawn(moving, 3, 2, 1, 24, 100, 50, 2, 1);
        assertNotEquals(still.getFirst().vx, moving.getFirst().vx);
    }
    @Test void fullArenaSpawnsAndBossPhasesHaveDistinctSequences() {
        HashSet<String> signatures = new HashSet<>();
        for (int phase = 1; phase <= 3; phase++) {
            List<Projectile> all = new ArrayList<>();
            for (int tick = 1; tick <= Patterns.LENGTH; tick++) Patterns.spawn(all, 4, 1, phase, tick, 118, 55, 1, 0);
            assertTrue(all.stream().anyMatch(b -> b.x < 0)); assertTrue(all.stream().anyMatch(b -> b.x > Patterns.WIDTH));
            assertTrue(all.stream().anyMatch(b -> b.y < 0)); assertTrue(all.stream().anyMatch(b -> b.y > Patterns.HEIGHT));
            signatures.add(all.size() + ":" + Patterns.name(4, 1, phase, 24));
        }
        assertEquals(3, signatures.size());
        List<Projectile> briar = new ArrayList<>();
        for (int tick = 1; tick <= Patterns.LENGTH; tick++) Patterns.spawn(briar, 1, 1, 1, tick, 118, 55, 0, 0);
        assertTrue(briar.stream().anyMatch(b -> b.x > Patterns.WIDTH * .75));
        assertTrue(briar.stream().anyMatch(b -> b.x < Patterns.WIDTH * .25));
    }
    @Test void telegraphsDoNotDamageAndWallsHaveUsableGaps() {
        Projectile hazard = new Projectile(100, 50, 0, 0, 13, 0, 28, 32);
        for (int i = 0; i < 27; i++) { hazard.tick(); assertFalse(hazard.hits(100, 50)); }
        hazard.tick(); assertTrue(hazard.hits(100, 50));
        for (int volley = 0; volley < 12; volley++) {
            List<Projectile> wall = new ArrayList<>(); Patterns.wall(wall, volley, 1);
            boolean vertical = volley % 2 == 0;
            int extent = vertical ? Patterns.WIDTH : Patterns.HEIGHT;
            float gap = 24 + volley * 47 % (extent - 48);
            for (Projectile b : wall) {
                assertEquals(48, b.delay);
                for (int tick = 0; tick < 160; tick++) {
                    b.tick(); assertFalse(b.hits(vertical ? gap : 118, vertical ? 55 : gap), "Wall gap blocked");
                }
            }
        }
    }
    @Test void targetedSpreadCanBeDodgedAfterLaunch() {
        List<Projectile> bullets = new ArrayList<>(); Patterns.aimed(bullets, 118, -8, 118, 55, 2.15f, 3);
        float hx = 118, hy = 55;
        for (int tick = 0; tick < 100; tick++) {
            hx = Math.min(Patterns.WIDTH - 6, hx + 2.05f);
            for (Projectile b : bullets) { b.tick(); assertFalse(b.hits(hx, hy)); }
        }
    }
    @Test void sampledCornersAndCenterCannotCampForAnEntirePhase() {
        for (int stage = 0; stage < 5; stage++) for (int phase : new int[]{1, 3}) {
            if (phase == 3 && stage != 2 && stage != 4) continue;
            for (float[] position : new float[][]{{6,6}, {230,6}, {6,104}, {230,104}, {118,55}}) {
                CharacterProfile p = CharacterRulesTest.character(Race.HUMAN, CharacterClass.WARRIOR); p.clearedMask = 31;
                Battle b = new Battle(p, stage);
                if (phase == 3) b.enemyHp = b.enemy.hp() / 4;
                b.heartX = position[0]; b.heartY = position[1]; b.bash();
                int hp = b.hp;
                while (b.state == Battle.State.DODGE) b.update(0, 0, false);
                assertTrue(b.hp < hp, "Safe camp: stage " + stage + " phase " + phase + " at " + position[0] + "," + position[1]);
            }
        }
    }
    @Test void collisionIsSweptAndDiagonalMovementIsNormalized() {
        Projectile fast = new Projectile(0, 50, 30, 0, 3, 0, 0, 3);
        fast.tick(); assertTrue(fast.hits(15, 50));
        CharacterProfile p = CharacterRulesTest.character(Race.HUMAN, CharacterClass.ROGUE);
        Battle b = new Battle(p, 0); b.bash(); float x = b.heartX, y = b.heartY;
        b.update(1, 1, false);
        assertEquals(p.speed(), Math.hypot(b.heartX - x, b.heartY - y), .001);
    }
    @Test void bossesNeedSeveralRoundsEvenWithLevelOneAbilitiesAndChangePhase() {
        for (int stage : new int[]{2, 4}) for (CharacterClass path : CharacterClass.values()) {
            CharacterProfile p = CharacterRulesTest.character(Race.HUMAN, path); p.clearedMask = 31;
            Battle b = new Battle(p, stage); int commands = 0; var phases = new HashSet<Integer>();
            while (b.state != Battle.State.WON && commands < 100) {
                phases.add(b.phase());
                Ability a = p.abilities().getFirst();
                if (CombatRules.abilityDamage(p, a) > 0 && b.pp >= p.cost(a)) b.ability(a); else b.bash();
                commands++;
                // Isolate boss durability from player dodge skill in this test.
                if (b.state == Battle.State.DODGE) b.state = Battle.State.COMMAND;
            }
            assertEquals(Battle.State.WON, b.state);
            assertTrue(commands >= 5, path + " killed boss in " + commands + " commands");
            assertTrue(phases.containsAll(List.of(1, 2, 3)));
        }
    }
}
