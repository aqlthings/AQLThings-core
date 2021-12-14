package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;
import fr.aquilon.minecraft.aquilonthings.module.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.AQLVox;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.MissingPermissionEx;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * A single websocket client
 * @author Billi
 */
public class APIWebSocket extends NanoWSD.WebSocket {
    private final APIUser user;
    private final String clientIP;
    private final ModuleLogger logger;
    private final IWebsocket module;
    private final Set<String> topics;
    private final Object topicsLock = new Object();

    public APIWebSocket(IWebsocket module, ModuleLogger logger, NanoHTTPD.IHTTPSession handshakeRequest, APIUser user) {
        super(handshakeRequest);
        this.clientIP = handshakeRequest.getRemoteIpAddress();
        this.logger = logger;
        this.module = module;
        this.topics = new HashSet<>();
        this.user = user;
    }

    @Override
    protected void onOpen() {
        module.registerWSClient(this);
        JSONObject welcome = new JSONObject();
        welcome.put("welcome", "AQLVox module for AquilonThings (v"+AQLVox.VERSION+")");
        welcome.put("version", AQLVox.VERSION);
        welcome.put("user", user.getName());
        sendMessage("websocket.info", welcome);
    }

    @Override
    protected void onClose(NanoWSD.WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
        module.unregisterWSClient(this);
    }

    @Override
    protected void onMessage(NanoWSD.WebSocketFrame message) {
        String payload = message.getTextPayload();
        if (payload.equals("PING")) return;
        JSONObject data;
        try {
            data = new JSONObject(payload);
        } catch (JSONException e) {
            sendMessage("websocket.error", new APIException(NanoHTTPD.Response.Status.BAD_REQUEST, "Invalid payload").toJSON());
            return;
        }
        String topic;
        try {
            topic = data.getString("topic");
        } catch (JSONException e) { // No topic provided
            topic = "websocket."+user.getName();
        }
        if (data.has("action")) try {
            if (data.get("action").equals("subscribe") && data.has("topics")) {
                for (Object o : data.getJSONArray("topics")) {
                    String t = (String) o;
                    String perm = t.replaceAll("\\*", "#.#")
                            .replaceAll("#", "__WILDCARD__");
                    try {
                        check("subscribe."+perm);
                    } catch (APIException e) {
                        sendMessage("websocket.error", e.toJSON());
                        return;
                    }
                    synchronized (topicsLock) {
                        topics.add(t);
                    }
                }
            } else if (data.get("action").equals("unsubscribe") && data.get("topics")!=null) {
                for (Object o : data.getJSONArray("topics")) {
                    String t = (String) o;
                    synchronized (topicsLock) {
                        topics.remove(t);
                    }
                }
            } else if (data.get("action").equals("info")) {
                JSONObject info = new JSONObject();
                JSONArray subs;
                synchronized (topicsLock) {
                     subs = new JSONArray(topics);
                }
                info.put("topics", subs);
                info.put("name", user.getName());
                info.put("perms", JSONUtils.jsonArray(user.getPermList().toArray()));
                sendMessage(topic, info);
            } else {
                String action = data.get("action").toString();
                try {
                    check("action."+action);
                    module.dispatchAction(this, action, topic, data);
                } catch (APIException e) {
                    sendMessage("websocket.error", e.toJSON());
                }
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "[WebSocket]", "Action error", e);
            sendMessage("websocket.error", new JSONObject("{\"message\":\"An unexpected exception occured\"}"));
        }
    }

    public String getClientIP() {
        return clientIP;
    }

    public APIUser getUser() {
        return user;
    }

    public void check(String topic) throws APIException {
        String perm = "ws."+topic;
        if (!user.hasPerm(perm)) {
            throw new MissingPermissionEx(user, perm); // Auth ok: Missing perm
        }
    }

    public boolean hasTopic(String topic) {
        if (topic.startsWith("websocket.")) return true;
        String[] hierarchy = APIUser.getPermHierarchy(topic);
        for (String pHierarchy: hierarchy) {
            synchronized (topicsLock) {
                for (String t : topics) {
                    if (t.equals("all")) return true;
                    if (pHierarchy.matches(APIUser.getPermRegex(t))) return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onPong(NanoWSD.WebSocketFrame pong) {}

    @Override
    protected void onException(IOException ex) {
        try {
            if (ex instanceof SocketTimeoutException) {
                logger.log(Level.INFO, "[WebSocket]", "Debug: Socket timeout (no ping received)", (Throwable) null);
                this.close(NanoWSD.WebSocketFrame.CloseCode.GoingAway, "No ping received, timeout.", true);
            } else if (ex instanceof SocketException) {
                this.close(NanoWSD.WebSocketFrame.CloseCode.NormalClosure, "Socket closed ("+ex.getMessage()+")", false);
            } else if (ex instanceof EOFException) {
                logger.log(Level.FINE, "[WebSocket]", "Socket closed abnormally (EOF)", (Throwable) null);
            } else throw ex;
        } catch (Exception e) {
            logger.log(Level.WARNING, "[WebSocket]", "ERROR: ", e);
        }
    }

    public boolean sendMessage(String topic, JSONObject data) {
        try {
            JSONObject res = new JSONObject();
            res.put("topic", topic);
            res.put("data", data);
            send(res.toString());
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
