package fr.aquilon.minecraft.aquilonthings.modules.AQLBlessures;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.IWebsocket;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.bukkit.event.EventHandler;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

import static fr.aquilon.minecraft.utils.JSONUtils.jsonDate;

/**
 * APIModule to expose AQLBlessures data
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class BlessuresHttpAPI extends APIModule {
    public static final String MODULE_NAME = "blessures";

    public static final String SUBERR_COUNTER_NOT_FOUND = "101";
    public static final String SUBERR_PLAYER_NOT_FOUND = "102";

    private final AQLBlessures blessures;
    private IWebsocket ws;

    public BlessuresHttpAPI(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        blessures = AquilonThings.instance.getModuleData(AQLBlessures.class);
        if (blessures == null) throw new IllegalStateException("AQLBlessures is not enabled");
    }

    @Override
    public void onReady() {
        registerRoute("getCounterList", NanoHTTPD.Method.GET, "/counters", this::getCounterList);
        registerRoute("getCounterSettings", NanoHTTPD.Method.GET, "/counters/{string:counter}", this::getCounterSettings);
        registerRoute("getCounterSettings", NanoHTTPD.Method.GET, "/counters/{string:counter}/p/{uuid:player}", this::getCounterPlayerState);
        APIModule websocketModule = getServer().getModule("ws");
        ws = (websocketModule instanceof IWebsocket) ? (IWebsocket) websocketModule : null;
    }

    public JSONObject getCounterList(APIRequest req) {
        JSONObject res = new JSONObject();
        List<InjuryCounter> counters = blessures.getCounters();
        res.put("list", JSONUtils.jsonArray(counters));
        res.put("totalCount", counters.size());
        res.put("activeCount", counters.stream().filter(InjuryCounter::isActive).count());
        return res;
    }

    public JSONObject getCounterSettings(APIRequest req) throws APIError {
        String counterName = req.getArg("counter").getAsString();
        InjuryCounter counter = blessures.getCounter(counterName);
        if (counter == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_COUNTER_NOT_FOUND,
                    "No such counter").addData("counter", counterName);
        return counter.toJSON(true);
    }

    public JSONObject getCounterPlayerState(APIRequest req) throws APIError {
        String counterName = req.getArg("counter").getAsString();
        InjuryCounter counter = blessures.getCounter(counterName);
        if (counter == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_COUNTER_NOT_FOUND,
                    "No such counter").addData("counter", counterName);
        UUID playerId = req.getArg("player").getAsUUID();
        PlayerState pState = counter.getPlayerState(playerId);
        if (pState == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_PLAYER_NOT_FOUND,
                    "No such player in this counter").addData("counter", counterName);
        JSONObject res = new JSONObject();
        res.put("counter", counter.toJSON(false));
        JSONObject stateJson = pState.toJSON(true);
        JSONObject player = (JSONObject) stateJson.remove("player");
        res.put("state", stateJson);
        res.put("player", player);
        return res;
    }

    @EventHandler
    public void onInjury(InjuryEvent evt) {
        if (ws == null) return;
        JSONObject res = evt.toJSON();
        res.put("time", jsonDate(System.currentTimeMillis()));
        ws.submitWSMessage("players."+evt.getPlayer().getUniqueId().toString().replace("-","")+".injury", res);
    }
}
