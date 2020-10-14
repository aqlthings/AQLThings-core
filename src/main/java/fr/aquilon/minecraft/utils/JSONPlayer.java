package fr.aquilon.minecraft.utils;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Helper class to get player informations as JSON
 * @author Billi
 */
public class JSONPlayer implements JSONExportable {
    private Player p;

    public JSONPlayer(Player player) {
        this.p = player;
    }

    public Player getPlayer() {
        return p;
    }

    @Override
    public JSONObject toJSON() {
        return toJSON(p, false);
    }

    public static JSONObject toJSON(OfflinePlayer p, boolean details) {
        if (p==null) return null;
        JSONObject res = new JSONObject();
        UUID uuid = p.getUniqueId();
        res.put("uuid", uuid.toString().replaceAll("-",""));
        res.put("name", p.getName());
        //List<PermissionGroup> groups =  PermissionsEx.getPermissionManager().getUser(uuid).getParents();
        //res.put("rank", groups.size()>0 ? groups.get(0).getName() : JSONObject.NULL);
        res.put("rank", "default"); // TODO: Resolve player rank
        String color = Utils.getPlayerColor(uuid);
        res.put("color", color.length()>1 ? JSONUtils.jsonColor(color.charAt(1)) : JSONObject.NULL);
        res.put("online", p.isOnline());
        if (details) res.put("banned", p.isBanned());
        if (p.isOnline()) {
            Player pOn = (Player) p;
            res.put("roleplayName", JSONUtils.jsonColoredString(pOn.getDisplayName()));
            if (details) {
                JSONObject stats = new JSONObject();
                stats.put("PV", pOn.getHealth());
                stats.put("food", pOn.getFoodLevel());
                stats.put("saturation", pOn.getSaturation());
                stats.put("gamemode", pOn.getGameMode().name());
                if (pOn.getRemainingAir() != pOn.getMaximumAir()) stats.put("air", pOn.getRemainingAir());
                stats.put("xp", pOn.getExp());
                stats.put("xpLvl", pOn.getExpToLevel());
                res.put("stats", stats);
                res.put("position", JSONUtils.jsonLocation(pOn.getLocation(), true));
                res.put("environment", JSONUtils.jsonBlockEnvironment(pOn.getLocation().getBlock()));
            }
        } else if (details) {
            res.put("lastPlayed", (p.hasPlayedBefore() ? JSONUtils.jsonDate(p.getLastPlayed()) : JSONObject.NULL));
        }
        return res;
    }
}
