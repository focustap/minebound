package com.focustap.minebound.rpg;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExpansionRulesTest {
    @TempDir Path temp;
    @Test void warlockStartsWithChaBlastAndLearnsPsychicBladeThroughStableBladeslingerNodes() throws Exception {
        CharacterProfile p=CharacterRulesTest.character(Race.HUMAN,CharacterClass.WARLOCK);
        assertTrue(p.abilities().contains(Ability.ELDRITCH_BLAST)); assertFalse(p.abilities().contains(Ability.PSYCHIC_BLADE));
        int damage=CombatRules.abilityDamage(p,Ability.ELDRITCH_BLAST); p.attributes[Attribute.CHA.ordinal()]+=2;
        assertEquals(damage+2,CombatRules.abilityDamage(p,Ability.ELDRITCH_BLAST));
        assertEquals(Attribute.CHA,CombatRules.bashScaling(p));
        var active=SkillGraph.NODES.get("psychic_blade_6"); assertEquals("Bladeslinger",active.branch());
        p.nodes.add("psychic_blade_5"); p.skillPoints=10;
        assertTrue(SkillGraph.purchaseBlocker(p,active).contains("4 more passive")); assertFalse(SkillGraph.purchase(p,active.id()));
        p.nodes.addAll(List.of("psychic_blade_1","psychic_blade_2","psychic_blade_3","psychic_blade_4"));
        assertEquals("",SkillGraph.purchaseBlocker(p,active)); assertTrue(SkillGraph.purchase(p,active.id()));
        assertTrue(p.abilities().contains(Ability.PSYCHIC_BLADE));
        ProfileStore store=new ProfileStore(temp.resolve("warlock.json")); store.save(p);
        assertTrue(store.load().abilities().contains(Ability.PSYCHIC_BLADE));
    }
    @Test void hpAndPpCarryAcrossVictoriesRetreatsAndReloads() throws Exception {
        CharacterProfile p=CharacterRulesTest.character(Race.ELF,CharacterClass.WIZARD);
        Recovery.initialize(p,100); p.currentHp=17; p.currentPp=11;
        Battle a=new Battle(p,0); assertEquals(17,a.hp); assertEquals(11,a.pp);
        a.enemyHp=1; a.ability(Ability.SPARK); assertEquals(Battle.State.WON,a.state);
        p.victory(0); int hp=p.currentHp, pp=p.currentPp;
        assertEquals(17,hp); assertEquals(9,pp);
        Battle b=new Battle(p,1); assertEquals(hp,b.hp); assertEquals(pp,b.pp); b.retreat();
        ProfileStore store=new ProfileStore(temp.resolve("p.json")); store.save(p);
        CharacterProfile loaded=store.load(); assertEquals(hp,loaded.currentHp); assertEquals(pp,loaded.currentPp);
        assertEquals(100,loaded.lastRecoveryTick);
    }
    @Test void recoveryRequiresOneElapsedDayAndCannotRepeatOrBeClaimedInBattle() {
        CharacterProfile p=CharacterRulesTest.character(Race.HUMAN,CharacterClass.WARRIOR); Recovery.initialize(p,500);
        p.currentHp=1; p.currentPp=0;
        assertFalse(Recovery.check(p,24499,false)); assertFalse(Recovery.check(p,24500,true));
        assertTrue(Recovery.check(p,24500,false)); assertEquals(p.maxHp(),p.currentHp);
        p.currentHp=3; assertFalse(Recovery.check(p,24500,false)); assertFalse(Recovery.check(p,30000,false));
        assertEquals(3,p.currentHp);
        assertFalse(Recovery.check(p,200,false)); assertEquals(200,p.lastRecoveryTick);
        assertFalse(Recovery.check(p,24199,false)); assertTrue(Recovery.check(p,24200,false));
    }
    @Test void xpLedgerRejectsRepeatedClicksDuplicateEventsAndBudgetBypassesAcrossReload() throws Exception {
        CharacterProfile p=CharacterRulesTest.character(Race.HUMAN,CharacterClass.ROGUE);
        assertEquals(2,XpLedger.accept(p,"farm","field",100,2,1,5,1200));
        assertEquals(0,XpLedger.accept(p,"farm","field",100,2,1,5,1200));
        assertEquals(0,XpLedger.accept(p,"farm","field",101,2,1,5,1200));
        assertEquals(2,XpLedger.accept(p,"farm","field2",102,2,1,5,1200));
        assertEquals(1,XpLedger.accept(p,"farm","field3",103,2,1,5,1200));
        ProfileStore store=new ProfileStore(temp.resolve("p.json")); store.save(p); p=store.load();
        assertEquals(0,XpLedger.accept(p,"farm","field4",200,2,1,5,1200));
        assertEquals(0,XpLedger.accept(p,"farm","field4",50,2,1,5,1200));
        assertEquals(2,XpLedger.accept(p,"farm","field4",24100,2,1,5,1200));
        assertTrue(XpLedger.harvested(7,7,0,true)); assertTrue(XpLedger.harvested(7,7,0,false));
        assertFalse(XpLedger.harvested(7,7,7,true)); assertFalse(XpLedger.harvested(6,7,0,false));
        assertFalse(XpLedger.harvested(0,0,0,false));
    }
    @Test void graphRequiresConnectivityAndFivePassivesBeforeAnActiveAndSupportsHybrids() {
        CharacterProfile p=CharacterRulesTest.character(Race.HUMAN,CharacterClass.WARRIOR); p.skillPoints=30;
        assertFalse(SkillGraph.purchase(p,"fire_6")); assertFalse(SkillGraph.purchase(p,"arcane_3"));
        assertTrue(SkillGraph.purchase(p,"arcane_1")); assertTrue(SkillGraph.purchase(p,"arcane_2"));
        assertTrue(SkillGraph.purchase(p,"arcane_3"));
        assertTrue(SkillGraph.purchase(p,"trickster_3")); // cross-family lateral bridge
        assertTrue(SkillGraph.purchase(p,"trickster_4")); assertTrue(SkillGraph.purchase(p,"trickster_5"));
        assertTrue(SkillGraph.purchase(p,"trickster_6")); assertTrue(p.abilities().contains(Ability.FEINT));
        int points=p.skillPoints; assertFalse(SkillGraph.purchase(p,"trickster_6")); assertEquals(points,p.skillPoints);
        assertTrue(p.hybridIdentity().contains("Wizard") && p.hybridIdentity().contains("Rogue"));
        assertTrue(p.cost(Ability.SPARK)<Ability.SPARK.cost); assertTrue(p.maxPp()>p.startingClass.basePp);
        for (var n:SkillGraph.NODES.values()) for (String link:n.links()) assertTrue(SkillGraph.NODES.containsKey(link));
    }
    @Test void wizardRemainsSpellFocusedAcrossWorldOneWithoutAnyDailyRest() {
        CharacterProfile p=CharacterRulesTest.character(Race.HUMAN,CharacterClass.WIZARD); Recovery.initialize(p,0);
        int spells=0,bashes=0,initial=p.currentPp;
        assertTrue(SkillGraph.purchase(p,"arcane_1"));
        for (int stage=0;stage<5;stage++) {
            if (p.skillPoints>0 && !p.nodes.contains("arcane_2")) SkillGraph.purchase(p,"arcane_2");
            if (p.skillPoints>0 && p.nodes.contains("arcane_2") && !p.nodes.contains("arcane_3")) SkillGraph.purchase(p,"arcane_3");
            if (p.currentPp<12 && p.goods.getOrDefault("ether",0)>0) Battle.useGood(p,"ether");
            Battle b=new Battle(p,stage);
            while (b.state!=Battle.State.WON) {
                if (b.pp>=p.cost(Ability.SPARK)) { assertTrue(b.ability(Ability.SPARK)); spells++; }
                else { b.bash(); bashes++; }
                if (b.state==Battle.State.DODGE) b.state=Battle.State.COMMAND; // resource fixture isolates dodge skill
            }
            p.victory(stage);
            while (p.attributePoints>0 && p.attributes[Attribute.INT.ordinal()]<18) p.spendAttribute(Attribute.INT);
        }
        assertTrue(spells>=20); assertTrue(bashes<=3,"Wizard needed "+bashes+" fallback BASH attacks");
        assertTrue(p.currentPp<initial); assertEquals(0,p.lastRecoveryTick);
    }
    @Test void warlockHasChaMeleeAndRetreatCannotMintUnlimitedRestoration() {
        CharacterProfile p=CharacterRulesTest.character(Race.HUMAN,CharacterClass.WARLOCK); Recovery.initialize(p,0);
        int bash=CombatRules.bash(p,1); p.attributes[Attribute.CHA.ordinal()]+=2;
        assertEquals(bash+1,CombatRules.bash(p,1));
        p.nodes.add("arcane_meditate"); p.currentPp=0;
        Battle b=new Battle(p,0); assertTrue(b.ability(Ability.MEDITATE)); b.retreat(); assertEquals(4,p.currentPp);
        b=new Battle(p,0); assertFalse(b.ability(Ability.MEDITATE));
        p.lastRecoveryTick=100; assertFalse(Recovery.check(p,1,false));
        b=new Battle(p,0); assertFalse(b.ability(Ability.MEDITATE)); // clock rollback cannot refresh the spell
        p.currentPp=0; p.pactPpThisDay=12; b=new Battle(p,0);
        for (int i=0;i<3;i++) { b.bash(); if (b.state==Battle.State.DODGE) b.state=Battle.State.COMMAND; }
        assertEquals(0,b.pp);
        assertTrue(Recovery.check(p,Recovery.DAY+1,false)); assertEquals(0,p.pactPpThisDay);
    }
    @Test void resetAggregateContainsNoInheritedProgressAndDoesNotMutateOldSnapshot() throws Exception {
        CharacterProfile old=CharacterRulesTest.character(Race.UNDEAD,CharacterClass.WARLOCK);
        old.victory(0); old.nodes.add("affliction_1"); old.introSeen=true; old.goods.put("ether",3); old.lore.add("secret");
        CharacterProfile reset=new CharacterProfile();
        assertFalse(reset.created); assertFalse(reset.introSeen); assertEquals(0,reset.xp); assertEquals(0,reset.clearedMask);
        assertTrue(reset.nodes.isEmpty()); assertTrue(reset.goods.isEmpty()); assertTrue(reset.lore.isEmpty());
        assertTrue(reset.xpWindows.isEmpty()); assertEquals(-1,reset.lastRecoveryTick); assertEquals(-1,reset.currentHp);
        assertEquals(0,reset.skillPoints); assertTrue(old.created); assertEquals(3,old.goods.get("ether"));
        ProfileStore store=new ProfileStore(temp.resolve("reset.json")); store.save(reset);
        CharacterProfile reloaded=store.load(); assertFalse(reloaded.created); assertEquals(-1,reloaded.currentHp);
    }
    @Test void everyNodeEffectHasAnImplementedRuleAndEveryAbilityCanBeExecuted() {
        List<String> effects=List.of("attr:STR","attr:DEX","attr:CON","attr:INT","attr:WIS","attr:CHA",
                "bash","spell","hp","pp","efficiency","speed","heal","shield","precision","vulnerable","resist","pact","drain","active");
        for (var node:SkillGraph.NODES.values()) assertTrue(effects.contains(node.effect()),node.id());
        for (Ability a:Ability.values()) {
            CharacterProfile p=CharacterRulesTest.character(Race.HUMAN,a.path); p.skillPoints=100;
            SkillGraph.NODES.values().stream().filter(n -> n.ability()==a).forEach(n -> p.nodes.add(n.id()));
            p.currentPp=p.maxPp(); Battle b=new Battle(p,0); assertTrue(b.ability(a),a.name());
            assertEquals(Battle.State.DODGE,b.state);
        }
    }
    @Test void efficiencyAndRefundsCannotMakeLifeDrainFree() {
        CharacterProfile p=CharacterRulesTest.character(Race.GNOME,CharacterClass.WARLOCK);
        p.nodes.addAll(List.of("arcane_3","destruction_4","affliction_drain"));
        Battle b=new Battle(p,0); int before=b.pp;
        assertEquals(1,p.cost(Ability.DRAIN)); assertTrue(b.ability(Ability.DRAIN));
        assertEquals(before-1,b.pp);
    }
    @Test void activeNodesImproveAlreadyKnownStartingSkillsInsteadOfBecomingEmptyPurchases() {
        CharacterProfile druid=CharacterRulesTest.character(Race.HUMAN,CharacterClass.DRUID);
        int damage=CombatRules.abilityDamage(druid,Ability.BRAMBLE),heal=CombatRules.healing(druid,Ability.BRAMBLE);
        druid.nodes.add("nature_6");
        assertEquals(damage+2,CombatRules.abilityDamage(druid,Ability.BRAMBLE));
        assertEquals(heal+2,CombatRules.healing(druid,Ability.BRAMBLE));
        CharacterProfile paladin=CharacterRulesTest.character(Race.HUMAN,CharacterClass.PALADIN);
        heal=CombatRules.healing(paladin,Ability.WARD); paladin.nodes.add("holy_defender_6");
        assertEquals(heal+2,CombatRules.healing(paladin,Ability.WARD));
        CharacterProfile warlock=CharacterRulesTest.character(Race.HUMAN,CharacterClass.WARLOCK);
        damage=CombatRules.abilityDamage(warlock,Ability.PSYCHIC_BLADE); warlock.nodes.add("psychic_blade_6");
        assertEquals(damage+2,CombatRules.abilityDamage(warlock,Ability.PSYCHIC_BLADE));
    }
}
