package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.modules;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.Context;
import fr.aquilon.minecraft.aquilonthings.module.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.AQLVox;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIRoute;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.HTMLResponse;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.RouteHandler;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Comparator;
import java.util.List;

/**
 * Created by Billi on 27/01/2019
 * @author Billi
 */
public class About extends APIModule {
    public static final String MODULE_NAME = "about";

    public About(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
    }

    public static final String ROUTE_GET_ABOUT = "getAbout";
    public static final String ROUTE_GET_VERSION = "getVersion";
    public static final String ROUTE_GET_SWAGGER = "getSwagger";
    public static final String ROUTE_GET_DOC_HOME = "getDocumentationHome";
    public static final String ROUTE_GET_DOC_MODULE = "getModuleDocumentation";
    public static final String ROUTE_GET_DOC_MODULE_ROUTE = "getModuleRoute";

    @Override
    public void onReady() {
        registerRoute(ROUTE_GET_ABOUT, NanoHTTPD.Method.GET, "/", this::getAbout, RouteHandler.HTML);
        registerRoute(ROUTE_GET_VERSION, NanoHTTPD.Method.GET, "/version", this::getVersion);
        registerRoute(ROUTE_GET_DOC_HOME, NanoHTTPD.Method.GET, "/doc", this::getDocumentationHome, RouteHandler.HTML);
        registerRoute(ROUTE_GET_DOC_MODULE, NanoHTTPD.Method.GET, "/doc/{string:module}", this::getModuleDocumentation, RouteHandler.HTML);
        registerRoute(ROUTE_GET_SWAGGER, NanoHTTPD.Method.GET, "/swagger.json", this::getSwagger, RouteHandler.RAW_JSON);
    }

    public HTMLResponse getAbout(APIRequest r) {
        HTMLResponse res = new HTMLResponse(HTMLResponse.Template.ABOUT, "About");
        res.append("<h1>About - AQLVox (v"+AQLVox.VERSION+") for AquilonThings (v"+AquilonThings.VERSION+")</h1>\n");
        res.append("<p>Nothing here right now ! Come back later</p>\n");
        res.append("<h2>Links</h2>\n");
        res.append("<ul>\n");
        res.append(1, "<li><a href=\"about/version\">Version informations</a></li>\n");
        res.append(1, "<li><a href=\"about/doc\">Documentation</a></li>\n");
        res.append("</ul>\n");
        return res;
    }

    public JSONObject getVersion(APIRequest r) {
        JSONObject res = new JSONObject();
        res.put("aqlthings", AquilonThings.VERSION);
        res.put("aqlvox", AQLVox.VERSION);
        JSONObject git = new JSONObject();
        git.put("commit", Context.GIT_COMMIT);
        git.put("branch", Context.GIT_BRANCH);
        res.put("git", git);
        res.put("generatedOn", JSONUtils.jsonDate(Context.GENERATED_ON));
        return res;
    }

    public HTMLResponse getDocumentationHome(APIRequest r) {
        HTMLResponse res = new HTMLResponse(HTMLResponse.Template.ABOUT, "Documentation");
        res.append("<h1>Documentation - AQLVox (v"+AQLVox.VERSION+") for AquilonThings (v"+AquilonThings.VERSION+")</h1>\n")
                .append("<p>Server documentation ...</p>\n<section>\n<h2>Route list</h2>\n");
        for (APIModule module : AQLVox.instance.getServer().getModules()) {
            res.append("<article>\n<h3><a href=\"doc/")
                    .append(module.getName())
                    .append("\">")
                    .append(module.getName())
                    .append("</a> (")
                    .append(module.getClass().getSimpleName())
                    .append(")</h3>\n<ul style=\"list-style-type: none; padding-left: 0;\">\n");
            List<APIRoute> routeList = module.getRoutes();
            routeList.sort(Comparator.comparing(APIRoute::getURI)); // Sort by uri
            for (APIRoute route : routeList) {
                res.append("<li style=\"margin-bottom: 3px;\">")
                        .append("<i style=\"display: inline-block; margin-right: 10px; font-style: normal;")
                        .append("text-align: center; border: 1px solid #777; border-radius: 5px; width: 90px;\">")
                        .append(route.getMethod().toUpperCase())
                        .append("</i> <span style=\"font-family: monospace;\">")
                        .append(route.getFullURI())
                        .append("</span> <b style=\"margin-left: 20px;\" title=\"Permission: ")
                        .append(route.getPermName()).append("\"><span>")
                        .append(route.getName())
                        .append("</span>(<small>")
                        .append(Utils.joinStrings(route.getArgs(), ", "))
                        .append("</small>)</b></li>\n");
            }
            res.append("</ul>\n</article>\n");
        }
        res.append("</section>");
        return res;
    }

    public HTMLResponse getModuleDocumentation(APIRequest r) throws APIError {
        APIModule m = AQLVox.instance.getServer().getModule(r.getArg("module").getAsString());
        if (m == null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, APIError.SUBERR_ROUTE_NOT_FOUND, "No such module");
        HTMLResponse res = new HTMLResponse(HTMLResponse.Template.ABOUT, "Documentation: "+m.getName());
        res.append("<h1>Documentation: "+m.getName()+" - AQLVox (v"+AQLVox.VERSION+") for AquilonThings (v"+AquilonThings.VERSION+")</h1>\n")
                .append("<p>Module documentation ...</p>\n<section>\n<h2>Route list</h2>\n")
                .append("<ul style=\"list-style-type: none; padding-left: 0;\">\n");
        List<APIRoute> routeList = m.getRoutes();
        routeList.sort(Comparator.comparing(APIRoute::getURI)); // Sort by uri
        for (APIRoute route : routeList) {
            res.append("<li style=\"margin-bottom: 3px;\">")
                    .append("<i style=\"display: inline-block; margin-right: 10px; font-style: normal;")
                    .append("text-align: center; border: 1px solid #777; border-radius: 5px; width: 90px;\">")
                    .append(route.getMethod().toUpperCase())
                    .append("</i> <span style=\"font-family: monospace;\">")
                    .append(route.getFullURI())
                    .append("</span> <b style=\"margin-left: 20px;\" title=\"Permission: ")
                    .append(route.getPermName()).append("\"><span>")
                    .append(route.getName())
                    .append("</span>(<small>")
                    .append(Utils.joinStrings(route.getArgs(), ", "))
                    .append("</small>)</b></li>\n");
        }
        res.append("</ul>\n</section>");
        return res;
    }

    public JSONObject getSwagger(APIRequest r) {
        JSONObject res = new JSONObject();
        res.put("openapi", "3.0.0");
        JSONObject info = new JSONObject();
        info.put("version", AQLVox.VERSION);
        info.put("title", "AQLVox");
        res.put("info", info);
        /*
        // TODO: servers
        JSONArray servers = new JSONArray();
        res.put("servers", servers);
        */
        JSONArray tags = new JSONArray();
        JSONObject paths = new JSONObject();
        List<APIModule> modules = AQLVox.instance.getServer().getModules();
        modules.sort(Comparator.comparing(APIModule::getName));
        for (APIModule module : modules) {
            JSONObject tag = new JSONObject();
            tag.put("name", module.getName());
            tag.put("description", module.getClass().getName());
            tags.put(tag);
            List<APIRoute> routeList = module.getRoutes();
            routeList.sort(Comparator.comparing(APIRoute::getURI)); // Sort by uri
            for (APIRoute route : routeList) {
                String routePath = route.getSwaggerURI();
                JSONObject path = paths.optJSONObject(routePath);
                if (path == null) path = new JSONObject();
                String method = route.getMethod();
                if (method.equals(APIRoute.METHOD_WEBSOCKET)) method = "GET";
                method = method.toLowerCase();
                JSONObject verb = path.optJSONObject(method);
                if (verb == null) verb = new JSONObject();
                JSONArray routeTags = new JSONArray();
                routeTags.put(module.getName());
                verb.put("tags", routeTags);
                verb.put("operationId", module.getName()+"."+route.getName());
                // TODO: summary
                APIRoute.RouteArg[] args = route.getArgs();
                if (args.length > 0) {
                    JSONArray parameters = new JSONArray();
                    for (APIRoute.RouteArg arg : args) {
                        JSONObject param = new JSONObject();
                        param.put("name", arg.getName());
                        param.put("in", "path");
                        param.put("required", true);
                        param.put("schema", arg.getType().getSchema());
                        parameters.put(param);
                    }
                    path.put("parameters", parameters);
                }
                // TODO: body parameters
                JSONObject responses = new JSONObject();
                // TODO: responses (errors + success)
                JSONObject resDef = new JSONObject();
                resDef.put("description", "The request result");
                responses.put("default", resDef);
                verb.put("responses", responses);
                path.put(method, verb);
                paths.put(routePath, path);
            }
        }
        res.put("tags", tags);
        res.put("paths", paths);
        return res;
    }
}
