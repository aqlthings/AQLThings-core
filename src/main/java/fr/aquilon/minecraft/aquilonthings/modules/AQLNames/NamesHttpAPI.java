package fr.aquilon.minecraft.aquilonthings.modules.AQLNames;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.IWebsocket;
import fr.aquilon.minecraft.utils.JSONPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.json.JSONObject;

import static fr.aquilon.minecraft.utils.JSONUtils.jsonDate;

/**
 * APIModule to expose AQLNames data
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class NamesHttpAPI extends APIModule {
    public static final String MODULE_NAME = "names";

    private final AQLNames names;
    private IWebsocket ws;

    public NamesHttpAPI(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        names = AquilonThings.instance.getModuleData(AQLNames.class);
        if (names == null) throw new IllegalStateException("AQLNames is not enabled");
    }

    @Override
    public void onReady() {
        registerRoute("getPlayerNamesList", NanoHTTPD.Method.GET, "/list", this::getPlayerNameList);
        APIModule websocketModule = getServer().getModule("ws");
        if (websocketModule instanceof IWebsocket) {
            ws = (IWebsocket) websocketModule;
        } else ws = null;
    }

    public JSONObject getPlayerNameList(APIRequest r) {
        JSONObject res = new JSONObject();
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerInfo info = names.getPlayerInfo(p.getUniqueId());
            String uuid = p.getUniqueId().toString().replaceAll("-","");
            JSONObject pInfo = new JSONObject();
            pInfo.put("player", JSONPlayer.toJSON(p, false));
            pInfo.put("name", info.getName() != null ? info.getName() : JSONObject.NULL);
            pInfo.put("desc", info.getDescription(null) != null ? info.getDescription(null) : JSONObject.NULL);
            res.put(uuid, pInfo);
        }
        return res;
    }

    @EventHandler
    public void onPlayerNameChange(PlayerNameChangeEvent evt) {
        if (ws == null) return;
        JSONObject res = evt.toJSON();
        res.put("time", jsonDate(System.currentTimeMillis()));
        ws.submitWSMessage("players."+evt.getPlayer().getUniqueId().toString().replace("-","")+".name", res);
    }

    @EventHandler
    public void onPlayerDescriptionChange(PlayerDescriptionChangeEvent evt) {
        if (ws == null) return;
        JSONObject res = evt.toJSON();
        res.put("time", jsonDate(System.currentTimeMillis()));
        ws.submitWSMessage("players."+evt.getPlayer().getUniqueId().toString().replace("-","")+".desc", res);
    }
}
