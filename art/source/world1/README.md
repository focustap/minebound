# World 1 source sprite sheets

These are the five sheets supplied by the repository owner in commit `53652fc` ("spirtes"). Originals are moved here intact, including their embedded credits. They depict Deltarune characters; they are not original Minebound artwork.

| Minebound encounter | Supplied sheet | Build frame source |
|---|---|---|
| Moss Sprite | Wobbly Thing | Wobble row, four frames |
| Briarling | Rabbick | Idle row, three frames |
| Thorn Knight | Rudinn / Rudinn Ranger | Rudinn idle row, four frames |
| Mire Oracle | Ponman | Idle/walk row, four frames |
| Crownroot | Jevil | Main row, three frames |

Encounter names and mechanics retain World 1's existing identities. Art is mapped independently in `EnemySprites.java`.

`gradle/enemy-sprites.gradle` selects frame rectangles and removes each frame's solid backdrop. It generates PNGs into `build/generated/enemySprites/assets/minebound/textures/enemy/world1`. Only generated combat frames are included as mod textures; sheets, credits panels and sheet backgrounds are not packaged as gameplay images. Changing crop coordinates or originals automatically invalidates the generation task.

Credits visible in the supplied sheets include **TOBY FOX** (Ponman), **Modern / Ralsei Gang** (Jevil), and **EJ, asmy** (Ponman ripping credit). Preserve these original files for their full embedded attribution. This folder records supplied provenance rather than granting an asset license.
