package fr.aquilon.minecraft.aquilonthings.modules.AQLMarkers;

import fr.aquilon.minecraft.utils.JSONExportable;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.permissions.Permissible;
import org.json.JSONObject;

/**
 * Interface for a simple marker
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public interface IMarker extends JSONExportable {
    /**
     * @return The unique ID for this marker, it can be zero or negative if no ID has been assigned
     */
    int getId();
    Location getPosition();
    String getName();
    String getDisplayName();
    String getIcon();
    String getDescription();
    String getPermission();
    MarkerGroup getGroup();
    void setGroup(MarkerGroup group);

    /**
     * Checks if the given user has the permission to use this marker
     * @param user The user to check the permissio nagainst
     * @return <code>true</code> if the user has the permission or if there is no permission, <code>false</code> otherwise
     */
    default boolean checkPermission(Permissible user) {
        String perm = getPermission();
        if (perm == null) return true;
        boolean permRequirement = true;
        if (perm.startsWith("!")) {
            permRequirement = false;
            perm = perm.substring(1);
        }
        return user.hasPermission(perm) == permRequirement;
    }

    default String asString() {
        return (getId() > 0 ? "#"+getId()+" " : "" )+getName()+ChatColor.YELLOW+
                " ("+getPosition().getWorld().getName()+": "+
                getPosition().getBlockX()+"/"+getPosition().getBlockY()+"/"+getPosition().getBlockZ()+")";
    }

    @Override
    default JSONObject toJSON() {
        return toJSON(false);
    }

    default JSONObject toJSON(boolean details) {
        JSONObject res = new JSONObject();
        res.put("id", getId() > 0 ? getId() : JSONObject.NULL);
        res.put("name", getName());
        res.put("position", JSONUtils.jsonLocation(getPosition(), true));
        res.put("displayName", getDisplayName());
        res.put("icon", getIcon());
        if (details) {
            res.put("description", getDescription());
            res.put("permission", getPermission());
        }
        return res;
    }
}
