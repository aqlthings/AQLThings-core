package fr.aquilon.minecraft.utils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Billi on 20/04/2017.
 *
 * @author Billi
 */
public class JSONUtils {
    public static JSONObject jsonDate(Instant when) {
        return jsonDate(when.toEpochMilli());
    }

    public static JSONObject jsonDate(long ms) {
        JSONObject time = new JSONObject();
        Date date = new Date(ms);
        time.put("ms",ms);
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
        time.put("time",tf.format(date));
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        time.put("date",df.format(date));
        return time;
    }

    public static JSONArray jsonArray(Object[] arr) {
        List list = Arrays.asList(arr);
        return new JSONArray(list);
    }

    public static JSONArray jsonArray(Collection<? extends JSONExportable> arr) {
        return jsonArray(arr.toArray(new JSONExportable[0]));
    }

    public static JSONArray jsonArray(JSONExportable... arr) {
        JSONArray res = new JSONArray();
        for (JSONExportable o: arr) {
            res.put(o==null ? JSONObject.NULL : o.toJSON());
        }
        return res;
    }

    public static JSONArray jsonArray(String... arr) {
        return new JSONArray(arr);
    }

    public static JSONArray jsonToStringArray(Object... arr) {
        JSONArray res = new JSONArray();
        for (Object s: arr) {
            res.put(s == null ? JSONObject.NULL : s.toString());
        }
        return res;
    }

    public static JSONObject jsonObject(Map data) {
        JSONObject res = new JSONObject();
        for (Object key: data.keySet()) {
            Object value = data.get(key);
            res.put(key.toString(), value == null ? JSONObject.NULL : value);
        }
        return res;
    }

    public static JSONObject jsonColor(char mcColor) {
        JSONObject res = new JSONObject();
        res.put("name", MinecraftParser.colorName(mcColor));
        res.put("mc", "§"+mcColor);
        res.put("html", MinecraftParser.htmlColor(mcColor));
        res.put("unix", MinecraftParser.unixColor(mcColor));
        return res;
    }

    public static JSONObject jsonColoredString(String mcString) {
        JSONObject res = new JSONObject();
        res.put("text", ChatColor.stripColor(mcString));
        res.put("mc", mcString);
        res.put("html", MinecraftParser.parseHTML(mcString, true));
        res.put("unix", MinecraftParser.parseUnix(mcString));
        return res;
    }

    public static JSONObject jsonWorld(World w) {
        Objects.requireNonNull(w);
        JSONObject res = new JSONObject();
        res.put("name", w.getName());
        res.put("environement", w.getEnvironment().name());
        res.put("type", w.getWorldType() != null ? w.getWorldType().name() : JSONObject.NULL);
        return res;
    }

    public static JSONObject jsonLocation(Location loc) {
        return jsonLocation(loc, false);
    }

    public static JSONObject jsonLocation(Location loc, boolean rotation) {
        Objects.requireNonNull(loc);
        JSONObject res = jsonVector(loc.toVector());
        Object world = loc.getWorld() != null ? jsonWorld(loc.getWorld()) : JSONObject.NULL;
        res.put("world", world);
        if (rotation) {
            res.put("yaw", loc.getYaw());
            res.put("pitch", loc.getPitch());
        }
        return res;
    }

    public static JSONObject jsonVector(Vector vec) {
        JSONObject res = new JSONObject();
        res.put("x", vec.getX());
        res.put("y", vec.getY());
        res.put("z", vec.getZ());
        return res;
    }

    public static JSONObject jsonBlockEnvironment(Block b) {
        JSONObject res = new JSONObject();
        res.put("biome", b.getBiome().name());
        res.put("temperature", b.getTemperature());
        res.put("humidity", b.getHumidity());
        res.put("light", b.getLightLevel());
        res.put("skyLight", b.getLightFromSky());
        res.put("block", b.getType().name());
        res.put("liquid", b.isLiquid());
        return res;
    }
}

