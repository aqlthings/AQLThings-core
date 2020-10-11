package fr.aquilon.minecraft.aquilonthings.modules.AQLVox.model;

import fr.aquilon.minecraft.aquilonthings.modules.AQLVox.AQLVox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Queue;

/**
 * Created by Billi on 18/04/2017.
 *
 * @author Billi
 */
public class APILogger {
    private final File folder;
    private final String name;
    private final Queue<File> archives;
    private final int limit;
    private File latestLog;
    private PrintStream out;
    private String day;
    private boolean enabled;

    public APILogger(File folder, String name, int limit) throws IOException {
        this.folder = folder;
        this.name = name;
        this.limit = limit;
        this.archives = new ArrayDeque<>();
        this.enabled = true;
        if (!folder.exists()) folder.mkdirs();
        processOldLogs();
        this.latestLog = new File(folder, name+"-latest.log");
        if (!latestLog.exists()) latestLog.createNewFile();
        this.out = new PrintStream(new FileOutputStream(latestLog, true), true, "utf-8");
        this.day = getDay();
    }

    private void processOldLogs() {
        List<File> oldLogs = Arrays.asList(
                folder.listFiles((dir, name) ->
                        name.startsWith(name+"-") && name.endsWith(".log") && !name.equals(name+"-latest.log")
                )
        );
        oldLogs.sort(Comparator.comparing(File::getName));
        archives.addAll(oldLogs);
        while (archives.size()>limit) {
            File toDelete = archives.poll();
            toDelete.delete();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void log(String ip, String route, int responseCode) {
        log(ip,route,responseCode,null, (String) null, null, null);
    }

    public void log(String ip, String route, int responseCode, APIUser user, List<String> comments,
                    String referer, String userAgent) {
        log(ip, route, responseCode, user, comments!=null?String.join("; ", comments):null, userAgent, referer);
    }

    public void log(String ip, String route, int responseCode, APIUser user, String comment,
                    String userAgent, String referer) {
        if (!day.equals(getDay())) rotateLogs();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        out.println(
                df.format(new Date())+" "+ // Date and time
                responseCode+" "+ // HTTP Status
                route+" \t"+ // URI
                ip+" "+ // APIRequest IP
                (user!=null?user.toString():"null")+ // Auth info
                (comment!=null && comment.length()>0?" ("+comment+")":"")+ // Comments if any
                (referer!=null?" \""+referer.replaceAll("\"","\\\"")+"\"":"")+ // Referer
                (userAgent!=null?" \""+userAgent.replaceAll("\"","\\\"")+"\"":"") // User Agent
        );
    }

    private void rotateLogs() {
        File archive = new File(folder, name+"-"+day+".log");
        latestLog.renameTo(archive);
        archives.add(archive);
        while (archives.size()>limit) {
            File toDelete = archives.poll();
            if (!toDelete.delete()) {
                AQLVox.LOGGER.mInfo("Couldn't delete old log file : "+toDelete.getName());
            }
        }
        latestLog = new File(folder, name+"-latest.log");
        try {
            this.out = new PrintStream(latestLog);
        } catch (FileNotFoundException e) { // Shouldn't happen
            e.printStackTrace();
        }
        this.day = getDay();
        out.println("=> Previous log archived into "+archive.getAbsolutePath());
    }

    private static String getDay() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(new Date());
    }
}
