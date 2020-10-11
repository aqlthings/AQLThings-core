package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.utils.JSONExportable;
import org.json.JSONObject;

/**
 * Created by Billi on 02/10/2018.
 *
 * @author Billi
 */
public class Skill implements JSONExportable {
    private String shorthand;
    private String category;
    private String name;
    private String label;

    public Skill(String category, String name) {
        this.category = category;
        this.name = name;
    }

    // --- Accessors ---

    public String getShorthand() {
        return shorthand;
    }

    public Skill setShorthand(String shorthand) {
        this.shorthand = shorthand;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public Skill setLabel(String label) {
        this.label = label;
        return this;
    }


    // --- Methods ---

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("category", getCategory());
        res.put("name", getName());
        res.put("label", getLabel());
        res.put("shorthand", getShorthand().toUpperCase());
        return res;
    }
}
