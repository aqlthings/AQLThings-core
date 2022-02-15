package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.routing;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.HTMLResponse;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server.IWebsocket;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;

/**
 * An handler to process requests made to some API routes.
 * <p>
 *     Made up of two components the handler itself which produces a response in its own format
 *     and a converter that handles the production of an HTTP response to send back to the client.
 * </p>
 * <p>
 *     This class provides some standard converters as static members.
 * </p>
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class RouteHandler<T> {
    private final GenericRouteHandler<T> handler;
    private final Function<T, NanoHTTPD.Response> converter;

    public RouteHandler(GenericRouteHandler<T> handler, Function<T, NanoHTTPD.Response> converter) {
        this.handler = Objects.requireNonNull(handler);
        this.converter = Objects.requireNonNull(converter);
    }

    public T accept(APIRequest r) throws APIException {
        return handler.apply(r);
    }

    public final NanoHTTPD.Response handle(APIRequest r) throws APIException {
        return converter.apply(accept(r));
    }

    // --- Specific Route handlers ---

    /**
     * Specific Route Handler for Websockets
     */
    public static class Websocket extends RouteHandler<NanoHTTPD.Response> {

        public Websocket(IWebsocket websocket) {
            super(createWebsocketHandler(websocket), DEFAULT);
        }

        public static GenericRouteHandler<NanoHTTPD.Response> createWebsocketHandler(IWebsocket websocket) {
            return r -> APIServer.createWebSocket(r.getSession(), websocket, r.getUser());
        }
    }

    // --- Static Helpers ---

    public static NanoHTTPD.Response newJSONSuccessResponse(JSONObject json) {
        JSONObject res = new JSONObject();
        res.put("data",json);
        res.put("status", "success");
        return newJSONResponse(res);
    }

    public static NanoHTTPD.Response newJSONResponse(JSONObject json) {
        return newJSONResponse(NanoHTTPD.Response.Status.OK, json);
    }

    public static NanoHTTPD.Response newJSONResponse(NanoHTTPD.Response.Status code, JSONObject json) {
        return NanoHTTPD.newFixedLengthResponse(code, "application/json; charset=utf-8", json.toString());
    }

    public static NanoHTTPD.Response newTextResponse(NanoHTTPD.Response.Status code, String text) {
        return NanoHTTPD.newFixedLengthResponse(code, "text/plain; charset=utf-8", text);
    }

    public static NanoHTTPD.Response newHTMLResponse(NanoHTTPD.Response.Status code, String html) {
        return NanoHTTPD.newFixedLengthResponse(code, "text/html; charset=utf-8", html);
    }

    // --- Response converters ---

    /**
     * A converter that does nothing
     */
    public static final Function<NanoHTTPD.Response, NanoHTTPD.Response> DEFAULT = r -> r;

    /**
     * A converter that returns the specified JSON as the result directly (no status wrapper)
     */
    public static final Function<JSONObject, NanoHTTPD.Response> RAW_JSON = RouteHandler::newJSONResponse;

    /**
     * A converter that returns the specified JSON wrapped in a status object
     */
    public static final Function<JSONObject, NanoHTTPD.Response> JSON = RouteHandler::newJSONSuccessResponse;

    /**
     * A converter that returns the specified HTML
     */
    public static final Function<HTMLResponse, NanoHTTPD.Response> HTML = r -> RouteHandler.newHTMLResponse(r.getCode(), r.getHTML());

    /**
     * A converter that returns the specified text as UTF-8 plain text
     */
    public static final Function<String, NanoHTTPD.Response> TEXT = r -> RouteHandler.newTextResponse(NanoHTTPD.Response.Status.OK, r);

    /**
     * @param contentType The MIME content-type
     * @return converter that stream the response with a custom content type
     */
    public static Function<InputStream, NanoHTTPD.Response> streaming(final String contentType) {
         return is -> NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, contentType, is);
    }
}
