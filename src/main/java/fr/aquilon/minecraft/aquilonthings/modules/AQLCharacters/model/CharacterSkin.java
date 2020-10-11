package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import org.bukkit.ChatColor;
import org.json.JSONObject;

/**
 * Created by Billi on 11/03/2018.
 *
 * @author Billi
 */
public class CharacterSkin extends AbstractSkin {
    private int charID;

    public CharacterSkin(int charID, String name) {
        super(name);
        this.charID = charID;
    }

    // --- Accessors ---

    public int getCharacterID() {
        return charID;
    }

    // --- Methods ---

    @Override
    public JSONObject toJSON() {
        return toJSON(false);
    }

    public JSONObject toJSON(boolean skipCharID) {
        JSONObject res = super.toJSON();
        if (!skipCharID) res.put("character", getCharacterID());
        return res;
    }

    @Override
    public String getLabel() {
        return ChatColor.WHITE+getName();
    }
}
