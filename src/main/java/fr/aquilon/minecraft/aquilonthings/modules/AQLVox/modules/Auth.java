package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.modules;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.AQLVox;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.InternalServerErrorEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIUser;
import fr.aquilon.minecraft.utils.JSONUtils;
import fr.aquilon.minecraft.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by Billi on 19/03/2018
 * @author Billi
 */
public class Auth extends APIModule {
    public static final long MILLIS_HOUR = 1000*60*60;
    public static final String MODULE_NAME = "auth";

    public static final String SUBERR_INVALID_TOKEN_DUR = "101";
    public static final String SUBERR_INVALID_KEY_NAME = "102";
    public static final String SUBERR_INVALID_PERMISSIONS = "103";
    public static final String SUBERR_SOURCE_FORBIDDEN_ACTION = "201";
    public static final String SUBERR_FORBIDDEN_VIEW_APIKEY = "202";
    public static final String SUBERR_NO_SUCH_KEY = "301";

    public static final String API_KEY_NAME_REGEX = "^[a-zA-Z0-9-_#/ ]{5,64}$";

    public static final String PERM_APIKEY_VIEW_ANY = Auth.MODULE_NAME+".apikey.view.any";

    // TODO: load from config
    public static final String TOKEN_ISSUER = "fr.aquilon-mc.AQLVox";
    public static final int TOKEN_DEFAULT_DURATION = 6; // Hours
    public static final int TOKEN_MAX_DURATION = 6; // Hours
    public static final int TOKEN_MAX_DURATION_STAFF = 24; // Hours

    public static final String PERM_PROLONGED_TOKEN = "auth.token.prolong";

    private DatabaseConnector db;

    public Auth(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        db = AquilonThings.instance.getNewDatabaseConnector();
    }

    public static final String ROUTE_GET_INFO = "getInfo";
    public static final String ROUTE_GET_API_KEYS = "getApiKeys";
    public static final String ROUTE_GET_API_KEY = "getApiKey";
    public static final String ROUTE_POST_CREATE_API_KEY = "createApiKey";
    public static final String ROUTE_DELETE_API_KEY = "deleteApiKey";
    public static final String ROUTE_POST_CREATE_TOKEN = "createToken";
    public static final String ROUTE_GET_PERMS = "getPermissions";

    @Override
    public void onReady() {
        registerRoute(ROUTE_GET_INFO, NanoHTTPD.Method.GET, "/info", this::getAuthInfos);
        registerRoute(ROUTE_GET_API_KEYS, NanoHTTPD.Method.GET, "/api-keys", this::getApiKeys);
        registerRoute(ROUTE_POST_CREATE_API_KEY, NanoHTTPD.Method.POST, "/api-keys", this::createApiKey);
        registerRoute(ROUTE_GET_API_KEY, NanoHTTPD.Method.GET, "/api-keys/{string:key-id}", this::getApiKey);
        //registerRoute(ROUTE_DELETE_API_KEY, NanoHTTPD.Method.DELETE, "/api-key/{string:key-id}", this::deleteApiKey);
        registerRoute(ROUTE_POST_CREATE_TOKEN, NanoHTTPD.Method.POST, "/token", this::createToken);
        registerRoute(ROUTE_GET_PERMS, NanoHTTPD.Method.GET, "/perms", this::getPermissions);
    }

    public JSONObject getAuthInfos(APIRequest r) {
        return r.getUser().toJSON();
    }

    public JSONObject createApiKey(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER, "Default user cannot create api keys.");
        if (!user.canManageApikeys())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_SOURCE_FORBIDDEN_ACTION, "Cannot create api keys.");
        JSONObject req = r.getJSONRequest();
        String apiKeyID = Utils.randomString(8);
        if (!req.has("name") || req.get("name")==null || req.get("name").toString().length()>64)
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_KEY_NAME, "Invalid api key name.")
                    .addData("code",1).addData("error", "Missing");
        String apiKeyName = req.get("name").toString();
        if (!apiKeyName.matches(API_KEY_NAME_REGEX))
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_KEY_NAME, "Invalid api key name.")
                    .addData("code",2).addData("error", "invalid")
                    .addData("minLength", 5).addData("maxLength", 64).addData("regex", API_KEY_NAME_REGEX);
        List<String> perms = null;
        if (req.has("permissions") && req.get("permissions")!=null) {
            if (!(req.get("permissions") instanceof JSONArray))
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_PERMISSIONS, "Invalid permissions.")
                        .addData("code",1).addData("error", "Permissions must be an array of strings");
            perms = new ArrayList<>();
            JSONArray reqPerms = (JSONArray) req.get("permissions");
            for (Object o : reqPerms) {
                if (!(o instanceof String))
                    throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_PERMISSIONS, "Invalid permissions.")
                            .addData("code",2).addData("error", "Permissions must be an array of strings");
                String p = (String) o;
                if (!user.hasPerm(p))
                    throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_PERMISSIONS, "Invalid permissions.")
                            .addData("code", 3).addData("error","Requested a permission you don't have").addData("perm", p);
                perms.add(p);
            }
        }
        String apiKey = Utils.randomString(64);
        Connection con = db.startTransaction();
        PreparedStatement stmt = db.prepare(con, "INSERT INTO aqlvox_apikeys(key_id, key_name, user_type, user_id, api_key, permissions) VALUES (?,?,?,?,?,?)");
        try {
            stmt.setString(1, apiKeyID);
            stmt.setString(2, apiKeyName);
            stmt.setString(3, user.getClass().getSimpleName());
            stmt.setString(4, user.getUniqueID());
            stmt.setString(5, apiKey);
            stmt.setString(6, perms!=null ? String.join("; ", perms) : null);
            stmt.execute();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || e.getSQLState().equals("23000")) { // Key id already exists
                db.endTransaction(con); // Close db connection normally
                return createApiKey(r); // Regenerate a new one, almost impossible to end up in an infinite loop
            }
            db.endTransaction(con, e);
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR, "Error when creating api key");
        }
        db.endTransaction(con);
        getLogger().mDebug("Issued api key "+apiKeyID+" ("+apiKeyName+") to "+user.toString()+".");
        JSONObject res = new JSONObject();
        res.put("apiKey", apiKey);
        JSONObject info = new JSONObject();
        info.put("keyID", apiKeyID);
        info.put("keyName", apiKeyName);
        info.put("userType", user.getClass().getSimpleName());
        info.put("userID", user.getUniqueID());
        info.put("permissions", perms); // null means user's permissions
        res.put("info",info);
        res.put("user", user.toJSON());
        return res;
    }

    public JSONObject createToken(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER, "Default user cannot create tokens.");
        if (!user.canCreateToken())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_SOURCE_FORBIDDEN_ACTION, "Cannot create tokens.");
        JSONObject req = r.getJSONRequest();
        int tokenDuration = TOKEN_DEFAULT_DURATION;
        if (req.get("duration")!=null) {
            try {
                tokenDuration = Integer.parseUnsignedInt(req.get("duration").toString());
            } catch (NumberFormatException e) {
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_TOKEN_DUR, "Invalid token duration");
            }
            if (user.hasPerm(PERM_PROLONGED_TOKEN)) { // Can create tokens that lasts longer
                if (tokenDuration>TOKEN_MAX_DURATION_STAFF)
                    throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_TOKEN_DUR, "Invalid token duration")
                        .addData("maxDuration", TOKEN_MAX_DURATION_STAFF);
            } else if (tokenDuration>TOKEN_MAX_DURATION)
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_TOKEN_DUR, "Invalid token duration")
                    .addData("maxDuration", TOKEN_MAX_DURATION);
        }
        String secret = AQLVox.instance.getConfig("auth.token.secret");
        if (secret==null) {
            getLogger().mWarning("JWT Token secret not configured !");
            throw new InternalServerErrorEx("Unable to create Token");
        }
        String token;
        String tokenID = Utils.randomString(16);
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + tokenDuration*MILLIS_HOUR); // Token lasts x hours
        JWTCreator.Builder tokBuilder = JWT.create()
                .withIssuer(TOKEN_ISSUER)
                .withIssuedAt(issuedAt)
                .withNotBefore(issuedAt)
                .withExpiresAt(expiresAt)
                .withClaim("tok-id", tokenID)
                .withClaim("usr-type", user.getClass().getSimpleName())
                .withClaim("usr-id", user.getUniqueID());
        try {
            token = tokBuilder.sign(Algorithm.HMAC256(secret));
        } catch (UnsupportedEncodingException e) {
            getLogger().log(Level.WARNING, null, "Error when creating JWT :", e);
            throw new InternalServerErrorEx("Unable to create Token");
        }
        getLogger().mDebug("Issued token '"+tokenID+"' to "+user.toString()+" for "+tokenDuration+" hours.");
        JSONObject res = new JSONObject();
        res.put("token", token);
        JSONObject info = new JSONObject();
        info.put("issuer", TOKEN_ISSUER);
        info.put("issuedAt", JSONUtils.jsonDate(issuedAt.getTime()));
        info.put("expiresAt", JSONUtils.jsonDate(expiresAt.getTime()));
        info.put("duration", ChronoUnit.HOURS.between(issuedAt.toInstant(), expiresAt.toInstant()));
        info.put("userType", user.getClass().getSimpleName());
        info.put("userID", user.getUniqueID());
        info.put("tokenID", tokenID);
        res.put("info",info);
        res.put("user", user.toJSON());
        return res;
    }

    public JSONObject getPermissions(APIRequest r) {
        JSONObject res = new JSONObject();
        res.put("perms",r.getUser().getPermList());
        return res;
    }

    public JSONObject getApiKeys(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER, "Default user cannot list api keys.");
        if (!user.canManageApikeys())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_SOURCE_FORBIDDEN_ACTION, "Cannot list api keys.");

        Connection con = db.startTransaction();
        PreparedStatement stmt = db.prepare(con, "SELECT * FROM aqlvox_apikeys WHERE user_type = ? AND user_id = ?");
        JSONArray keyList = new JSONArray();
        try {
            stmt.setString(1, user.getClass().getSimpleName());
            stmt.setString(2, user.getUniqueID());
            ResultSet res = stmt.executeQuery();
            while (res.next()) {
                JSONObject k = new JSONObject();
                k.put("id", res.getString("key_id"));
                k.put("name", res.getString("key_name"));
                String permsStr = res.getString("permissions");
                List<String> perms = permsStr!=null ? Arrays.asList(permsStr.split("; ")) : null;
                k.put("permissions", perms != null ? perms : JSONObject.NULL); // null means user's permissions
                keyList.put(k);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e);
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR, "Error when listing api keys");
        }
        db.endTransaction(con);
        JSONObject json = new JSONObject();
        json.put("list", keyList);
        json.put("count", keyList.length());
        return json;
    }

    public JSONObject getApiKey(APIRequest r) throws APIException {
        APIUser user = r.getUser();
        if (user.isDefault())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, APIError.SUBERR_FORBIDEN_DEFAULT_USER, "Default user cannot list api keys.");
        if (!user.canManageApikeys())
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_SOURCE_FORBIDDEN_ACTION, "Cannot list api keys.");

        Connection con = db.startTransaction();
        PreparedStatement stmt = db.prepare(con, "SELECT * FROM aqlvox_apikeys WHERE key_id = ?");
        JSONObject key = new JSONObject();
        try {
            stmt.setString(1, r.getArg("key-id").getAsString());
            ResultSet res = stmt.executeQuery();
            if (!res.next()) {
                db.endTransaction(con);
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_NO_SUCH_KEY, "No key with this id");
            }
            String ownerType = res.getString("user_type");
            String ownerID = res.getString("user_id");
            JSONObject owner = new JSONObject();
            owner.put("type", ownerType);
            owner.put("id", ownerID);
            if ((!user.getClass().getSimpleName().equals(ownerType) || !user.getUniqueID().equals(ownerID)) && !user.hasPerm(PERM_APIKEY_VIEW_ANY)) {
                db.endTransaction(con);
                throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDDEN_VIEW_APIKEY, "Not allowed to view this api key")
                        .addData("owner", owner);
            }
            key.put("id", res.getString("key_id"));
            key.put("name", res.getString("key_name"));
            key.put("key", res.getString("api_key"));
            String permsStr = res.getString("permissions");
            List<String> perms = permsStr!=null ? Arrays.asList(permsStr.split("; ")) : null;
            key.put("permissions", perms); // null means user's permissions
            key.put("owner", owner);
        } catch (SQLException e) {
            db.endTransaction(con, e);
            throw new APIError(APIError.APIErrorEnum.ERROR_INTERNAL_ERROR, APIError.SUBERR_INTERNAL_ERROR, "Error when listing api keys");
        }
        db.endTransaction(con);
        return key;
    }
}
