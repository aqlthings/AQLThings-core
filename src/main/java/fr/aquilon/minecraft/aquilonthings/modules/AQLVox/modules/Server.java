package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.modules;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.MissingPermissionEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIUser;
import org.bukkit.Bukkit;
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

    public Server(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
    }

    public static final String ROUTE_POST_COMMAND = "serverCommand";

    @Override
    public void onReady() {
        registerRoute(ROUTE_POST_COMMAND, NanoHTTPD.Method.POST, "/command", this::runCommand);
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
}
