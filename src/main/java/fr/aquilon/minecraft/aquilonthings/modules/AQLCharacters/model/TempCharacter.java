package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import org.json.JSONObject;

/**
 * Created by Billi on 11/03/2018.
 */
public class TempCharacter extends AbstractCharacter {
    private String name;
    private String skin;

    public TempCharacter(String pUUID) {
        super(pUUID);
    }

    // --- Accessors ---

    @Override
    public String getName() {
        return name;
    }

    public TempCharacter setName(String name) {
        this.name = name;
        return this;
    }

    public String getSkin() {
        return skin;
    }

    public TempCharSkin getSkinObject() {
        if (getSkin()==null) return null;
        return new TempCharSkin(this);
    }

    public TempCharacter setSkin(String skin) {
        this.skin = skin;
        return this;
    }

    @Override
    public JSONObject toJSON(boolean details, boolean skipFilter) {
        JSONObject res = super.toJSON(details, skipFilter);
        res.put("skin", getSkin());
        return res;
    }
}
