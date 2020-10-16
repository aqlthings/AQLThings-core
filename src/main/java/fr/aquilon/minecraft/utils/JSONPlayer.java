package fr.aquilon.minecraft.utils;

import fr.aquilon.minecraft.aquilonthings.utils.Rank;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
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
        res.put("online", p.isOnline());
        Rank rank = Utils.getPlayerRank(p.getUniqueId());
        res.put("rank", rank != null ? rank.getName() : JSONObject.NULL);
        res.put("color", rank != null && rank.getColor() != null ? JSONUtils.jsonColor(rank.getColor().getChar()) : JSONObject.NULL);
        if (details) {
            res.put("banned", p.isBanned());
            res.put("rankDetails", rank != null ? rank.toJSON() : JSONObject.NULL);
        }
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
