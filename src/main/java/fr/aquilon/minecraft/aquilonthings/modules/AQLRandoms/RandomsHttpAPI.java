package fr.aquilon.minecraft.aquilonthings.modules.AQLRandoms;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.IWebsocket;
import org.bukkit.event.EventHandler;
import org.json.JSONObject;

import static fr.aquilon.minecraft.utils.JSONUtils.jsonDate;

/**
 * APIModule to expose AQLRandoms data
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class RandomsHttpAPI extends APIModule {
    public static final String MODULE_NAME = "random";

    private final AQLRandoms rand;
    private IWebsocket ws;

    public RandomsHttpAPI(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        rand = AquilonThings.instance.getModuleData(AQLRandoms.class);
        if (rand == null) throw new IllegalStateException("AQLRandoms is not enabled");
    }

    @Override
    public void onReady() {
        APIModule websocketModule = getServer().getModule("ws");
        ws = (websocketModule instanceof IWebsocket) ? (IWebsocket) websocketModule : null;
    }

    @EventHandler
    public void onRandom(AQLRandomEvent evt) {
        if (ws == null) return;
        JSONObject res = evt.toJSON();
        res.put("time", jsonDate(System.currentTimeMillis()));
        ws.submitWSMessage("players."+evt.getSender().getUniqueId().toString().replace("-","")+".random", res);
    }
}
