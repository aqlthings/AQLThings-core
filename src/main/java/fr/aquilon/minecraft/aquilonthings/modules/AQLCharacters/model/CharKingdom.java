package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.utils.JSONExportable;
import org.json.JSONObject;

/**
 * Created by Billi on 14/04/2018.
 *
 * @author Billi
 */
public class CharKingdom implements JSONExportable {
    private String name;
    private int forumID;
    private int groupID;

    public CharKingdom(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getForumID() {
        return forumID;
    }

    public CharKingdom setForumID(int forumID) {
        this.forumID = forumID;
        return this;
    }

    public int getGroupID() {
        return groupID;
    }

    public CharKingdom setGroupID(int groupID) {
        this.groupID = groupID;
        return this;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("name", getName());
        res.put("forumID", getForumID());
        res.put("groupID", getGroupID());
        return res;
    }
}
