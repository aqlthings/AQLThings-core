package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.ModuleNotFoundEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.NotFoundEx;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A single API module exposing a set of routes.
 * <p>
 *     A module has a unique name and is attached to an API server.
 *     It serves requests to a set of API routes using some route handlers.
 * </p>
 * <p>
 *     Extend this class to implement a new API module.<br/>
 *     Register routes using {@link #registerRoute(String, NanoHTTPD.Method, String, GenericRouteHandler, Function)}
 *     and other methods in the {@link #onReady()} method.
 * </p>
 * @author Billi
 */
public abstract class APIModule implements Listener {
    private final String name;
    private final ModuleLogger logger;
    private final APIServer server;
    private final Map<String, RouteHandler<?>> handlers;
    private final Map<String, APIRoute> routes;

    public APIModule(String name, APIServer server, ModuleLogger logger) {
        this.name = name;
        this.server = server;
        this.logger = logger;
        this.handlers = new HashMap<>();
        this.routes = new HashMap<>();
    }

    // --- Getters ---

    public ModuleLogger getLogger() {
        return logger;
    }

    public APIServer getServer() {
        return server;
    }

    public String getName() {
        return name;
    }

    public APIRoute getRoute(String tName) {
        return routes.get(tName);
    }

    public List<APIRoute> getRoutes() {
        return new ArrayList<>(routes.values());
    }

    // --- Route registration methods ---

    /**
     * Register a standard API route for a status wrapped JSON object
     * @param tName The name of the route
     * @param method The HTTP method (verb) for this route
     * @param uri The URI of this route in this module
     * @param handler A method to be called to process requests
     */
    protected void registerRoute(String tName, NanoHTTPD.Method method, String uri, JSONRouteHandler handler) {
        registerRoute(tName, method, uri, handler, RouteHandler.JSON);
    }

    /**
     * Register an API route for a JSON object (without status)
     * @param tName The name of the route
     * @param method The HTTP method (verb) for this route
     * @param uri The URI of this route in this module
     * @param handler A method to be called to process requests
     */
    protected void registerRawJsonRoute(String tName, NanoHTTPD.Method method, String uri, JSONRouteHandler handler) {
        registerRoute(tName, method, uri, handler, RouteHandler.RAW_JSON);
    }

    /**
     * Register an API route leaving the response implementation open to you
     * @param tName The name of the route
     * @param method The HTTP method (verb) for this route
     * @param uri The URI of this route in this module
     * @param handler A method to be called to process requests
     */
    protected void registerRawRoute(String tName, NanoHTTPD.Method method, String uri, RawRouteHandler handler) {
        registerRoute(tName, method, uri, handler, RouteHandler.DEFAULT);
    }

    /**
     * Register an API route for a HTML document
     * @param tName The name of the route
     * @param method The HTTP method (verb) for this route
     * @param uri The URI of this route in this module
     * @param handler A method to be called to process requests
     */
    protected void registerHTMLRoute(String tName, NanoHTTPD.Method method, String uri, HTMLRouteHandler handler) {
        registerRoute(tName, method, uri, handler, RouteHandler.HTML);
    }

    /**
     * Register an API route with a custom response converter
     * @see RouteHandler
     * @param tName The name of the route
     * @param method The HTTP method (verb) for this route
     * @param uri The URI of this route in this module
     * @param handler A method to be called to process requests
     * @param converter A method to translate the handlers response to an HTTP response
     */
    protected <T> void registerRoute(String tName, NanoHTTPD.Method method, String uri, GenericRouteHandler<T> handler, Function<T, NanoHTTPD.Response> converter) {
        registerRouteHandler(
                new APIRoute(tName, method.name(), getName(), uri),
                new RouteHandler<>(handler, converter));
    }

    /**
     * Register a websocket API route.
     * <p>This method can only be called by modules implementing the {@link IWebsocket} interface</p>
     * @see IWebsocket
     * @param tName The name of the route
     * @param uri The URI of this route in this module
     */
    protected void registerWebsocketRoute(String tName, String uri, IWebsocket handler) {
        registerRouteHandler(
                new APIRoute(tName, APIRoute.METHOD_WEBSOCKET, getName(), uri),
                new RouteHandler.Websocket(handler));
    }

    /**
     * Register an API route
     * @param route The route parameters
     * @param handler The handler to process requests
     */
    protected void registerRouteHandler(APIRoute route, RouteHandler<?> handler) {
        routes.put(route.getName(), route);
        handlers.put(route.getName(), handler);
    }

    // ---

    /**
     * Override this method to register API routes
     */
    public void onReady() {}

    public final NanoHTTPD.Response serve(APIRequest request) throws APIException {
        if (!request.getModule().equals(getName())) throw new ModuleNotFoundEx(request.getModule());
        RouteHandler<?> handler = handlers.get(request.getName());
        if (handler!=null) return handler.handle(request);
        throw new NotFoundEx(request.getFullURI());
    }
}
