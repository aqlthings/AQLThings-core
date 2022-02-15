package fr.aquilon.minecraft.aquilonthings.modules.AQLBabel;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.APIServer;
import fr.aquilon.minecraft.utils.JSONPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * APIModule to expose AQLBabel data
 * TODO: Websocket event when selecting language
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class BabelHttpAPI extends APIModule {
    public static final String MODULE_NAME = "babel";

    private final AQLBabel babel;

    public BabelHttpAPI(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        babel = AquilonThings.instance.getModuleData(AQLBabel.class);
        if (babel == null) throw new IllegalStateException("AQLBabel is not enabled");
    }

    @Override
    public void onReady() {
        registerRoute("getPlayerSelectedLanguages", NanoHTTPD.Method.GET, "/players", this::getPlayerSelectedLanguages);
        registerRoute("getPlayerLanguages", NanoHTTPD.Method.GET, "/players/{uuid:player}", this::getPlayerLanguages);
        registerRoute("getLanguages", NanoHTTPD.Method.GET, "/languages", this::getLanguages);
    }

    public JSONObject getPlayerSelectedLanguages(APIRequest r) {
        JSONObject res = new JSONObject();
        for (Player p : Bukkit.getOnlinePlayers()) {
            BabelPlayer info = babel.getPlayerInfo(p);
            String uuid = p.getUniqueId().toString().replaceAll("-","");
            JSONObject pInfo = new JSONObject();
            pInfo.put("player", JSONPlayer.toJSON(p, false));
            Language selectedLang = info.getSelectedLanguage();
            pInfo.put("selectedLanguage", selectedLang != null ? selectedLang.toJSON() : JSONObject.NULL);
            res.put(uuid, pInfo);
        }
        return res;
    }

    public JSONObject getPlayerLanguages(APIRequest r) throws APIError {
        JSONObject res = new JSONObject();
        OfflinePlayer p = r.getArg("player").getAsPlayer();
        BabelPlayer info = babel.getPlayerInfo(p);
        res.put("player", JSONPlayer.toJSON(p, false));
        Language selectedLang = info.getSelectedLanguage();
        res.put("selectedLanguage", selectedLang != null ? selectedLang.toJSON() : JSONObject.NULL);
        JSONArray known = new JSONArray();
        for (BabelPlayer.PlayerLanguage lang : info.getLanguages()) {
            JSONObject langJson = lang.toJSON();
            Language language = babel.getLanguage(lang.getLanguage());
            langJson.put("language", language.toJSON());
            known.put(langJson);
        }
        res.put("knownLanguages", known);
        return res;
    }

    public JSONObject getLanguages(APIRequest r) {
        JSONObject res = new JSONObject();
        for (Language lang : babel.getLanguages())
            res.put(lang.getKey(), lang.toJSON());
        return res;
    }
}
