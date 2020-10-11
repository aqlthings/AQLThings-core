package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions;

import fi.iki.elonen.NanoHTTPD;
import org.json.JSONObject;

/**
 * Created by Billi on 29/09/2018.
 *
 * @author Billi
 */
public class RedirectionEx extends APIException {
    private String location;
    private JSONObject info;

    public RedirectionEx(String location) {
        this(location, null);
    }

    public RedirectionEx(String location, String msg) {
        super(NanoHTTPD.Response.Status.REDIRECT_SEE_OTHER, msg);
        this.location = location;
        this.info = new JSONObject();
    }

    public String getLocation() {
        return location;
    }

    @Override
    public APIException addData(String key, Object o) {
        info.put(key, o);
        return this;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject body = new JSONObject();
        body.put("status","success");
        JSONObject data = new JSONObject();
        data.put("state", "redirect");
        data.put("location", location);
        if (getMessage()!=null) data.put("message", getMessage());
        data.put("info", info);
        body.put("data", data);
        return body;
    }

    @Override
    public NanoHTTPD.Response getResponse() {
        NanoHTTPD.Response res = super.getResponse();
        res.addHeader("Location", getLocation());
        return res;
    }
}
