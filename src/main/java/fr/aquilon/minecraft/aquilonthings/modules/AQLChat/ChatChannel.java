package fr.aquilon.minecraft.aquilonthings.modules.AQLChat;

import fr.aquilon.minecraft.utils.JSONExportable;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.bukkit.ChatColor;
import org.json.JSONObject;

/**
 * Created by Billi on 17/04/2017.
 *
 * @author Billi
 */
public class ChatChannel implements JSONExportable {
    public static final ChatColor DEFAULT_COLOR = ChatColor.WHITE;
    public static final String DEFAULT_FORMAT = "{color}[{nick}] {sender}{color}: {message}";
    public static final int DEFAULT_DISTANCE = 0;

    private final String name;
    private final String nick;
    private final int distance;
    private final ChatColor color;
    private final String format;

    public ChatChannel(String name, String nick, int distance) {
        this(name, nick, distance, DEFAULT_COLOR, DEFAULT_FORMAT);
    }

    public ChatChannel(String name, String nick, int distance, ChatColor color, String format) {
        this.name = name;
        this.nick = nick;
        this.distance = distance;
        this.color = color;
        this.format = format;
    }

    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("name",name);
        res.put("nick",nick);
        res.put("color", JSONUtils.jsonColor(color.toString().charAt(1)));
        if (distance!=0) res.put("distance", distance);
        res.put("global", distance==0);
        res.put("format",format);
        return res;
    }

    public String getName() {
        return name;
    }

    public String getGlobalPermission() {
        return AQLChat.PERM_CHAN_BASE.concat(getName().toLowerCase());
    }

    public String getJoinPermission() {
        return getGlobalPermission().concat(".join");
    }

    public String getReadPermission() {
        return getGlobalPermission().concat(".read");
    }

    public String getSpeakPermission() {
        return getGlobalPermission().concat(".speak");
    }

    public String getLeavePermission() {
        return getGlobalPermission().concat(".leave");
    }

    public String getFormatPermission() {
        return getGlobalPermission().concat(".format");
    }

    public String getBanPermission() {
        return getGlobalPermission().concat(".ban");
    }

    public String getNick() {
        return nick;
    }

    public int getDistance() {
        return distance;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getFormat() {
        return format;
    }

    public String computeMessage(String message, String pName, String pDispName,
                                            String pPrefix, String pSuffix, ChatColor pColor, boolean colored) {
        String res = getFormat();
        res = res.replace("{color}", getColor().toString())
                .replace("{nick}", getNick())
                .replace("{sender_color}", pColor != null ? pColor.toString() : "")
                .replace("{sender_prefix}", pPrefix != null ? pPrefix : "")
                .replace("{sender_suffix}", pSuffix != null ? pSuffix : "")
                .replace("{sender_rp}", pDispName)
                .replace("{sender_name}", pName)
                .replace("{sender}", AquilonChatEvent.getDecoratedSenderName(pName, pPrefix, pSuffix, pColor));
        res = ChatColor.translateAlternateColorCodes('&', res);
        return res.replace("{message}", colored ?
                ChatColor.translateAlternateColorCodes('&', message) : message);
    }
}
