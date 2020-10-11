package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.utils.JSONExportable;
import org.json.JSONObject;

/**
 * Created by Billi on 14/04/2018.
 *
 * @author Billi
 */
public class CharOrigins implements JSONExportable {
    private String name;
    private String wiki;

    public CharOrigins(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getWiki() {
        return wiki;
    }

    public CharOrigins setWiki(String wiki) {
        this.wiki = wiki;
        return this;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("name", getName());
        res.put("wiki", getWiki());
        return res;
    }
}
