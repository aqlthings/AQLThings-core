package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.utils.JSONExportable;
import org.json.JSONObject;

/**
 * Created by Billi on 02/10/2018.
 *
 * @author Billi
 */
public class SkillCategory implements JSONExportable {
    private String name;
    private String label;
    private boolean required;

    public SkillCategory(String name) {
        this.name = name;
    }

    // --- Accessors ---

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public SkillCategory setLabel(String label) {
        this.label = label;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public SkillCategory setRequired(boolean required) {
        this.required = required;
        return this;
    }

    // --- Methods ---

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("name", getName());
        res.put("label", getLabel());
        return res;
    }
}
