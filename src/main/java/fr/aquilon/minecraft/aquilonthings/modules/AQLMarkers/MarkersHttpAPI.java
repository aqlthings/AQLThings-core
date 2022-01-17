package fr.aquilon.minecraft.aquilonthings.modules.AQLMarkers;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import fr.aquilon.minecraft.utils.JSONPlayer;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * APIModule to expose AQLMarkers data
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class MarkersHttpAPI extends APIModule {
    public static final String MODULE_NAME = "markers";
    private AQLMarkers markers;

    public static final String SUBERR_MARKER_NOT_FOUND = "101";
    public static final String SUBERR_GROUP_NOT_FOUND = "102";
    public static final String SUBERR_PLAYER_NOT_FOUND = "103";
    public static final String SUBERR_MISSING_PLAYER_ID = "201";

    public MarkersHttpAPI(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        markers = AquilonThings.instance.getModuleData(AQLMarkers.class);
        if (markers == null) throw new IllegalStateException("AQLMarkers is not enabled");
    }

    @Override
    public void onReady() {
        registerRoute("getMarkerGroups", NanoHTTPD.Method.GET, "/", this::getMarkerGroups);
        registerRoute("getMarkerById", NanoHTTPD.Method.GET, "/marker/{int:id}", this::getMarker);
        registerRoute("teleportToMarkerById", NanoHTTPD.Method.POST, "/marker/{int:id}/teleport", this::teleportToMarker);
        registerRoute("getMarkerByName", NanoHTTPD.Method.GET, "/m/{string:name}", this::getMarker);
        registerRoute("teleportToMarkerByName", NanoHTTPD.Method.POST, "/m/{string:name}/teleport", this::teleportToMarker);
        registerRoute("getMarkerGroupById", NanoHTTPD.Method.GET, "/group/{int:id}", this::getMarkerGroup);
        registerRoute("getMarkerGroupByName", NanoHTTPD.Method.GET, "/g/{string:name}", this::getMarkerGroup);
    }

    private JSONObject getMarkerGroups(APIRequest req) {
        JSONObject res = new JSONObject();
        Set<MarkerGroup> groups = markers.getGroups();
        res.put("groups", JSONUtils.jsonArray(groups));
        return res;
    }

    private JSONObject getMarkerGroup(APIRequest req) throws APIError {
        JSONObject res = new JSONObject();
        MarkerGroup group = markers.getMainGroup();
        if (req.hasArg("id")) {
            int id = req.getArg("id").getAsInt();
            if (id != 0) group = markers.getGroup(id);
        } else if (req.hasArg("name")) {
            String groupName = req.getArg("name").getAsString();
            if (!groupName.equals("main")) group = markers.getGroup(groupName);
        }
        if (group == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_GROUP_NOT_FOUND, "No group found");
        res.put("group", group.toJSON());
        res.put("markers", JSONUtils.jsonArray(group.getMarkers()));
        return res;
    }

    private JSONObject getMarker(APIRequest req) throws APIError {
        IMarker marker = null;
        if (req.hasArg("id")) {
            int id = req.getArg("id").getAsInt();
            marker = markers.getMainGroup().getMarker(id);
        } else if (req.hasArg("name")) {
            String name = req.getArg("name").getAsString();
            marker = markers.getMainGroup().getMarker(name);
        }
        if (marker == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_MARKER_NOT_FOUND, "No marker found");
        JSONObject res = marker.toJSON(true);
        MarkerGroup group = marker.getGroup();
        res.put("group", group != null ? group.toJSON() : JSONObject.NULL);
        return res;
    }

    private JSONObject teleportToMarker(APIRequest req) throws APIError {
        IMarker marker = null;
        if (req.hasArg("id")) {
            int id = req.getArg("id").getAsInt();
            marker = markers.getMainGroup().getMarker(id);
        } else if (req.hasArg("name")) {
            String name = req.getArg("name").getAsString();
            MarkerGroup group;
            if (req.hasArg("group")) group = markers.getGroup(req.getArg("group").getAsString());
            else group = markers.getMainGroup();
            if (group == null)
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_GROUP_NOT_FOUND, "No group with this name");
            marker = group.getMarker(name);
        }
        if (marker == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_MARKER_NOT_FOUND, "No marker found");
        JSONObject json = req.getJSONRequest();
        Player target;
        try {
            UUID uuid = Objects.requireNonNull(Utils.getUUID(json.getString("uuid")));
            target = Bukkit.getPlayer(uuid);
        } catch (JSONException ex) {
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_MISSING_PLAYER_ID, "Missing target UUID");
        } catch (NullPointerException ex) {
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_MISSING_PLAYER_ID, "Invalid UUID");
        }
        if (target == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_PLAYER_NOT_FOUND, "Player not online");
        target.teleport(marker.getPosition(), PlayerTeleportEvent.TeleportCause.PLUGIN);
        JSONObject res = new JSONObject();
        res.put("marker", marker.toJSON(true));
        res.put("teleport", JSONPlayer.toJSON(target, false));
        return res;
    }
}
