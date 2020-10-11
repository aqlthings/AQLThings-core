package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.exceptions;

/**
 * Created by Billi on 29/09/2018.
 *
 * @author Billi
 */
public class MaxCharCountReachedEx extends Exception {
    private int max;

    public MaxCharCountReachedEx(int nb) {
        super("Reached maximum character count ("+nb+')');
        this.max = nb;
    }

    public int getMax() {
        return max;
    }
}
