package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users.APIUser;
import fr.aquilon.minecraft.utils.JSONExportable;
import org.bukkit.command.CommandSender;
import org.json.JSONObject;

/**
 * Created by Billi on 14/04/2018.
 *
 * @author Billi
 */
public class CharRace implements JSONExportable {
    private String name;
    private String infos;
    private String category;
    private float minHeight, maxHeight;
    private float minWeight, maxWeight, defWeight;

    public CharRace(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getInfos() {
        return infos;
    }

    public CharRace setInfos(String infos) {
        this.infos = infos;
        return this;
    }

    public String getCategory() {
        return category;
    }

    public CharRace setCategory(String category) {
        this.category = category;
        return this;
    }

    public float getMinHeight() {
        return minHeight;
    }

    public CharRace setMinHeight(float minHeight) {
        this.minHeight = minHeight;
        return this;
    }

    public float getMaxHeight() {
        return maxHeight;
    }

    public CharRace setMaxHeight(float maxHeight) {
        this.maxHeight = maxHeight;
        return this;
    }

    public float getMeanHeight() {
        return (getMaxHeight()+getMinHeight())/2;
    }

    public float getMinWeight() {
        return minWeight;
    }

    public CharRace setMinWeight(float minWeight) {
        this.minWeight = minWeight;
        return this;
    }

    public float getMaxWeight() {
        return maxWeight;
    }

    public CharRace setMaxWeight(float maxWeight) {
        this.maxWeight = maxWeight;
        return this;
    }

    public float getDefWeight() {
        return defWeight;
    }

    public CharRace setDefWeight(float defWeight) {
        this.defWeight = defWeight;
        return this;
    }

    public static boolean userCanUse(APIUser user, String category) {
        return user.hasPerm(Character.PERM_USE_RACE_CATEGORY.replace("{{CATEGORY}}",category));
    }

    public boolean userCanUse(APIUser user) {
        return userCanUse(user, getCategory());
    }

    public static boolean userCanUse(CommandSender sender, String category) {
        return sender.hasPermission(AquilonThings.PERM_ROOT+"."+
                Character.PERM_USE_RACE_CATEGORY.replace("{{CATEGORY}}",category));
    }

    public boolean userCanUse(CommandSender sender) {
        return userCanUse(sender, getCategory());
    }

    @Override
    public JSONObject toJSON() {
        return toJSON(false);
    }

    public JSONObject toJSON(boolean skipFilter) {
        JSONObject res = new JSONObject();
        res.put("name", getName());
        res.put("infos", getInfos());
        JSONObject height = new JSONObject();
        height.put("min", minHeight);
        height.put("max", maxHeight);
        res.put("height", height);
        // weight ?
        if (!skipFilter) res.put("category", getCategory());
        return res;
    }
}
