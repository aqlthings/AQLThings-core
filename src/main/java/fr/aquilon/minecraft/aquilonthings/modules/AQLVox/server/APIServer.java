package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.AQLVox;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.InternalServerErrorEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.InvalidAuthEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.ModuleNotFoundEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.NotFoundEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.logging.APILogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.modules.About;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.modules.Auth;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.modules.Players;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.modules.Server;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.modules.Websocket;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.routing.APIRoute;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.routing.RouteHandler;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users.APIForumUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users.APIStaticUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.users.APIUser;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created by Billi on 17/04/2017.
 * @author Billi
 */
public class APIServer extends NanoWSD {
    public static final Class[] DEFAULT_MODULE_LIST = {
            About.class,
            Players.class,
            Server.class,
            Auth.class,
            Websocket.class
    };

    private final int port;
    private final HashMap<String, APIModule> modules;
    private final AQLVox data;
    private final APILogger apiLogger;
    private final JWTVerifier tokenVerifier;

    public APIServer(int port, AQLVox data) throws UnsupportedEncodingException {
        super(port);
        this.port = port;
        this.modules = new HashMap<>();
        this.data = data;
        this.apiLogger = data.getApiLogger();
        String tokenSecret = data.getConfig("auth.token.secret");
        if (tokenSecret == null || tokenSecret.isEmpty())
            throw new IllegalArgumentException("Missing JWT secret (config: auth.token.secret)");
        tokenVerifier = JWT.require(Algorithm.HMAC256(tokenSecret))
                .withIssuer(Auth.TOKEN_ISSUER)
                .build();
    }

    private void loadModules() {
        int routeCount = 0;
        modules.clear();
        FileConfiguration conf = data.getConfig();
        List<Class> moduleClasses;
        if (conf.get("modules.enable") != null) {
            moduleClasses = new ArrayList<>();
            List<String> confModules = conf.getStringList("modules.enable");
            ClassLoader loader = getClass().getClassLoader();
            for (String confModule : confModules) {
                try {
                    moduleClasses.add(loader.loadClass(confModule));
                } catch (ClassNotFoundException e) {
                    AQLVox.LOGGER.mWarning("No API module class found matching: "+confModule);
                }
            }
        } else {
            moduleClasses = new ArrayList<>(Arrays.asList(DEFAULT_MODULE_LIST));
        }
        for (Class mClass : moduleClasses) {
            if (!APIModule.class.isAssignableFrom(mClass)) {
                AQLVox.LOGGER.mWarning("Trying to load invalid API module class: "+mClass.getName());
                continue;
            }
            APIModule m;
            try {
                try {
                    Constructor<APIModule> construct = mClass.getConstructor(APIServer.class, ModuleLogger.class);
                    m = construct.newInstance(this, AQLVox.LOGGER);
                } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    AQLVox.LOGGER.mWarning("Unable to register API module: " + mClass.getSimpleName());
                    AQLVox.LOGGER.log(Level.INFO, null, "API module error:", e);
                    continue;
                }
                modules.put(m.getName().toLowerCase(), m);
                AquilonThings.instance.getModule(AQLVox.class).registerEventListener(m);
                m.onReady();
            } catch (Throwable err) {
                AQLVox.LOGGER.mWarning("Error while registering API module: " + mClass.getSimpleName());
                AQLVox.LOGGER.log(Level.INFO, null, "Exception:", err);
                continue;
            }
            routeCount += m.getRoutes().size();
            AQLVox.LOGGER.mDebug("Registered module: "+m.getName()+" ("+m.getClass().getName()+") with "+m.getRoutes().size()+" routes");
        }
        AQLVox.LOGGER.mInfo("Registered "+modules.size()+" API modules ("+routeCount+" routes).");
    }

    public List<APIModule> getModules() {
        return new ArrayList<>(modules.values());
    }

    public APIModule getModule(String name) {
        return modules.get(name);
    }

    public boolean isWebsocketRequest(IHTTPSession req) {
        return this.isWebsocketRequested(req);
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) { // Useless but mandatory
        return null;
    }

    @Override
    public Response serve(IHTTPSession session) {
        long startTime = System.currentTimeMillis();
        Thread.currentThread().setName("AQLVox #"+Thread.currentThread().getId());
        Response res;
        String remoteIP = session.getRemoteIpAddress();
        List<String> comments = new ArrayList<>();
        APIUser user = null;
        try {
            APIRequest req = APIRequest.fromSession(this, session); // Might throw an error if invalid request
            if (session.getHeaders().containsKey("x-forwarded-for")) {
                String[] ipRoute = session.getHeaders().get("x-forwarded-for").split(", ");
                comments.add("via: "+ Utils.joinStrings(ipRoute, ", ")+", "+remoteIP);
                remoteIP = ipRoute[ipRoute.length-1]; // Last address before aquilon apache proxy.
            }
            user = getUserFromRequest(session, comments); // Might throw an exception if invalid auth
            if (!user.isDefault() && user.getSource()==null)
                AQLVox.LOGGER.mWarning("User source not defined ! "+String.join("; ",comments));
            if (session.getMethod().equals(Method.OPTIONS)) {
                res = newFixedLengthResponse(Response.Status.OK, "text/plain", "OK");
                res.addHeader("Allow", "GET, POST, PUT, DELETE");
                res.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
                res.addHeader("Access-Control-Allow-Headers", "Authorization, X-Api-Key, X-Api-User, X-Api-Set-Groups, Content-Type");
            } else if (req==null) {
                res = (new NotFoundEx(session.getUri(), "No such route.")).getResponse();
            } else {
                req.setUser(user);
                if (req.isRoot()) {
                    String msg = "<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\"/>" +
                            "<title>Aquilon - API</title></head>\n" +
                            "<body style=\"font-family: Calibri, sans-serif;\"><h1>Aquilon - API</h1>\n" +
                            "<p>Bienvenue sur l'API du serveur Minecraft Roleplay <b>" +
                            "<a href=\"https://www.aquilon-mc.fr/\">Aquilon</a></b>.<br/>" +
                            "Vous trouverez la documentation ici : <a href=\"about/doc\">Documentation</a>.</p>\n" +
                            "<p style=\"text-align: right;\"><i><b>AQLVox</b> (v" + AQLVox.VERSION + ") pour " +
                            "AquilonThings (v" + AquilonThings.VERSION + ")</i></p>" +
                            "</body></html>";
                    res = RouteHandler.newHTMLResponse(Response.Status.OK, msg);
                } else {
                    try {
                        if (!modules.containsKey(req.getModule())) throw new ModuleNotFoundEx(req.getModule());
                        user.check(req);
                        APIModule module = modules.get(req.getModule());
                        res = module.serve(req);
                    } catch (APIException ex) {
                        throw ex; // Bubble up
                    } catch (Exception ex) {
                        AQLVox.LOGGER.log(Level.WARNING, null, "Uncaught exception: ", ex);
                        res = (new InternalServerErrorEx("Uncaught exception ("+ex.getClass().getSimpleName()+")")).getResponse();
                    }
                }
            }
        } catch (APIError ex) {
            res = ex.getResponse();
            comments.add("error: "+ex.getCode());
        } catch (APIException ex) {
            res = ex.getResponse();
        }
        res.addHeader("Access-Control-Allow-Origin", session.getHeaders().getOrDefault("origin","*"));
        res.addHeader("Access-Control-Allow-Credentials", "true");
        comments.add("time: "+(System.currentTimeMillis()-startTime)+"ms");
        apiLogger.log(remoteIP, APIRoute.getString(session), res.getStatus().getRequestStatus(), user, comments,
                session.getHeaders().get("referer"), session.getHeaders().get("user-agent"));
        return res;
    }

    public static Response createWebSocket(IHTTPSession request, IWebsocket m, APIUser user) {
        Map<String, String> headers = request.getHeaders();
        if (!NanoWSD.HEADER_WEBSOCKET_VERSION_VALUE.equalsIgnoreCase(headers.get(NanoWSD.HEADER_WEBSOCKET_VERSION))) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
                    "Invalid Websocket-Version " + headers.get(NanoWSD.HEADER_WEBSOCKET_VERSION));
        }

        if (!headers.containsKey(NanoWSD.HEADER_WEBSOCKET_KEY)) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT, "Missing Websocket-Key");
        }

        NanoWSD.WebSocket webSocket = new APIWebSocket(m, AQLVox.LOGGER, request, user);
        Response handshakeResponse = webSocket.getHandshakeResponse();
        try {
            handshakeResponse.addHeader(NanoWSD.HEADER_WEBSOCKET_ACCEPT, NanoWSD.makeAcceptKey(headers.get(NanoWSD.HEADER_WEBSOCKET_KEY)));
        } catch (NoSuchAlgorithmException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
                    "The SHA-1 Algorithm required for websockets is not available on the server.");
        }

        if (headers.containsKey(NanoWSD.HEADER_WEBSOCKET_PROTOCOL)) {
            handshakeResponse.addHeader(NanoWSD.HEADER_WEBSOCKET_PROTOCOL, headers.get(NanoWSD.HEADER_WEBSOCKET_PROTOCOL).split(",")[0]);
        }

        return handshakeResponse;
    }

    public APIUser getUserFromRequest(IHTTPSession req, List<String> comments) throws APIError {
        String rawAuth = req.getHeaders().getOrDefault("authorization",null);
        if (rawAuth!=null) {
            String[] auth = rawAuth.split(" ", 2);
            if (auth.length<2)
                throw new APIError(APIError.APIErrorEnum.ERROR_INVALID_AUTH, APIError.SUBERR_INVALID_AUTH_MODE, "Malformed Authorization header.")
                        .addData("code", 1);
            if (auth[0].equals("Basic")) {
                /* - PASSWORD - */
                // TODO: b64 decode, username should contains a type prefix, otherwise check static users
                comments.add("authType: Password");
                //AQLVox.LOGGER.mDebug("Authenticated user '"+username+"' from password");
            } else if (auth[0].equals("Bearer") && auth[1].contains(".")) {
                /* - TOKEN - */
                DecodedJWT token;
                try {
                    token = tokenVerifier.verify(auth[1]);
                } catch (JWTVerificationException e) {
                    AQLVox.LOGGER.mInfo("Invalid auth token: "+e.getClass().getSimpleName()+" - "+e.getMessage());
                    throw new APIError(APIError.APIErrorEnum.ERROR_INVALID_AUTH, APIError.SUBERR_INVALID_TOKEN, "Invalid auth token")
                            .addData("info", e.getMessage())
                            .addData("error", e.getClass().getSimpleName());
                }
                String userType = token.getClaim("usr-type").asString();
                String userID = token.getClaim("usr-id").asString();
                String tokenID = token.getClaim("tok-id").asString();
                comments.add("authType: Token");
                comments.add("tokenID: "+tokenID);
                //AQLVox.LOGGER.mDebug("Authenticated token '"+tokenID+"'");
                APIUser usr = AQLVox.instance.getUser(userType, userID);
                if (usr==null)
                    throw new APIError(
                            APIError.APIErrorEnum.ERROR_INTERNAL_ERROR,
                            APIError.SUBERR_INTERNAL_ERROR,
                            "Error looking up user")
                            .addData("userType", userType)
                            .addData("userId", userID)
                            .addData("tokenId", tokenID);
                usr.setSource(APIUser.Source.TOKEN);
                return usr;
            } else if (auth[0].equals("Bearer")) {
                /* - API KEY - */
                DatabaseConnector db = AquilonThings.instance.getNewDatabaseConnector();
                Connection con = db.startTransaction();
                PreparedStatement stmt = db.prepare(con, "SELECT * FROM aqlvox_apikeys WHERE api_key = ?");
                try {
                    stmt.setString(1, auth[1]);
                    ResultSet res = stmt.executeQuery();
                    if (!res.next()) {
                        throw new APIError(APIError.APIErrorEnum.ERROR_INVALID_AUTH, APIError.SUBERR_INVALID_APIKEY, "Invalid api key");
                    }
                    String userType = res.getString("user_type");
                    String userID = res.getString("user_id");
                    String apiKeyID = res.getString("key_id");
                    String permsStr = res.getString("permissions");
                    List<String> perms = permsStr!=null ? Arrays.asList(permsStr.split("; ")) : null;
                    db.endTransaction(con);
                    comments.add("authType: ApiKey");
                    comments.add("apikeyID: "+apiKeyID);
                    //AQLVox.LOGGER.mDebug("Authenticated api key '"+apiKeyID+"'");
                    APIUser usr = data.getUser(userType, userID);
                    if (usr==null) { // TODO: remove apikey from db
                        throw new APIError(
                                APIError.APIErrorEnum.ERROR_INVALID_AUTH,
                                APIError.SUBERR_INVALID_APIKEY,
                                "Expired api key")
                                .addData("info", "user no longer exists")
                                .addData("code", 1);
                    }
                    usr.setSource(APIUser.Source.API_KEY);
                    if (perms!=null) usr.getPermissions().clearAllPerms().addAllPerms(perms);
                    return usr;
                } catch (SQLException e) {
                    db.endTransaction(con, e);
                    throw new APIError(
                            APIError.APIErrorEnum.ERROR_INTERNAL_ERROR,
                            APIError.SUBERR_INTERNAL_ERROR,
                            "Error while looking up api key");
                }
            } else {
                // TODO: Add 'AQLVox' auth scheme
                // Fields are separated by ';'
                // Allowed fields:
                //   - method (default: 'password')
                //   - usertype (required except for token/apikey)
                //   - userid (required except for token/apikey)
                //   - value (optional, required for token/apikey)

                throw new APIError(
                        APIError.APIErrorEnum.ERROR_INVALID_AUTH,
                        APIError.SUBERR_INVALID_AUTH_MODE,
                        "Unsupported Authorization type.");
            }
        }

        /* - STATIC USER KEY - */ // Should we keep this auth method ? Send as basic auth ?
        String userName = req.getHeaders().getOrDefault("x-api-user",null);
        if (userName==null) {
            List<String> userQueryParam = req.getParameters().getOrDefault("user", null);
            if (userQueryParam!=null && userQueryParam.size()>0) userName = userQueryParam.get(0);
        }
        if (userName!=null) {
            comments.add("authType: SigningKey");
            APIStaticUser usr = data.getStaticUser(userName);
            if (usr==null) throw new APIError(
                    APIError.APIErrorEnum.ERROR_INVALID_AUTH,
                    APIError.SUBERR_INVALID_STATIC_USER,
                    "Invalid user");
            return usr.setSource(APIUser.Source.SIGNING_KEY);
        }

        /* - FORUM USER COOKIE - */
        String forumUID = req.getCookies().read(data.getConfig("auth.forum.cookie.user"));
        if (forumUID!=null) {
            int forumUserID;
            try {
                forumUserID = Integer.parseInt(forumUID);
            } catch (NumberFormatException e) {
                return data.getDefaultUser();
            }
            if (forumUserID == 1) return data.getDefaultUser();
            String forumSession = req.getCookies().read(data.getConfig("auth.forum.cookie.session"));
            if (forumSession==null)
                throw new APIError(APIError.APIErrorEnum.ERROR_INVALID_AUTH, APIError.SUBERR_INVALID_AUTH, "Missing session cookie")
                    .addData("uid", forumUID);
            APIUser res;
            String customGroups = req.getHeaders().getOrDefault("x-api-set-groups",null);
            res = APIForumUser.fromSession(forumUserID, forumSession, customGroups);
            if (res==null) throw new InvalidAuthEx().addData("info", "Error when authenticating user "+forumUID).addData("uid", forumUID);
            res.setSource(APIUser.Source.FORUM_COOKIES);
            comments.add("authType: ForumCookies");
            //AQLVox.LOGGER.mDebug("Authenticated forum user ("+forumUID+") : "+res.getName());
            return res;
        }
        return data.getDefaultUser();
    }

    public void start() {
        loadModules();
        try {
            AQLVox.LOGGER.mInfo("Démarrage de l'API ...");
            super.start(15000, false);
            AQLVox.LOGGER.mInfo("API ouverte sur le port "+this.getListeningPort());
        } catch (IOException ex) {
            AQLVox.LOGGER.mSevere("Démarrage de l'API impossible, port "+port+" utilisé.");
            AQLVox.LOGGER.log(Level.FINE, null, "Exception: ",ex);
        }
    }

    public void stop() {
        AQLVox.LOGGER.mInfo("Arret de l'API.");
        for (APIModule m : modules.values()) {
            if (!IWebsocket.class.isAssignableFrom(m.getClass())) continue;
            IWebsocket ws = (IWebsocket) m;
            for (APIWebSocket client : ws.getWsClients()) {
                try {
                    client.close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "Server shutting down.", false);
                } catch (IOException ignored) {}
            }
        }
        super.stop();
    }

    public void restart() {
        stop();
        start();
    }
}
