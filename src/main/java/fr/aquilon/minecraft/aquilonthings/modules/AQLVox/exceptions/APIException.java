package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.routing.RouteHandler;
import org.json.JSONObject;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public class APIException extends Exception {
    private NanoHTTPD.Response.Status status;
    private JSONObject error;

    public APIException(NanoHTTPD.Response.Status status, String message) {
        super(message);
        this.status = status;
        this.error = new JSONObject();
    }

    public APIException addData(String key, Object o) {
        // return this to allow chaining.
        this.error.put(key, o);
        return this;
    }

    public JSONObject toJSON() {
        JSONObject body = new JSONObject();
        body.put("status","error");
        body.put("error", error);
        return body;
    }

    public NanoHTTPD.Response.Status getStatus() {
        return status;
    }

    public NanoHTTPD.Response getResponse() {
        return RouteHandler.newJSONResponse(status, toJSON());
    }
}
