package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.modules;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.MissingPermissionEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users.APIUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.routing.RouteHandler;
import fr.aquilon.minecraft.utils.LimitedInputStream;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * A global API module to expose general information
 * @author Billi
 */
public class Server extends APIModule {
    public static final String MODULE_NAME = "server";

    public static final String SUBERR_MISSING_COMMAND = "101";
    public static final String SUBERR_INVALID_LOG_CATEGORY = "201";
    public static final String SUBERR_INVALID_LOG_FILE = "202";

    public Server(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
    }

    public static final String ROUTE_POST_COMMAND = "serverCommand";
    public static final String ROUTE_GET_LOGS = "getLogs";

    @Override
    public void onReady() {
        registerRoute(ROUTE_POST_COMMAND, NanoHTTPD.Method.POST, "/command", this::runCommand);
        registerRoute(ROUTE_GET_LOGS, NanoHTTPD.Method.GET, "/logs/{string:category}/{string:log}",
                this::getLogs, RouteHandler.DEFAULT);
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

    public NanoHTTPD.Response getLogs(APIRequest r) throws APIException {
        String category = r.getArg("category").getAsString();
        String logDate = r.getArg("log").getAsString();
        if (!category.equals("minecraft"))
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_LOG_CATEGORY,
                    "Unknown log category");
        if (!logDate.equals("latest"))
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_LOG_FILE,
                    "Unknown log file");
        File logsFolder;
        try {
            File serverFolder = AquilonThings.instance.getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
            logsFolder = new File(serverFolder, "logs");
            if (!logsFolder.isDirectory()) throw new FileNotFoundException("Log folder not found");
        } catch (Exception err) {
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Couldn't find log folder");
        }
        String logFile = "latest.log"; // Vary this depending on requested category and log
        File log = new File(logsFolder, logFile);
        InputStream stream;
        try {
            stream = new FileInputStream(log);
        } catch (FileNotFoundException e) {
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                    "Couldn't find log file");
        }
        Map<String, String> headers = r.getSession().getHeaders();
        NanoHTTPD.Response.Status status = NanoHTTPD.Response.Status.OK;
        String range = null;
        if (headers.containsKey("range")) {
            String[] rangeRequest = headers.get("range").split("=", 2);
            if (rangeRequest.length != 2 || !rangeRequest[0].equals("bytes")) {
                throw new APIException(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE, "Invalid range request");
            }
            if (rangeRequest[1].contains(",")) {
                throw new APIException(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE,
                        "Unsupported multi-range request");
            }
            String[] rangeValues = rangeRequest[1].split("-",2);
            long rangeStart, rangeEnd;
            try {
                rangeStart = Long.parseUnsignedLong(rangeValues[0].trim());
                String rangeEndRequest = rangeValues[1].trim();
                if (rangeEndRequest.isEmpty()) rangeEnd = -1;
                else {
                    rangeEnd = Long.parseUnsignedLong(rangeEndRequest);
                    if (rangeStart >= rangeEnd) throw new IllegalArgumentException("Invalid negative range");
                }
            } catch (Exception ex) {
                throw new APIException(NanoHTTPD.Response.Status.RANGE_NOT_SATISFIABLE, "Invalid range request");
            }
            status = NanoHTTPD.Response.Status.PARTIAL_CONTENT;
            try {
                stream.skip(rangeStart);
                if (rangeEnd > 0)
                    stream = new LimitedInputStream(stream, rangeEnd);
            } catch (IOException ex) {
                throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR,
                        "IO error while reading log file");
            }
            range = "bytes "+rangeStart+"-"+(rangeEnd > 0 ? rangeEnd : Long.MAX_VALUE)+"/*";
            // FIXME: We might be sending back an invalid range end, we should instead use multipart/byteranges
        }
        NanoHTTPD.Response res = NanoHTTPD.newChunkedResponse(status, "text/plain; charset=utf-8", stream);
        if (range != null) res.addHeader("Content-Range", range);
        return res;
    }
}
