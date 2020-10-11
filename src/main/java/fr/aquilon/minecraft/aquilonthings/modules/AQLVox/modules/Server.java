package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.modules;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLSeason.AQLSeason;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.MissingPermissionEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIUser;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A global API module to expose general information
 * @author Billi
 */
public class Server extends APIModule {
    public static final String MODULE_NAME = "server";

    public static final String SUBERR_MISSING_COMMAND = "101";
    public static final String SUBERR_NO_SEASONS = "201";

    private final AQLSeason seasons;

    public Server(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        seasons = AquilonThings.instance.getModuleData(AQLSeason.class);
    }

    public static final String ROUTE_POST_COMMAND = "serverCommand";
    public static final String ROUTE_GET_TIME = "serverTimeRoot";
    public static final String ROUTE_GET_TIME_SEASON = "serverTimeSeason";
    public static final String ROUTE_GET_TIME_TIME = "serverTimeTime";

    @Override
    public void onReady() {
        registerRoute(ROUTE_POST_COMMAND, NanoHTTPD.Method.POST, "/command", this::runCommand);
        registerRoute(ROUTE_GET_TIME, NanoHTTPD.Method.GET, "/time/", this::getTimeInfos);
        registerRoute(ROUTE_GET_TIME_SEASON, NanoHTTPD.Method.GET, "/time/season", this::getSeason);
        registerRoute(ROUTE_GET_TIME_TIME, NanoHTTPD.Method.GET, "/time/time", this::getTime);
    }

    public JSONObject runCommand(APIRequest r) throws APIException {
        APIUser usr = r.getUser();
        JSONObject data = r.getJSONRequest();
        String commandLine;
        try {
            commandLine = data.getString("command");
        } catch (JSONException ex) {
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_MISSING_COMMAND, "Missing command.");
        }
        if (commandLine.startsWith("/")) commandLine = commandLine.substring(1);
        String command = commandLine.split(" ",2)[0];
        PluginCommand pCmd = Bukkit.getPluginCommand(command);
        String pluginName = "minecraft";
        String cmdDesc = null;
        if (pCmd!=null) {
            pluginName = pCmd.getPlugin().getName();
            cmdDesc = pCmd.getDescription();
        }
        String perm = MODULE_NAME+".command."+pluginName+'.'+command;
        if (!usr.hasPerm(perm)) throw new MissingPermissionEx(usr, perm);
        final String cmdLine = commandLine;
        Bukkit.getScheduler().runTask(AquilonThings.instance, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdLine));
        JSONObject body = new JSONObject();
        body.put("command", command);
        body.put("plugin", pluginName);
        body.put("desc", cmdDesc);
        body.put("commandLine", commandLine);
        return body;
    }

    public JSONObject getTimeInfos(APIRequest r) throws APIException {
        if (seasons==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_NO_SEASONS,
                "Seasons are not defined");
        JSONObject res = new JSONObject();
        res.put("season", getSeason(r));
        res.put("time", getTime(r));
        return res;
    }

    public JSONObject getSeason(APIRequest r) throws APIException {
        if (seasons==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_NO_SEASONS,
                "Seasons are not defined");
        int seasonID = seasons.getSeason();
        int year = seasons.getYear();
        JSONObject res = new JSONObject();
        res.put("seasonID", seasonID);
        res.put("seasonName", seasons.getSeasonLabelFromID(seasonID));
        res.put("year", year);
        res.put("text", seasons.getSeasonLabelFromID(seasonID)+" "+year);
        return res;
    }

    public JSONObject getTime(APIRequest r) throws APIException {
        if (seasons==null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_NO_SEASONS,
                "Seasons are not defined");
        World world = Bukkit.getWorld(AquilonThings.instance.getConfig().getString("mainMap", "world"));
        if (world==null) {
            getLogger().mWarning("Couldn't serve time, default world not found");
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "World not found");
        }
        long worldTime = world.getFullTime();
        JSONObject res = new JSONObject();
        res.put("hour", seasons.getHour(worldTime));
        res.put("minute", seasons.getMinute(worldTime));
        res.put("text", seasons.getHoraire(worldTime));
        return res;
    }
}
