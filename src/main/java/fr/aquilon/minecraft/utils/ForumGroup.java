package fr.aquilon.minecraft.utils;

import org.json.JSONObject;

/**
 * Created by Billi on 17/11/2018.
 *
 * @author Billi
 */
public class ForumGroup implements JSONExportable {

    private int id;
    private String name;
    private String color;
    private boolean system;

    public ForumGroup(int id) {
        this(id, null);
    }

    public ForumGroup(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isSystem() {
        return system;
    }

    public void setSystem(boolean system) {
        this.system = system;
    }

    public static ForumGroup fromJSON(JSONObject o) {
        ForumGroup res = new ForumGroup(
                Integer.parseInt(o.get("id").toString()),
                o.has("name") ? o.getString("name") : null
        );
        if (o.get("color")!=null) res.setColor((String) o.get("color"));
        if (o.get("system")!=null) res.setSystem((Boolean) o.get("system"));
        return res;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("id", getID());
        res.put("name", getName());
        if (getColor()!=null) res.put("color", getColor());
        res.put("system", isSystem());
        return res;
    }
}
