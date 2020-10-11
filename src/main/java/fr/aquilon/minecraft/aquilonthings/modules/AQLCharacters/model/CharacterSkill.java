package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.utils.JSONExportable;
import org.json.JSONObject;

/**
 * Created by Billi on 19/03/2018.
 *
 * @author Billi
 */
public class CharacterSkill implements JSONExportable {
    private int charID;
    private String category;
    private boolean categoryUnlocked;
    private String name;
    private int level;
    private String comment;

    public CharacterSkill(int charID, String category, String name) {
        this.charID = charID;
        this.category = category;
        this.categoryUnlocked = false;
        this.name = name;
        this.level = 0;
        this.comment = null;
    }

    // --- Accessors ---

    public int getCharID() {
        return charID;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public CharacterSkill setLevel(int level) {
        this.level = level;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public CharacterSkill setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public boolean isCategoryUnlocked() {
        return categoryUnlocked;
    }

    public CharacterSkill setCategoryUnlocked(boolean categoryUnlocked) {
        this.categoryUnlocked = categoryUnlocked;
        return this;
    }

    // --- Methods ---

    public int getBonus() {
        if (!categoryUnlocked) return 0;
        return getLevel()*2+1;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("category", getCategory());
        res.put("categoryUnlocked", isCategoryUnlocked());
        res.put("name", getName());
        res.put("level", getLevel());
        res.put("bonus", getBonus());
        if (getComment()!=null) res.put("comment", getComment());
        return res;
    }

    public String toPacketData() {
        return getCategory()+"::"+getName()+"="+getLevel();
    }

    @Override
    public String toString() {
        return toPacketData();
    }
}
