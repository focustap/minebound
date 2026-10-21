package com.focustap.minebound.rpg;

import java.util.List;

public record EnemyDefinition(String id, String name, int hp, int damage, int xp, boolean boss) {
    public static final List<EnemyDefinition> WORLD_ONE = List.of(
            new EnemyDefinition("moss_sprite", "Moss Sprite", 42, 6, 60, false),
            new EnemyDefinition("briarling", "Briarling", 57, 7, 90, false),
            new EnemyDefinition("thorn_knight", "Thorn Knight", 108, 8, 120, true),
            new EnemyDefinition("mire_oracle", "Mire Oracle", 82, 8, 150, false),
            new EnemyDefinition("crownroot", "Crownroot", 186, 9, 240, true));

    public int phase(int health) {
        if (!boss) return 1;
        if (health * 3 <= hp) return 3;
        return health * 3 <= hp * 2 ? 2 : 1;
    }
}
