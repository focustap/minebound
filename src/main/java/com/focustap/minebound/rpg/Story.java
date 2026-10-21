package com.focustap.minebound.rpg;

import java.util.List;

/** Structured narrative data, independent of widgets and transport. */
public final class Story {
    public record Stage(String name, String description, String intro, String defeat, String lore) {}
    public static final List<String> OPENING = List.of(
            "The stone is cold. The voice inside it is not.\n\n'At last. Someone with hands. You would not believe how long we have needed hands.'",
            "Long before the first village bell, another realm grew beside the Overworld. Its people learned to borrow the space between realities.\n\nThen something borrowed back.",
            "The old altars sealed the crossing. Now roots grow through their locks.\n\nIn the Whispering Woods, a crowned guardian has forgotten what it was guarding.",
            "Touch the seal. Give the realm a name to remember.\n\nYour tools stay here. Your courage, unfortunately, comes with you.");
    public static final List<Stage> WORLD_ONE = List.of(
            new Stage("Mosslight Crossing","A small clearing where the moss listens before it grows.",
                    "Moss Sprite: 'Please wipe your soul before entering.'",
                    "A path opens beneath the moss. Something below it knocks once.",
                    "The moss remembers a road that was sealed from both ends. Someone wanted to keep more than visitors out."),
            new Stage("Briar Post","A toll station staffed by someone who accepts only apologies.",
                    "Briarling: 'The toll is one sincere apology. Yours sounds suspiciously rehearsed.'",
                    "The briars part. A guard's bell rings without a tongue.",
                    "The briars were planted after the sealing. Their roots point inward, toward the crown."),
            new Stage("The Empty Watch","A knight still keeps an oath whose words have been scratched away.",
                    "Thorn Knight: 'I was told to admit no one. They did not specify how long.'",
                    "Thorn Knight: 'Tell the Crown I kept the gate. Tell it ... I am tired.'",
                    "The knight's broken oath names a Keeper of the Unwritten. The rest of the inscription was erased from inside the stone."),
            new Stage("Oracle's Mire","The pond reflects tomorrow, usually with unhelpful timing.",
                    "Mire Oracle: 'You will step left. Unless you heard that. Ah. This is why I dislike appointments.'",
                    "The mire shows a crown, a key, and a hand turning the key from the wrong side.",
                    "The first corruption came through an opened door. The records call it an accident. The pond declines to agree."),
            new Stage("The Root-Crowned Keep","The forest's guardian is holding something shut with its own heart.",
                    "Crownroot: 'I am not the prisoner. I am the lock. Do not mistake the two.'",
                    "The crown cracks. Beneath it, a clear voice whispers: 'Find the other seals. The Unwritten has learned our names.'\n\nFor a moment, the woods remember sunlight.",
                    "Crownroot bound itself to the crossing to hold the breach closed. Its madness was the strain of a lock being picked, slowly, from beyond.") );
    private Story() {}
    public static Stage stage(int stage) { return WORLD_ONE.get(stage); }
}
