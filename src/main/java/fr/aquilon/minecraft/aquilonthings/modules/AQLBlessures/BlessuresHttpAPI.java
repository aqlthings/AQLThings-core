package fr.aquilon.minecraft.aquilonthings.modules.AQLBlessures;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.IWebsocket;
import org.bukkit.event.EventHandler;
import org.json.JSONObject;

import static fr.aquilon.minecraft.utils.JSONUtils.jsonDate;

/**
 * APIModule to expose AQLBlessures data
 * TODO: Add routes to expose injury counters over HTTP API
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class BlessuresHttpAPI extends APIModule {
    public static final String MODULE_NAME = "blessures";

    private final AQLBlessures blessures;
    private IWebsocket ws;

    public BlessuresHttpAPI(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        blessures = AquilonThings.instance.getModuleData(AQLBlessures.class);
        if (blessures == null) throw new IllegalStateException("AQLBlessures is not enabled");
    }

    @Override
    public void onReady() {
        APIModule websocketModule = getServer().getModule("ws");
        ws = (websocketModule instanceof IWebsocket) ? (IWebsocket) websocketModule : null;
    }

    @EventHandler
    public void onInjury(InjuryEvent evt) {
        if (ws == null) return;
        JSONObject res = evt.toJSON();
        res.put("time", jsonDate(System.currentTimeMillis()));
        ws.submitWSMessage("players."+evt.getPlayer().getUniqueId().toString().replace("-","")+".injury", res);
    }
}
