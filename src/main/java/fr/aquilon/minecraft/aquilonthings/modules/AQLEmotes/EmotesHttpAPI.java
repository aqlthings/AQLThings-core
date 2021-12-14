package fr.aquilon.minecraft.aquilonthings.modules.AQLEmotes;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.module.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.IWebsocket;
import org.bukkit.event.EventHandler;
import org.json.JSONObject;

import static fr.aquilon.minecraft.utils.JSONUtils.jsonDate;

/**
 * APIModule to expose AQLEmotes data
 * TODO: Add routes to expose player emotes over HTTP API
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class EmotesHttpAPI extends APIModule {
    public static final String MODULE_NAME = "emotes";

    private final AQLEmotes emotes;
    private IWebsocket ws;

    public EmotesHttpAPI(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        emotes = AquilonThings.instance.getModuleData(AQLEmotes.class);
        if (emotes == null) throw new IllegalStateException("AQLEmotes is not enabled");
    }

    @Override
    public void onReady() {
        APIModule websocketModule = getServer().getModule("ws");
        ws = (websocketModule instanceof IWebsocket) ? (IWebsocket) websocketModule : null;
    }

    @EventHandler
    public void onPlayerEmote(PlayerEmoteEvent evt) {
        if (ws == null) return;
        JSONObject res = evt.toJSON();
        res.put("time", jsonDate(System.currentTimeMillis()));
        ws.submitWSMessage("players."+evt.getPlayer().getUniqueId().toString().replace("-","")+".emote", res);
    }
}
