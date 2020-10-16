package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import fr.aquilon.minecraft.utils.JSONExportable;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Created by Billi on 11/03/2018.
 */
public class CharacterPlayer implements JSONExportable {
    private String uuid;
    private String username;
    private int selectedChar;
    private String name;
    private String skin;
    private int commonSkin;
    private long updated;

    public CharacterPlayer(String uuid) {
        this.uuid = uuid;
    }

    // --- Accessors ---

    public String getUUID() {
        return uuid;
    }

    public UUID getUUIDObject() {
        if (uuid==null) return null;
        return UUID.fromString(Utils.addUuidDashes(uuid));
    }

    public CharacterPlayer setUUID(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public CharacterPlayer setUsername(String username) {
        this.username = username;
        return this;
    }

    public int getSelectedCharacter() {
        return selectedChar;
    }

    public CharacterPlayer setSelectedCharacter(int selectedChar) {
        this.selectedChar = selectedChar;
        return this;
    }

    public String getName() {
        return name;
    }

    public CharacterPlayer setName(String name) {
        this.name = name;
        return this;
    }

    public String getSkinName() {
        return skin;
    }

    public CharacterPlayer setSkinName(String skin) {
        this.skin = skin;
        return this;
    }

    public int getCommonSkin() {
        return commonSkin;
    }

    public CharacterPlayer setCommonSkin(int commonSkin) {
        this.commonSkin = commonSkin;
        return this;
    }

    public long getUpdated() {
        return updated;
    }

    public CharacterPlayer setUpdated(long updated) {
        this.updated = updated;
        return this;
    }

    public CharacterPlayer setUpdated(Timestamp updated) {
        this.updated = updated.getTime();
        return this;
    }

    // --- Methods ---

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("uuid", getUUID());
        res.put("username", getUsername());
        res.put("selectedCharacter", getSelectedCharacter() != 0 ? getSelectedCharacter() : JSONObject.NULL);
        res.put("name", getName());
        res.put("skin", getSkinName());
        res.put("commonSkin", getCommonSkin());
        res.put("updated", JSONUtils.jsonDate(getUpdated()));
        return res;
    }
}
