package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.exceptions;

/**
 * Created by Billi on 29/09/2018.
 *
 * @author Billi
 */
public class MaxSkinCountReachedEx extends Exception {
    private int max;

    public MaxSkinCountReachedEx(int nb) {
        super("Reached maximum skin count per character ("+nb+')');
        this.max = nb;
    }

    public int getMax() {
        return max;
    }
}
