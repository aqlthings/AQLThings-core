package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import org.bukkit.ChatColor;

/**
 * Created by Billi on 13/11/2018.
 *
 * @author Billi
 */
public class TempCharSkin extends AbstractSkin {
    private TempCharacter character;

    public TempCharSkin(TempCharacter character) {
        super("Skin de "+character.getName());
        this.character = character;
        setFile(character.getSkin());
    }

    // --- Accessors ---

    public TempCharacter getCharacter() {
        return character;
    }

    // --- Methods ---

    @Override
    public String getLabel() {
        return "skin temporaire, "+ChatColor.ITALIC+ChatColor.WHITE+getName();
    }
}
