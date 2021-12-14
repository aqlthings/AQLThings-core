package fr.aquilon.minecraft.aquilonthings.modules.AQLCalendar;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.module.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.json.JSONObject;

/**
 * APIModule to expose AQLCalendar data
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class CalendarHttpAPI extends APIModule {
    public static final String MODULE_NAME = "calendar";

    public static final String SUBERR_NO_MAIN_WORLD = "101";
    public static final String SUBERR_WORLD_NOT_FOUND = "102";

    private final AQLCalendar calendar;

    public CalendarHttpAPI(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        calendar = AquilonThings.instance.getModuleData(AQLCalendar.class);
        if (calendar == null) throw new IllegalStateException("AQLCalendar is not enabled");
    }

    @Override
    public void onReady() {
        registerRoute("getMainWorldInfo", NanoHTTPD.Method.GET, "/main", this::getWorldInfo);
        registerRoute("getAllWorldsInfo", NanoHTTPD.Method.GET, "/worlds", this::getAllWorldsInfo);
        registerRoute("getWorldInfo", NanoHTTPD.Method.GET, "/worlds/{string:world}", this::getWorldInfo);
        registerRoute("getWorldSeason", NanoHTTPD.Method.GET, "/worlds/{string:world}/season", this::getWorldSeason);
        registerRoute("getWorldTime", NanoHTTPD.Method.GET, "/worlds/{string:world}/time", this::getWorldTime);
    }

    public JSONObject getAllWorldsInfo(APIRequest req) {
        JSONObject res = new JSONObject();
        for (World world : Bukkit.getWorlds()) {
            res.put(world.getName(), calendar.getWorldCalendar(world.getName()).toJSON(false));
        }
        return res;
    }

    public JSONObject getWorldInfo(APIRequest req) throws APIError {
        String worldName;
        if (req.hasArg("world")) worldName = req.getArg("world").getAsString();
        else {
            worldName = AquilonThings.instance.getConfig().getString("mainWorld", null);
            if (worldName == null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND,
                    SUBERR_NO_MAIN_WORLD, "Main world not defined");
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND,
                SUBERR_WORLD_NOT_FOUND, "No such world").addData("world", worldName);
        WorldCalendar cal = calendar.getWorldCalendar(world.getName());
        return cal.toJSON(true);
    }

    public JSONObject getWorldSeason(APIRequest req) {
        // TODO
        return null;
    }

    public JSONObject getWorldTime(APIRequest req) {
        // TODO
        return null;
    }

    // ----------------------------- Old routes

    /*
    public JSONObject getTimeInfos(APIRequest r) throws APIException {
        JSONObject res = new JSONObject();
        res.put("season", getSeason(r));
        res.put("time", getTime(r));
        return res;
    }

    public JSONObject getSeason(APIRequest r) throws APIException {
        int seasonID = calendar.getSeason();
        int year = calendar.getYear();
        JSONObject res = new JSONObject();
        res.put("seasonID", seasonID);
        res.put("seasonName", calendar.getSeasonLabelFromID(seasonID));
        res.put("year", year);
        res.put("text", calendar.getSeasonLabelFromID(seasonID)+" "+year);
        return res;
    }

    public JSONObject getTime(APIRequest r) throws APIException {
        World world = Bukkit.getWorld(AquilonThings.instance.getConfig().getString("mainMap", "world"));
        if (world==null) {
            getLogger().mWarning("Couldn't serve time, default world not found");
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "World not found");
        }
        long worldTime = world.getFullTime();
        JSONObject res = new JSONObject();
        res.put("hour", calendar.getHour(worldTime));
        res.put("minute", calendar.getMinute(worldTime));
        res.put("text", calendar.getHoraire(worldTime));
        return res;
    }
    */
}
