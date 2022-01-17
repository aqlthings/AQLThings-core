package fr.aquilon.minecraft.aquilonthings.modules.AQLFire;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.IWebsocket;
import org.bukkit.event.EventHandler;
import org.json.JSONObject;

import static fr.aquilon.minecraft.utils.JSONUtils.jsonDate;

/**
 * APIModule to expose AQLFire data
 * TODO: Add routes to expose last fires over HTTP API
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class FireHttpAPI extends APIModule {
    public static final String MODULE_NAME = "fire";

    private final AQLFire fire;
    private IWebsocket ws;

    public FireHttpAPI(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        fire = AquilonThings.instance.getModuleData(AQLFire.class);
        if (fire == null) throw new IllegalStateException("AQLFire is not enabled");
    }

    @Override
    public void onReady() {
        APIModule websocketModule = getServer().getModule("ws");
        ws = (websocketModule instanceof IWebsocket) ? (IWebsocket) websocketModule : null;
    }

    @EventHandler
    public void onFire(AQLFireEvent evt) {
        if (ws == null) return;
        JSONObject res = evt.toJSON();
        res.put("time", jsonDate(System.currentTimeMillis()));
        ws.submitWSMessage("server.fire", res);
    }
}
