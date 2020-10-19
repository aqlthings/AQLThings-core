package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.AQLVox;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.BadJsonBodyEx;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import fr.aquilon.minecraft.utils.JSONExportable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public class APIRequest implements JSONExportable {
    private APIRoute route;
    private String uri;
    private Map<String, Arg> args;
    private APIUser user;
    private NanoHTTPD.IHTTPSession session;
    private Map<String, String> requestFiles;

    public APIRequest(NanoHTTPD.IHTTPSession session, APIRoute route, String uri) {
        this.uri = uri;
        this.session = session;
        this.route = route;
        this.args = null;
        this.requestFiles = null;
    }

    public void setUser(APIUser user) {
        this.user = user;
    }

    public APIUser getUser() {
        return user;
    }

    public NanoHTTPD.IHTTPSession getSession() {
        return session;
    }

    public String getURI() {
        return uri;
    }

    public APIRoute getRoute() {
        return route;
    }

    public String getMethod() {
        return route.getMethod();
    }

    public String getModule() {
        return route.getModule();
    }

    public String getName() {
        return route.getName();
    }

    public String getFullURI() {
        return route.getModule()+getURI();
    }

    public boolean isRoot() {
        return route.isRoot();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = route.toJSON();
        res.put("uri", getURI());
        JSONObject argList = new JSONObject();
        for (Arg a : args.values()) {
            argList.put(a.getName(), a.toJSON());
        }
        res.put("args", argList);
        return res;
    }

    public Arg getArg(String name) {
        if (args==null) {
            args = new HashMap<>();
            String[] parts = getURI().substring(1).split("/");
            for (int i=0; i<parts.length; i++) {
                APIRoute.RouteArg routeArg = route.getArgFromPosition(i);
                if (routeArg==null) continue;
                args.put(routeArg.getName(), new Arg(routeArg, parts[i]));
            }
        }
        return args.get(name);
    }

    public boolean hasArg(String name) {
        return getArg(name)!=null;
    }

    /**
     * index starts at 1
     */
    public Arg getArg(int index) {
        return getArg(route.getArg(index).getName());
    }

    public int getArgCount() {
        return route.getArgCount();
    }

    public String getPermName() {
        return APIRoute.PERM_PREFIX_ROUTE +
                getModule() +
                getURI().replace('/','.') +
                (getURI().equals("/") ? "root." : ".") +
                getMethod().toLowerCase();
    }

    public boolean check(String tName) {
        return getRoute().getName().equals(tName);
    }

    private APIRequest setBody(Map<String, String> body) {
        requestFiles = body;
        return this;
    }

    /**
     * Returns the files in the session body.
     * <p>
     *     If verb is POST, content will be in <code>postData</code>,<br/>
     *     Otherwise content will be saved to a temp file and it's path is in <code>content</code>
     * </p>
     * @return The files map
     */
    public Map<String, String> getRequestFiles() {
        return requestFiles;
    }

    /**
     * Parses the content of the body depending on the HTTP verb as a JSON object
     * @return A JSONObject name
     * @throws APIError If unable to parse JSON session body
     */
    public JSONObject getJSONRequest() throws APIError {
        Map<String, String> files = getRequestFiles();
        try {
            if (session.getMethod()==NanoHTTPD.Method.POST) {
                String postData = files.get("postData");
                if (postData==null) throw new BadJsonBodyEx("Empty JSON body.");
                return new JSONObject(postData);
            } else if (session.getMethod()==NanoHTTPD.Method.PUT) {
                String tmpFile = files.get("content");
                if (tmpFile==null || tmpFile.isEmpty()) throw new BadJsonBodyEx("Empty JSON body.");
                return new JSONObject(new FileReader(tmpFile));
            } else throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, APIError.SUBERR_INVALID_REQUEST_METHOD, "Invalid verb.");
        } catch (JSONException e) {
            throw new BadJsonBodyEx("Unable to parse JSON body.").addData("error", e.getMessage());
        } catch (IOException e) {
            AQLVox.LOGGER.log(Level.WARNING, null, "Unable to parse JSON body", e);
            throw new BadJsonBodyEx("Unable to parse JSON body.");
        }
    }

    public static APIRequest fromSession(APIServer serv, NanoHTTPD.IHTTPSession session) throws APIError {
        String method = session.getMethod().name();
        String uri = session.getUri();
        uri = uri.replaceAll("//","/");
        Map<String, String> requestFiles = new HashMap<>();
        try {
            session.parseBody(requestFiles);
        } catch (IOException | NanoHTTPD.ResponseException e) {
            AQLVox.LOGGER.log(Level.WARNING, null, "Unable to parse request body", e);
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, APIError.SUBERR_INVALID_REQUEST_BODY, "Bad request !");
        }
        if (uri.equals("/")) {
            return new APIRequest(session, APIRoute.ROOT_ROUTE, "/").setBody(requestFiles);
        }
        String[] uriParts = uri.substring(1).split("/",2);
        String subUri = "/";
        if (uriParts.length>1 && !uriParts[1].equalsIgnoreCase("root")) subUri = '/'+uriParts[1];
        if (subUri.length()>1 && subUri.endsWith("/")) subUri = subUri.substring(0, subUri.length()-1);
        if (AQLVox.instance.getServer().isWebsocketRequest(session)) method = APIRoute.METHOD_WEBSOCKET;
        String module = uriParts[0].toLowerCase();

        APIModule apiModule = serv.getModule(module);
        if (apiModule==null) return null;
        for (APIRoute route: apiModule.getRoutes()) {
            if (route.matches(method, module, subUri)) {
                return new APIRequest(session, route, subUri).setBody(requestFiles);
            }
        }
        return null;
    }

    public static class Arg implements JSONExportable {
        private APIRoute.RouteArg template;
        private String value;

        public Arg(APIRoute.RouteArg template, String value) {
            this.template = template;
            this.value = value;
        }

        public String getName() {
            return template.getName();
        }

        public APIRoute.ArgumentType getType() {
            return template.getType();
        }

        public String getAsString() {
            return value;
        }

        public int getAsInt() throws APIError {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, APIError.SUBERR_INVALID_REQUEST_PARAM,
                        "Inavlid parameter value, expected integer")
                        .addData("param", template.getName());
            }
        }

        public long getAsLong() throws APIError {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, APIError.SUBERR_INVALID_REQUEST_PARAM,
                        "Inavlid parameter value, expected long")
                        .addData("param", template.getName());
            }
        }

        public UUID getAsUUID() throws APIError {
            String dashed = Utils.addUuidDashes(value);
            if (dashed == null) {
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, APIError.SUBERR_INVALID_REQUEST_PARAM,
                        "Invalid parameter value, expected UUID")
                        .addData("param", template.getName());
            }
            return UUID.fromString(dashed);
        }

        public OfflinePlayer getAsPlayer() throws APIError {
            return Bukkit.getOfflinePlayer(getAsUUID());
        }

        public Object getObject() throws APIError {
            switch (getType()) {
                case STRING:
                    return getAsString();
                case INT:
                    return getAsInt();
                case LONG:
                    return getAsLong();
                case UUID:
                    return getAsUUID();
                default:
                    return null;
            }
        }

        @Override
        public JSONObject toJSON() {
            JSONObject res = new JSONObject();
            res.put("type", getType().name().toLowerCase());
            res.put("name", getName());
            res.put("value", value);
            return res;
        }
    }
}
