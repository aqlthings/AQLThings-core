package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.modules;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.utils.JSONPlayer;
import fr.aquilon.minecraft.utils.JSONUtils;
import fr.aquilon.minecraft.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public class Players extends APIModule {
    public static final String MODULE_NAME = "players";

    // Removed 101
    public static final String SUBERR_PLAYER_NOT_ONLINE = "102";
    public static final String SUBERR_INVALID_PLAYER_NAME = "103";

    public Players(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
    }

    public static final String ROUTE_GET_PLAYERS = "getPlayers";
    public static final String ROUTE_GET_PLAYER_LIST = "getPlayerList";
    public static final String ROUTE_GET_PLAYER_COUNT = "getPlayerCount";
    public static final String ROUTE_GET_PLAYER_LIMIT = "getPlayerLimit";
    public static final String ROUTE_GET_PLAYER_DETAILS = "getPlayerDetails";
    public static final String ROUTE_GET_PLAYER_INV = "getPlayerInventory";
    public static final String ROUTE_GET_PLAYER_ENDER = "getPlayerEnderchest";
    public static final String ROUTE_POST_SEARCH_PLAYER = "searchPlayer";

    @Override
    public void onReady() {
        registerRoute(ROUTE_GET_PLAYERS, NanoHTTPD.Method.GET, "/", this::getPlayers);
        registerRoute(ROUTE_GET_PLAYER_LIST, NanoHTTPD.Method.GET, "/list", this::getPlayerList);
        registerRoute(ROUTE_GET_PLAYER_COUNT, NanoHTTPD.Method.GET, "/count", this::getPlayerCount);
        registerRoute(ROUTE_GET_PLAYER_LIMIT, NanoHTTPD.Method.GET, "/limit", this::getPlayerLimit);
        registerRoute(ROUTE_POST_SEARCH_PLAYER, NanoHTTPD.Method.POST, "/search", this::searchPlayer);
        registerRoute(ROUTE_GET_PLAYER_DETAILS, NanoHTTPD.Method.GET, "/p/{uuid:player}", this::getPlayerDetails);
        registerRoute(ROUTE_GET_PLAYER_INV, NanoHTTPD.Method.GET, "/p/{uuid:player}/inventory", this::getPlayerInventory);
        registerRoute(ROUTE_GET_PLAYER_ENDER, NanoHTTPD.Method.GET, "/p/{uuid:player}/enderchest", this::getPlayerEnderchest);
    }

    public JSONObject searchPlayer(APIRequest r) throws APIException {
        JSONObject req = r.getJSONRequest();
        if (req.get("name")==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_PLAYER_NAME,
                    "Invalid or missing player name");
        UUID uuid = Utils.findUsernameUUID(req.get("name").toString());
        if (uuid == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_PLAYER_NAME,
                    "Couldn't find user UUID");
        return JSONPlayer.toJSON(Bukkit.getOfflinePlayer(uuid), false);
    }

    public JSONObject getPlayerInventory(APIRequest r) throws APIException {
        OfflinePlayer offP = r.getArg("player").getAsPlayer();
        UUID uuid = offP.getUniqueId();
        if (!offP.isOnline())
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_PLAYER_NOT_ONLINE,
                "Player is not online");

        JSONObject pObj = new JSONObject();
        pObj.put("name", offP.getName());
        pObj.put("uuid", uuid.toString().replaceAll("-",""));
        Player p = offP.getPlayer();
        pObj.put("inventory", jsonInventory(p.getInventory().getContents()));
        return pObj;
    }

    public JSONObject getPlayerEnderchest(APIRequest r) throws APIException {
        OfflinePlayer offP = r.getArg("player").getAsPlayer();
        UUID uuid = offP.getUniqueId();
        if (!offP.isOnline())
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_PLAYER_NOT_ONLINE,
                    "Player is not online.");

        JSONObject pObj = new JSONObject();
        pObj.put("name", offP.getName());
        pObj.put("uuid", uuid.toString().replaceAll("-",""));
        Player p = offP.getPlayer();
        pObj.put("enderchest", jsonInventory(p.getEnderChest().getContents()));
        return pObj;
    }

    public JSONObject getPlayerDetails(APIRequest r) throws APIError {
        OfflinePlayer offP = r.getArg("player").getAsPlayer();
        return JSONPlayer.toJSON(offP, true);
    }

    public JSONObject getPlayerList(APIRequest r) {
        JSONObject body = new JSONObject();
        List<JSONObject> playerList = new ArrayList<>();
        HashMap<String, Integer> rankCount = new HashMap<>();
        for (Player p: Bukkit.getServer().getOnlinePlayers()) {
            JSONObject pInfo = JSONPlayer.toJSON(p, false);
            String rank = pInfo.getString("rank");
            rankCount.put(rank, (rankCount.containsKey(rank) ? rankCount.get(rank)+1 : 1));
            playerList.add(pInfo);
        }
        playerList.sort(Comparator.comparing(o -> o.getString("name").toLowerCase()));
        body.put("list", playerList);
        JSONObject stats = new JSONObject();
        for (String rank: rankCount.keySet()) {
            stats.put(rank, rankCount.get(rank));
        }
        body.put("stats", stats);
        return body;
    }

    public JSONObject getPlayerCount(APIRequest r) {
        JSONObject body = new JSONObject();
        body.put("status","success");
        int count = Bukkit.getServer().getOnlinePlayers().size();
        body.put("count", count);
        return body;
    }

    public JSONObject getPlayerLimit(APIRequest r) {
        JSONObject body = new JSONObject();
        int limit = Bukkit.getServer().getMaxPlayers();
        body.put("limit", limit);
        return body;
    }

    public JSONObject getPlayers(APIRequest r) {
        JSONObject body = new JSONObject();
        body.put("list", getPlayerList(r).get("list"));
        int count = Bukkit.getServer().getOnlinePlayers().size();
        body.put("count", count);
        int limit = Bukkit.getServer().getMaxPlayers();
        body.put("limit", limit);
        return body;
    }

    public static JSONArray jsonInventory(ItemStack[] inv) {
        JSONArray res = new JSONArray();
        for (ItemStack i: inv) {
            if (i==null) {
                res.put(JSONObject.NULL);
                continue;
            }
            JSONObject item = new JSONObject();
            item.put("id", i.getType().name());
            //item.put("meta", i.getItemMeta().serialize()); // TODO: find a way to get NBT
            item.put("count", i.getAmount());
            if (i.getItemMeta().hasDisplayName()) item.put("name", i.getItemMeta().getDisplayName());
            if (i.getItemMeta().hasLore()) item.put("lore", JSONUtils.jsonArray(i.getItemMeta().getLore().toArray()));
            if (i.getItemMeta().hasEnchants()) {
                JSONArray enchants = new JSONArray();
                Map<Enchantment, Integer> enchs = i.getItemMeta().getEnchants();
                for (Enchantment e : enchs.keySet()) {
                    JSONObject ench = new JSONObject();
                    ench.put("id", e.getName());
                    ench.put("name", e.toString());
                    ench.put("level", enchs.get(e));
                    enchants.put(ench);
                }
                item.put("enchants", enchants);
            }
            res.put(item);
        }
        return res;
    }
}
