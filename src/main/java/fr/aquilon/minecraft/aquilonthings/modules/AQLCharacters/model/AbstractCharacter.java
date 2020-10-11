package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIForumUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIUser;
import fr.aquilon.minecraft.utils.JSONExportable;
import fr.aquilon.minecraft.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by Billi on 11/03/2018.
 */
public abstract class AbstractCharacter implements JSONExportable {
    private String pUUID;
    private char sex;
    private String race;
    private float height;
    private float weight;

    public AbstractCharacter(String pUUID) {
        setPlayerUUID(pUUID);
    }

    // --- Accessors ---

    public String getPlayerUUID() {
        return pUUID;
    }

    public UUID getPlayerUUIDObject() {
        if (pUUID==null) return null;
        return Utils.getUUID(getPlayerUUID());
    }

    public AbstractCharacter setPlayerUUID(String pUUID) {
        if (pUUID!=null && pUUID.length()!=32) throw new IllegalArgumentException("Invalid UUID");
        this.pUUID = pUUID;
        return this;
    }

    public String getPlayerName() {
        if (getPlayerUUID()==null) return null;
        OfflinePlayer p = Bukkit.getOfflinePlayer(getPlayerUUIDObject());
        if (p==null) return null;
        return p.getName();
    }

    public abstract String getName();

    public char getSex() {
        return sex;
    }

    public AbstractCharacter setSex(char sex) {
        if (sex!='M' && sex!='F') throw new IllegalArgumentException("Sex can only be 'M' or 'F'");
        this.sex = sex;
        return this;
    }

    public String getRace() {
        return race;
    }

    public AbstractCharacter setRace(String race) {
        this.race = race;
        return this;
    }

    public float getHeight() {
        return height;
    }

    public AbstractCharacter setHeight(float height) {
        this.height = height;
        return this;
    }

    public float getWeight() {
        return weight;
    }

    public AbstractCharacter setWeight(float weight) {
        this.weight = weight;
        return this;
    }

    // --- Methods ---

    public JSONObject toJSON(boolean details, boolean skipFilter) {
        JSONObject res = new JSONObject();
        res.put("name",getName());
        res.put("sex",getSex()=='M'?"male":"female");
        res.put("race",getRace());
        if (details) {
            res.put("height", getHeight());
        }
        res.put("player",getPlayerUUID());
        res.put("playerName",getPlayerName());
        return res;
    }

    @Override
    public JSONObject toJSON() {
        return toJSON(false, false);
    }

    public boolean userCanView(APIUser u) {
        if (u.hasPerm(Character.PERM_VIEW_ALL_CHARACTERS)) return true;
        return userIsOwner(u);
    }

    public boolean userCanUse(APIUser u) {
        if (u.hasPerm(Character.PERM_EDIT_ALL_CHARACTERS)) return true;
        return userIsOwner(u);
    }

    public boolean userIsOwner(APIUser u) {
        if (!(u instanceof APIForumUser)) return false;
        return getPlayerUUIDObject().equals(((APIForumUser) u).getMinecraftUUID());
    }
}
