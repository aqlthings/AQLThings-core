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
    public static final String DEFAULT_FORMAT = "{color}[{nick}] {prefix}{sender}{color}: {message}";
    public static final int DEFAULT_DISTANCE = 0;

    private String name;
    private String nick;
    private int distance;
    private ChatColor color;
    private String format;

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

    public String getJoinPermission() {
        return AQLChat.PERM_JOIN.concat(getName().toLowerCase());
    }

    public String getReadPermission() {
        return AQLChat.PERM_READ.concat(getName().toLowerCase());
    }

    public String getSpeakPermission() {
        return AQLChat.PERM_SPEAK.concat(getName().toLowerCase());
    }

    public String getLeavePermission() {
        return AQLChat.PERM_LEAVE.concat(getName().toLowerCase());
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
}
