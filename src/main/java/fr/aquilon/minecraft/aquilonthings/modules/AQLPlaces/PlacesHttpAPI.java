package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.PlayerEntersBiomeEvent;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.PlayerEntersPlaceEvent;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.PlayerLeavesPlaceEvent;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.IWebsocket;
import org.bukkit.event.EventHandler;
import org.json.JSONObject;

import static fr.aquilon.minecraft.utils.JSONUtils.jsonDate;

/**
 * APIModule to expose AQLPlaces data
 * TODO: Add routes to expose places over HTTP API
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class PlacesHttpAPI extends APIModule {
    public static final String MODULE_NAME = "places";

    private final AQLPlaces places;
    private IWebsocket ws;

    public PlacesHttpAPI(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        places = AquilonThings.instance.getModuleData(AQLPlaces.class);
        if (places == null) throw new IllegalStateException("AQLPlaces is not enabled");
    }

    @Override
    public void onReady() {
        APIModule websocketModule = getServer().getModule("ws");
        ws = (websocketModule instanceof IWebsocket) ? (IWebsocket) websocketModule : null;
    }

    @EventHandler
    public void onPlayerEntersPlace(PlayerEntersPlaceEvent evt) {
        if (ws == null) return;
        JSONObject res = evt.toJSON();
        res.put("time", jsonDate(System.currentTimeMillis()));
        ws.submitWSMessage("players."+evt.getPlayer().getUniqueId().toString().replace("-","")+".place.enter", res);
    }

    @EventHandler
    public void onPlayerLeavesPlace(PlayerLeavesPlaceEvent evt) {
        if (ws == null) return;
        JSONObject res = evt.toJSON();
        res.put("time", jsonDate(System.currentTimeMillis()));
        ws.submitWSMessage("players."+evt.getPlayer().getUniqueId().toString().replace("-","")+".place.leave", res);
    }

    @EventHandler
    public void onPlayerEntersBiome(PlayerEntersBiomeEvent evt) {
        if (ws == null) return;
        JSONObject res = evt.toJSON();
        res.put("time", jsonDate(System.currentTimeMillis()));
        ws.submitWSMessage("players."+evt.getPlayer().getUniqueId().toString().replace("-","")+".biome", res);
    }
}
