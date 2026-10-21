package com.focustap.minebound.rpg;

import java.util.*;

/** Shared, ordered graph. Purchases are validated on the server, not by the canvas. */
public final class SkillGraph {
    public enum Kind { ATTRIBUTE, PASSIVE, ACTIVE, SPECIALIZATION }
    public record Node(String id, String label, String family, String branch, Kind kind,
                       String effect, int value, Ability ability, List<String> links, int cost, float x, float y, int color) {
        public String description() {
            if (ability != null) return ability.description + " Learn or master (+2 damage/healing/shield where applicable). Scales with " + ability.scaling + ".";
            return switch (effect) {
                case "bash" -> "+" + value + " BASH damage.";
                case "spell" -> "+" + value + " offensive spell damage.";
                case "hp" -> "+" + value + " maximum HP; current HP is unchanged.";
                case "pp" -> "+" + value + " maximum PP; current PP is unchanged.";
                case "efficiency" -> "Spell costs reduced by 1 PP (minimum 1).";
                case "speed" -> "Modestly improves heart movement; global cap still applies.";
                case "heal" -> "+" + value + " restorative healing.";
                case "shield" -> "+" + value + " finite guard shield per phase.";
                case "precision" -> "+" + value + " damage every third BASH.";
                case "vulnerable" -> "+" + value + " damage against an afflicted enemy.";
                case "resist" -> "+" + value + " to deterministic status saving throws.";
                case "pact" -> "Psychic Blade primes a stronger CHA-based BASH combo.";
                case "drain" -> "Damaging curse pulses heal 1 HP, up to three per use.";
                default -> "+1 " + effect.replace("attr:", "") + ".";
            };
        }
    }
    public static final Map<String,Node> NODES;
    static {
        LinkedHashMap<String,Node> nodes = new LinkedHashMap<>();
        branch(nodes, CharacterClass.WARRIOR, "guardian", "Guardian", -125, 0, Ability.BULWARK,
                new String[]{"attr:CON", "hp", "shield", "resist", "attr:STR"});
        branch(nodes, CharacterClass.WARRIOR, "berserker", "Berserker", -95, 0, Ability.RECKLESS_STRIKE,
                new String[]{"attr:STR", "bash", "precision", "hp", "attr:STR"});
        branch(nodes, CharacterClass.ROGUE, "assassin", "Assassin", -65, 1, Ability.BACKSTAB,
                new String[]{"attr:DEX", "precision", "speed", "vulnerable", "attr:DEX"});
        branch(nodes, CharacterClass.ROGUE, "trickster", "Trickster", -35, 1, Ability.FEINT,
                new String[]{"attr:CHA", "speed", "shield", "resist", "attr:DEX"});
        branch(nodes, CharacterClass.WIZARD, "arcane", "Arcane", -5, 2, Ability.ARCANE_MISSILES,
                new String[]{"attr:INT", "pp", "efficiency", "shield", "spell"});
        branch(nodes, CharacterClass.WIZARD, "fire", "Fire", 20, 2, Ability.FIREBOLT,
                new String[]{"attr:INT", "spell", "pp", "vulnerable", "attr:INT"});
        branch(nodes, CharacterClass.WIZARD, "ice", "Ice", 45, 2, Ability.FROSTBOLT,
                new String[]{"attr:INT", "shield", "pp", "resist", "attr:CON"});
        branch(nodes, CharacterClass.DRUID, "nature", "Nature", 72, 3, Ability.BRAMBLE,
                new String[]{"attr:WIS", "heal", "hp", "resist", "attr:WIS"});
        branch(nodes, CharacterClass.DRUID, "restoration", "Restoration", 99, 3, Ability.REJUVENATE,
                new String[]{"attr:WIS", "heal", "pp", "shield", "attr:CON"});
        branch(nodes, CharacterClass.PALADIN, "holy_defender", "Holy Defender", 126, 4, Ability.WARD,
                new String[]{"attr:CON", "shield", "heal", "resist", "attr:WIS"});
        branch(nodes, CharacterClass.PALADIN, "retribution", "Retribution", 151, 4, Ability.SMITE,
                new String[]{"attr:STR", "bash", "shield", "vulnerable", "attr:WIS"});
        branch(nodes, CharacterClass.WARLOCK, "psychic_blade", "Psychic Blade", 177, 5, Ability.PSYCHIC_BLADE,
                new String[]{"attr:CHA", "bash", "pact", "hp", "attr:CHA"});
        branch(nodes, CharacterClass.WARLOCK, "destruction", "Destruction", 199, 5, Ability.EMBER_NOVA,
                new String[]{"attr:CHA", "spell", "pp", "efficiency", "attr:CHA"});
        branch(nodes, CharacterClass.WARLOCK, "affliction", "Affliction", 218, 5, Ability.HEX,
                new String[]{"attr:CHA", "vulnerable", "drain", "resist", "attr:CHA"});
        extra(nodes, "arcane_cantrip", "Threadlight", "Arcane", Ability.CANTRIP, "arcane_6", 650, -50);
        extra(nodes, "arcane_meditate", "Still Mind", "Arcane", Ability.MEDITATE, "arcane_cantrip", 712, -50);
        extra(nodes, "affliction_drain", "Life Siphon", "Affliction", Ability.DRAIN, "affliction_6", -480, -400);
        extra(nodes, "ice_barrier", "Ice Mirror", "Ice", Ability.BARRIER, "ice_6", 470, 470);
        // Lateral bridges allow hybrid travel at the middle of neighboring branches.
        String[][] bridges = {{"arcane_3","trickster_3"},{"psychic_blade_3","retribution_3"},
                {"ice_3","nature_3"},{"restoration_3","holy_defender_3"},{"berserker_3","assassin_3"},
                {"destruction_3","fire_3"},{"affliction_3","guardian_3"}};
        for (String[] bridge : bridges) { link(nodes, bridge[0], bridge[1]); link(nodes, bridge[1], bridge[0]); }
        NODES = Collections.unmodifiableMap(nodes);
    }
    private SkillGraph() {}
    private static void branch(Map<String,Node> out, CharacterClass family, String id, String title, float angle, int colorIndex,
                               Ability ability, String[] effects) {
        int[] colors = {0xFFE8B86C,0xFF6EA484,0xFFCDBDE9,0xFF92C97C,0xFFB9DDE5,0xFFC488BB};
        double radians = Math.toRadians(angle);
        for (int depth = 1; depth <= 6; depth++) {
            String effect = depth <= 5 ? effects[depth-1] : "active";
            Kind kind = depth == 6 ? Kind.ACTIVE : effect.startsWith("attr:") ? Kind.ATTRIBUTE : depth == 3 ? Kind.SPECIALIZATION : Kind.PASSIVE;
            String label = depth == 6 ? ability.label() : effect.startsWith("attr:") ? "+1 " + effect.substring(5)
                    : switch(effect) { case "pp" -> "Reservoir"; case "hp" -> "Endurance"; case "efficiency" -> "Efficient Weave";
                    case "shield" -> "Aegis"; case "spell" -> "Amplify"; case "speed" -> "Light Step"; case "pact" -> "Blade Pact";
                    case "precision" -> "Precision"; case "vulnerable" -> "Exploit"; case "heal" -> "Renewal"; case "resist" -> "Resolve";
                    case "drain" -> "Leeching Curse"; default -> "Force"; };
            float distance = 70 + depth * 73;
            out.put(id + "_" + depth, new Node(id + "_" + depth, label, family.label, title, kind, effect,
                    effect.equals("pp") || effect.equals("hp") ? 3 : 1, depth == 6 ? ability : null,
                    depth == 1 ? List.of() : List.of(id + "_" + (depth-1)), 1,
                    (float)Math.cos(radians)*distance, (float)Math.sin(radians)*distance, colors[colorIndex]));
        }
    }
    private static void extra(Map<String,Node> out, String id, String label, String branch, Ability a, String parent, float x, float y) {
        out.put(id, new Node(id,label,a.path.label,branch,Kind.ACTIVE,"active",0,a,List.of(parent),2,x,y,0xFFCDBDE9));
    }
    private static void link(Map<String,Node> out, String id, String other) {
        Node n = out.get(id); List<String> links = new ArrayList<>(n.links); links.add(other);
        out.put(id,new Node(n.id,n.label,n.family,n.branch,n.kind,n.effect,n.value,n.ability,List.copyOf(links),n.cost,n.x,n.y,n.color));
    }
    public static boolean reachable(CharacterProfile p, Node n) {
        return n.links.isEmpty() || n.links.stream().anyMatch(p.nodes::contains);
    }
    public static boolean purchase(CharacterProfile p, String id) {
        Node n = NODES.get(id);
        if (!p.created || n == null || p.nodes.contains(id) || p.skillPoints < n.cost || !reachable(p,n)) return false;
        if (n.ability != null && p.nodes.stream().map(NODES::get).filter(Objects::nonNull).filter(x -> x.ability == null).count() < 5) return false;
        p.skillPoints -= n.cost; p.nodes.add(id); return true;
    }
    public static int bonus(CharacterProfile p, String effect) {
        return p.nodes.stream().map(NODES::get).filter(Objects::nonNull).filter(n -> effect.equals(n.effect)).mapToInt(Node::value).sum();
    }
    public static int attributeBonus(CharacterProfile p, Attribute a) { return bonus(p,"attr:"+a); }
    public static boolean unlocks(CharacterProfile p, Ability a) {
        return p.nodes.stream().map(NODES::get).filter(Objects::nonNull).anyMatch(n -> n.ability == a);
    }
}
