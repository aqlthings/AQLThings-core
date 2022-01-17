package fr.aquilon.minecraft.aquilonthings.utils;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.utils.JSONExportable;
import fr.aquilon.minecraft.utils.JSONUtils;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.util.Objects;

/**
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
public class Rank implements JSONExportable {
    private final String name;
    private final String prefix;
    private final String suffix;
    private final ChatColor color;

    public static Rank getRank(String name, ConfigurationSection rankConf) {
        Objects.requireNonNull(name);
        if (rankConf == null) return null;
        ChatColor color = null;
        String colorName = rankConf.getString("color", null);
        if (colorName != null) try {
            color = ChatColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            AquilonThings.LOGGER.info(AquilonThings.LOG_PREFIX+" Rank("+name+"): Invalid color");
        }
        return new Rank(
                name,
                rankConf.getString("prefix", null),
                rankConf.getString("suffix", null),
                color
        );
    }

    public Rank(String name, String prefix, String suffix, ChatColor color) {
        this.name = Objects.requireNonNull(name);
        this.prefix = prefix;
        this.suffix = suffix;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return getPrefix(null);
    }

    public String getPrefix(String def) {
        return prefix != null ? ChatColor.translateAlternateColorCodes('&', prefix) : def;
    }

    public String getSuffix() {
        return getSuffix(null);
    }

    public String getSuffix(String def) {
        return suffix != null ? ChatColor.translateAlternateColorCodes('&', suffix) : def;
    }

    public ChatColor getColor() {
        return color;
    }

    public String getColor(String def) {
        return color != null ? color.toString() : def;
    }

    public String decorateUsername(Player p) {
        return decorateUsername(p.getName());
    }

    public String decorateUsername(String name) {
        return decorateUsername(name, null, null, null);
    }

    public String decorateUsername(String name, String customPrefix, ChatColor customColor, String customSuffix) {
        return ChatColor.translateAlternateColorCodes('&',
                (prefix != null ? prefix : "") +
                        (customPrefix != null ? customPrefix : "") +
                        (color != null ? color.toString() : "") +
                        (customColor != null ? customColor.toString() : "") +
                        name +
                        (suffix != null ? suffix : "") +
                        (customSuffix != null ? customSuffix : "")
        );
    }

    @Override
    public JSONObject toJSON() {
        JSONObject res = new JSONObject();
        res.put("name", name);
        res.put("color", color != null ? JSONUtils.jsonColor(color.getChar()) : JSONObject.NULL);
        res.put("prefix", prefix != null ? JSONUtils.jsonColoredString(prefix) : JSONObject.NULL);
        res.put("suffix", suffix != null ? JSONUtils.jsonColoredString(suffix) : JSONObject.NULL);
        return res;
    }
}
