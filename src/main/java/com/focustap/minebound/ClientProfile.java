package com.focustap.minebound;

/**
 * Local visual-prototype state. NOT secure or persistent.
 * Replace with server-owned SavedData plus sync packets before multiplayer.
 */
public final class ClientProfile {
    public static final ClientProfile DEMO = new ClientProfile();

    public int skillPoints = 1; // Automatically granted for first altar interaction in prototype.
    public int roguePassives;
    public int magePassives;
    public int unlockedStage = 0;

    public int str() { return 1; }
    public int dex() { return 1 + roguePassives; }
    public int con() { return 1; }
    public int intelligence() { return 1 + magePassives; }
    public int wis() { return 1; }
    public int cha() { return 1; }

    public boolean spendRogue() {
        if (skillPoints == 0) return false;
        skillPoints--;
        roguePassives++;
        return true;
    }

    public boolean spendMage() {
        if (skillPoints == 0) return false;
        skillPoints--;
        magePassives++;
        return true;
    }

    public boolean rogueAbility() { return roguePassives >= 5; }
    public boolean mageAbility() { return magePassives >= 5; }
}
