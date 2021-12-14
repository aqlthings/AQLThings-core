package fr.aquilon.minecraft.aquilonthings.utils;

import com.google.common.base.Charsets;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import fr.aquilon.minecraft.aquilonthings.module.IModule;
import fr.aquilon.minecraft.aquilonthings.module.Module;
import fr.aquilon.minecraft.aquilonthings.module.ModuleLogger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * Various static minecraft-related utils
 * @author Billi
 */
public class Utils {
    public static UUID findUsernameUUID(String name) {
        String url = "https://api.mojang.com/users/profiles/minecraft/"+name;
        try {
            JSONObject json = httpGetJson(new URL(url));
            return UUID.fromString(Objects.requireNonNull(addUuidDashes(json.getString("id"))));
        } catch (Throwable e) {
            AquilonThings.LOGGER.log(Level.INFO, AquilonThings.LOG_PREFIX+" Unable to find UUID for \""+name+"\"", e);
            return null;
        }
    }

    /**
     * @param playerId The player UUID
     * @return The player rank
     */
    public static Rank getPlayerRank(UUID playerId) {
        LuckPerms perms = LuckPermsProvider.get();
        User permUser;
        try {
            permUser = perms.getUserManager().loadUser(playerId).get();
        } catch (InterruptedException | ExecutionException e) {
            AquilonThings.LOGGER.log(Level.INFO, AquilonThings.LOG_PREFIX+
                    " Unable to load user permissions: "+playerId, e);
            return null;
        }
        String rankName = permUser.getPrimaryGroup();
        FileConfiguration config = AquilonThings.instance.getConfig();
        return Rank.getRank(rankName, config.getConfigurationSection("players.ranks."+rankName));
    }

    /**
     * Decorate the player name with prefix, color and suffix based on its rank
     * @param p The player whose name must be decorated
     * @return The decorated player name
     */
    public static String decoratePlayerName(Player p) {
        return decoratePlayerName(p, null, null, null);
    }

    public static String decoratePlayerName(UUID id, String name) {
        return decoratePlayerName(id, name, null, null, null);
    }

    public static String decoratePlayerName(Player p, String customPrefix, ChatColor customColor, String customSuffix) {
        return decoratePlayerName(p.getUniqueId(), p.getName(), customPrefix, customColor, customSuffix);
    }

    public static String decoratePlayerName(UUID playerId, String playerName, String customPrefix,
                                            ChatColor customColor, String customSuffix) {
        Rank rank = getPlayerRank(playerId);
        if (rank == null) return playerName;
        return rank.decorateUsername(playerName, customPrefix, customColor, customSuffix);
    }

    public boolean checkArg(String[] args, int arg, String expected) {
        if (args.length <= arg) return false;
        return args[arg].equals(expected);
    }

    public static Thread threadFromID(long id) {
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.getId() == id) return t;
        }
        return null;
    }

    public static String addUuidDashes(String input) {
        if (input.length()==36 && input.contains("-")) return input;
        if (input.length()!=32) return null;
        StringBuilder sb = new StringBuilder(input);
        sb.insert(8, '-');
        sb.insert(13, '-');
        sb.insert(18, '-');
        sb.insert(23, '-');
        return sb.toString();
    }

    public static <T> T callOnMainThread(Callable<T> task) throws InterruptedException, ExecutionException {
        if (Bukkit.isPrimaryThread()) {
            try {
                return task.call();
            } catch (Exception e) {
                throw new ExecutionException(e);
            }
        }
        return Bukkit.getScheduler().callSyncMethod(AquilonThings.instance, task).get();
    }

    /**
     * Converts a short UUID string to an UUID object when possible
     * @param uuidString The uuid as a string (possibly without the dashes)
     * @return The UUID object is possible, null otherwise
     */
    public static UUID getUUID(String uuidString) {
        String str = addUuidDashes(uuidString);
        if (str == null) return null;
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean playerHasWarningPerm(CommandSender p, Class<? extends IModule> klass) {
        Objects.requireNonNull(klass);
        Module m = AquilonThings.instance.getModule(klass);
        return p.hasPermission(AquilonThings.PERM_WARNING+m.getName().toLowerCase());
    }

    public static void warnStaff(Class<? extends IModule> klass, String message){
        warnStaff(klass, message, new String[0]);
    }

    public static void warnStaff(Class<? extends IModule> klass, String message, String[] except) {
        Module m = klass != null ? AquilonThings.instance.getModule(klass) : null;
        if (m == null) AquilonThings.LOGGER.log(Level.INFO, AquilonThings.LOG_PREFIX+" "+message);
        else ModuleLogger.logStatic(m.data().getClass(), Level.INFO, null, message, null);
        List<String> excepts = Arrays.asList(except);
        String prefix = ChatColor.GRAY + (m != null ? "["+m.getName()+"] " : "[AquilonThings] ");
        for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            if (excepts.contains(p.getName())) continue;
            if (m != null && !playerHasWarningPerm(p, klass)) continue;
            if (m == null && !p.hasPermission(AquilonThings.PERM_WARNING+"global")) continue;
            p.sendMessage(prefix+ChatColor.WHITE+message);
        }
    }

    public static FileConfiguration loadConfig(String configFile) {
        FileConfiguration newConfig = YamlConfiguration.loadConfiguration(new File(AquilonThings.instance.getDataFolder(), configFile));
        //if (newConfig.getKeys(false).size()<1) return null;

        final InputStream defConfigStream = getResource(configFile);
        if (defConfigStream == null) return null;

        newConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, Charsets.UTF_8)));
        return newConfig;
    }

    public static void saveResource(String resourcePath, boolean replace, ModuleLogger logger) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found.");
        }

        File outFile = new File(AquilonThings.instance.getDataFolder(), resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(AquilonThings.instance.getDataFolder(), resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } else {
                logger.mWarning("Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

    public static InputStream getResource(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        try {
            URL url = AquilonThings.class.getClassLoader().getResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    public static boolean isSameLocation(Location pos1, Location pos2) {
        return pos1.getWorld()!=pos2.getWorld() || pos1.getX()!=pos2.getX() || pos1.getY()!=pos2.getY() || pos1.getZ()!=pos2.getZ();
    }

    public static String joinStrings (Object[] arr, String separator) {
        return joinStrings(arr,separator,0);
    }

    public static String joinStrings (Object[] parts, String separator, int beginIndex) {
        if (parts.length==0) return "";
        if (beginIndex>=parts.length) return "";
        StringBuilder res = new StringBuilder();
        List<Object> arr = Arrays.asList(parts).subList(beginIndex, parts.length);
        for (Object s: arr) {
            res.append(s.toString()).append(separator);
        }
        return res.substring(0, res.length()-separator.length());
    }

    public static Map<String, String> readArguments(String[] args, int startIndex) {
        if (args.length <= startIndex) return Collections.emptyMap();
        List<String> list = Arrays.asList(args);
        list = list.subList(startIndex, list.size());
        Iterator<String> tokens = list.iterator();
        Map<String, String> res = new HashMap<>();
        while (tokens.hasNext()) {
            String token = tokens.next();
            if (token.matches("^[a-zA-Z]+:.+")) {
                int sep = token.indexOf(':');
                String key = token.substring(0, sep);
                res.put(key, readStringArgument(token.substring(sep+1), tokens));
            } else {
                String remaining = res.get(null);
                if (remaining == null) remaining = token;
                else remaining += " "+token;
                res.put(null, remaining);
            }
        }
        return res;
    }

    public static String readStringArgument(String start, Iterator<String> tokens) {
        if (!start.startsWith("\"")) return start;
        if (start.endsWith("\"")) return start.substring(1, start.length()-1);
        StringBuilder res = new StringBuilder(start.substring(1));
        while (tokens.hasNext()) {
            String t = tokens.next();
            res.append(' ');
            if (t.endsWith("\"")) {
                res.append(t, 0, t.length() - 1);
                break;
            }
            res.append(t);
        }
        return res.toString();
    }

    /**
     * Decorates every substring of a string with a prefix and a suffix
     * @param source The string to look into
     * @param search The substring to look for
     * @param prefix What to put before each substring
     * @param suffix What to put after each substring
     * @return The string decorated
     */
    public static String decorateSubstringsIgnoreCase(String source, String search, String prefix, String suffix) {
        StringBuilder res = new StringBuilder();
        String sourceLo = source.toLowerCase();
        String searchLo = search.toLowerCase();
        int index = sourceLo.indexOf(searchLo);
        int endIndex = 0;
        while (index!=-1) {
            int prevEndIndex = endIndex;
            endIndex = index+search.length();
            String sub = source.substring(index, endIndex);
            res.append(index > 0 ? source.substring(prevEndIndex, index) : "").append(prefix).append(sub).append(suffix);
            index = sourceLo.indexOf(searchLo, endIndex);
        }
        res.append(source.substring(endIndex));
        return res.toString();
    }

    public static JSONObject httpGetJson(URL url) throws IOException {
        // Open connection
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        if (con.getResponseCode()/100!=2) {
            throw new IOException("Invalid HTTP status code: "+con.getResponseCode()+" (url: "+url.toString()+")");
        }
        // Get response
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        // Parse JSON
        return new JSONObject(response.toString());
    }

    public static final char[] RANDOM_STRING_CHARSET = {'0','1','2','3','4','5','6','7','8','9',
            'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
            'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z'
    };

    public static String randomString(int length) {
        return randomString(length, RANDOM_STRING_CHARSET);
    }

    public static String randomString(int length, char[] charset) {
        Random alea = new Random();
        StringBuilder str = new StringBuilder(length);
        for (int i =0; i<length; i++) {
            str.append(charset[alea.nextInt(charset.length)]);
        }
        return str.toString();
    }

    public static int[] blockToPositionArray(Block b) {
        int[] res = new int[3];
        res[0] = b.getX();
        res[1] = b.getY();
        res[2] = b.getZ();
        return res;
    }

    public static <T> T[] mergeArrays(T[] a,T[] b) {
        @SuppressWarnings("unchecked")
        T[] res = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length+b.length);
        System.arraycopy(a, 0, res, 0, a.length);
        System.arraycopy(b, 0, res, a.length, b.length);
        return res;
    }
}
