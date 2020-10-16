package fr.aquilon.minecraft.aquilonthings.modules.AQLChat;

import fi.iki.elonen.NanoHTTPD;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIError;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.APIException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.exceptions.MissingPermissionEx;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIForumUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIModule;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIRequest;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIServer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIUser;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.APIWebSocket;
import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model.IWebsocket;
import fr.aquilon.minecraft.utils.InvalidArgumentEx;
import fr.aquilon.minecraft.utils.JSONUtils;
import fr.aquilon.minecraft.utils.MinecraftParser;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * APIModule to expose AQLChat data
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class ChatHttpAPI extends APIModule {
    public static final String MODULE_NAME = "chat";

    public static final String SUBERR_CHANNEL_NOT_FOUND = "101";
    public static final String SUBERR_MISSING_MESSAGE = "102";
    public static final String SUBERR_INVALID_COLOR = "103";
    public static final String SUBERR_FORBIDDEN_CHANNEL = "201";

    private final AQLChat chat;
    private IWebsocket ws;

    public ChatHttpAPI(APIServer server, ModuleLogger logger) {
        super(MODULE_NAME, server, logger);
        chat = AquilonThings.instance.getModuleData(AQLChat.class);
        if (chat == null) throw new IllegalStateException("AQLChat is not enabled");
    }

    @Override
    public void onReady() {
        registerRoute("getChatHistory", NanoHTTPD.Method.GET, "/history", this::getChatHistory);
        registerRoute("getChannelList", NanoHTTPD.Method.GET, "/channels", this::getChannelList);
        registerRoute("speakIntoChannel", NanoHTTPD.Method.POST, "/channels/{string:channel}/speak", this::speakIntoChannel);
        registerRoute("speakIntoChannelAsUser", NanoHTTPD.Method.POST, "/channels/{string:channel}/speak/{string:username}", this::speakIntoChannel);
        APIModule websocketModule = getServer().getModule("ws");
        if (websocketModule instanceof IWebsocket) {
            ws = (IWebsocket) websocketModule;
            ws.registerAction("chat.history", this::websocketGetChatHistory);
        } else ws = null;
    }

    public JSONObject getChannelList(APIRequest r) {
        APIUser usr = r.getUser();
        List<ChatChannel> chans = chat.getChannelList().stream()
                .filter(c -> usr.hasPerm("chat.channel."+c.getName().toLowerCase()+".read"))
                .collect(Collectors.toList());
        JSONObject res = new JSONObject();
        res.put("channels", JSONUtils.jsonArray(chans));
        return res;
    }

    public JSONObject getChatHistory(APIRequest r) {
        JSONObject res = new JSONObject();
        res.put("history", JSONUtils.jsonArray(chat.getChatHistory()));
        return res;
    }

    public JSONObject speakIntoChannel(APIRequest r) throws APIException {
        String channel = r.getArg("channel").getAsString().toLowerCase();
        String name = null;
        if (r.hasArg("username")) name = r.getArg("username").getAsString();
        if (name == null && r.getUser() instanceof APIForumUser) name = r.getUser().getName();

        ChatChannel chan = chat.getChannel(channel);
        if (chan==null)
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_FOUND, SUBERR_CHANNEL_NOT_FOUND, "No such chat channel.")
                    .addData("channel", channel);

        if (!r.getUser().hasPerm("chat.channel."+chan.getName().toLowerCase()+".speak"))
            throw new APIError(APIError.APIErrorEnum.ERROR_NOT_ALLOWED, SUBERR_FORBIDDEN_CHANNEL, "Missing permission for this channel.");

        JSONObject data = r.getJSONRequest();
        String message;
        try {
            message = data.getString("message");
        } catch (JSONException ex) {
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_MISSING_MESSAGE, "Missing message.");
        }

        ChatColor color = ChatColor.BLUE;
        if (data.has("color")) {
            String colorName;
            try {
                colorName = data.getString("color");
            } catch (JSONException ex) {
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_COLOR, "Invalid color parameter");
            }
            color = MinecraftParser.colorFromName(colorName);
            if (color==null)
                throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, SUBERR_INVALID_COLOR, "Invalid color code");
            String colorPerm = "chat.channel."+chan.getName().toLowerCase()+".speak.color."+color.name().toLowerCase();
            if (!r.getUser().hasPerm(colorPerm))
                throw new MissingPermissionEx(r.getUser(), colorPerm);
        }

        AquilonChatEvent msg;
        try {
            if (name!=null) {
                msg = new AquilonChatEvent(name, color, chan, message);
            } else {
                msg = new AquilonChatEvent(null, chan, message);
            }
        } catch (InvalidArgumentEx e) {
            String errMsg = "Invalid argument.";
            if (e.getCode()==AquilonChatEvent.EX_CODE_BAD_ANONYMOUS_CHANNEL) errMsg = "Invalid channel";
            if (e.getCode()==AquilonChatEvent.EX_CODE_MESSAGE_EMPTY) errMsg = "Message is empty";
            if (e.getCode()==AquilonChatEvent.EX_CODE_MESSAGE_TOO_LONG) errMsg = "Message is too long";
            if (e.getCode()==AquilonChatEvent.EX_CODE_NAME_TOO_LONG) errMsg = "Name is too long";
            throw new APIError(APIError.APIErrorEnum.ERROR_BAD_REQUEST, "11"+e.getCode(), errMsg);
        }
        msg.fillRecipients();
        msg.call(chat);
        return msg.toJSON();
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(AquilonChatEvent chatEvent) {
        if (ws == null) return;
        JSONObject res = chatEvent.toJSON();
        String suffix = (chatEvent.isConsole()?chatEvent.getSenderName():chatEvent.getSender().getUniqueId().toString().replace("-",""));
        ws.submitWSMessage("chat."+chatEvent.getChannel().getName().toLowerCase()+"."+suffix, res);
    }

    public void websocketGetChatHistory(APIWebSocket client, String action, String topic, JSONObject request) throws APIException {
        JSONObject msg = new JSONObject();
        APIUser usr = client.getUser();
        List<AquilonChatEvent> chatHistory = Arrays.stream(chat.getChatHistory())
                .filter(e -> usr.hasPerm("chat.channel."+e.getChannel().getName().toLowerCase()+".read"))
                .collect(Collectors.toList());
        msg.put("history", JSONUtils.jsonArray(chatHistory));
        msg.put("count", chatHistory.size());
        client.sendMessage("chat.history", msg);
    }
}
