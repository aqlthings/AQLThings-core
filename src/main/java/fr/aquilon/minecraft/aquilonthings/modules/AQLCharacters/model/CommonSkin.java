package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import org.bukkit.ChatColor;
import org.json.JSONObject;

/**
 * Created by Billi on 13/11/2018.
 *
 * @author Billi
 */
public class CommonSkin extends AbstractSkin {
    private int id;
    private String category;

    public CommonSkin(int id, String name) {
        super(name);
        this.id = id;
    }

    // --- Accessors ---

    public int getID() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public CommonSkin setCategory(String category) {
        this.category = category;
        return this;
    }

    @Override
    public JSONObject toJSON() {
        return toJSON(false);
    }

    public JSONObject toJSON(boolean showFilter) {
        JSONObject res = super.toJSON();
        res.put("id", getID());
        if (showFilter) res.put("category", getCategory());
        return res;
    }

    @Override
    public String getLabel() {
        return "skin commun, "+ChatColor.WHITE+getName()+ChatColor.YELLOW+
                ", categorie "+ChatColor.WHITE+getCategory();
    }
}
