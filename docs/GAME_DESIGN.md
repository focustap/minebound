# Minebound game design

The current implemented rules are documented in [COMBAT_AND_CHARACTERS.md](COMBAT_AND_CHARACTERS.md), with [verification](TESTING.md) and [art direction](ART_DIRECTION.md).

The altar opens a persistent character and a five-node World 1 map. Encounters take place entirely in menus; Minecraft equipment and combat remain separate. First-time players create a race/class/attribute build before entering stages. Classes provide a starting identity while all five skill paths remain accessible through a common root.

Single-player progression currently awards first-clear encounter XP only. Long-term Minecraft activity rewards need explicit anti-automation design before they are added. GOODS, more worlds and a much larger interconnected skill tree remain future content.

The intended multiplayer design is described in [NETWORKING.md](NETWORKING.md). Current battles and saved characters are local; party invitations, readiness, authoritative damage and shared hearts are not implemented.
