package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.exceptions;

/**
 * Created by Billi on 10/03/2018.
 *
 * @author Billi
 */
public class CharacterNotFoundEx extends Exception {
    public CharacterNotFoundEx() {
        super("No such character");
    }
}
