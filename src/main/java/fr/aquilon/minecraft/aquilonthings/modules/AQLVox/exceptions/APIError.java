package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.RouteHandler;
import org.json.JSONObject;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public class APIError extends APIException {
    public static final String SUBERR_MODULE_NOT_FOUND = "001";
    public static final String SUBERR_ROUTE_NOT_FOUND = "002";
    public static final String SUBERR_INVALID_AUTH = "003";
    public static final String SUBERR_PERM_MISSING = "004";
    public static final String SUBERR_INVALID_JSON = "005";
    public static final String SUBERR_INVALID_SIGNING = "006";
    public static final String SUBERR_INVALID_TOKEN = "007";
    public static final String SUBERR_INVALID_REQUEST_BODY = "008";
    public static final String SUBERR_INVALID_AUTH_MODE = "009";
    public static final String SUBERR_INVALID_APIKEY = "010";
    public static final String SUBERR_FORBIDEN_DEFAULT_USER = "011";
    public static final String SUBERR_INVALID_REQUEST_METHOD = "012";
    public static final String SUBERR_INVALID_STATIC_USER = "013";
    public static final String SUBERR_INVALID_REQUEST_PARAM = "014";
    public static final String SUBERR_INTERNAL_ERROR = "999";

    private APIErrorEnum error;
    private String subError;
    private String message;
    private JSONObject data;

    public APIError(APIErrorEnum error, String subError, String message) {
        super(error.getStatus(), message);
        this.error = error;
        if (subError.length()!=3) throw new IllegalArgumentException("Invalid subError code");
        try {
            Integer.parseInt(subError);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid subError code");
        }
        this.subError = subError;
        this.message = message;
        this.data = new JSONObject();
    }

    public APIError addData(String key, Object o) {
        // return this to allow chaining.
        this.data.put(key, o);
        return this;
    }

    public JSONObject toJSON() {
        JSONObject body = new JSONObject();
        body.put("status","error");
        JSONObject err = new JSONObject();
        err.put("status",getStatus().getRequestStatus());
        err.put("code",getCode());
        err.put("message",message);
        err.put("data", data);
        body.put("error", err);
        return body;
    }

    public APIErrorEnum getError() {
        return error;
    }

    public String getSubError() {
        return subError;
    }

    public String getCode() {
        return error.getCode()+'x'+subError;
    }

    public String getErrorMessage() {
        return message;
    }

    public NanoHTTPD.Response getResponse() {
        return RouteHandler.newJSONResponse(getStatus(), toJSON());
    }

    // Constants: error codes
    public enum APIErrorEnum {
        ERROR_NOT_FOUND("001", NanoHTTPD.Response.Status.NOT_FOUND),
        ERROR_INVALID_AUTH("002", NanoHTTPD.Response.Status.UNAUTHORIZED),
        ERROR_NOT_ALLOWED("002", NanoHTTPD.Response.Status.FORBIDDEN),
        ERROR_BAD_REQUEST("003", NanoHTTPD.Response.Status.BAD_REQUEST),
        ERROR_INTERNAL_ERROR("999", NanoHTTPD.Response.Status.INTERNAL_ERROR);

        private String error;
        private NanoHTTPD.Response.Status status;
        APIErrorEnum(String error, NanoHTTPD.Response.Status status) {
            this.error = error;
            this.status = status;
        }

        public String getCode() {
            return error;
        }

        public NanoHTTPD.Response.Status getStatus() {
            return status;
        }
    }
}
