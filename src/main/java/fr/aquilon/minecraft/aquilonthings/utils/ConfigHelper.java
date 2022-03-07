package fr.aquilon.minecraft.aquilonthings.utils;

import com.google.common.base.Charsets;
import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.module.ModuleLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

public class ConfigHelper {
    public static void saveFile(File outFile, InputStream stream) throws IOException {
        OutputStream out = new FileOutputStream(outFile);
        byte[] buf = new byte[1024];
        int len;
        while ((len = stream.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        stream.close();
    }

    public static YamlConfiguration loadConfig(File configFile, String defaultFileName, ModuleLogger logger) {
        if (!configFile.exists() && defaultFileName != null) {
            saveResource(configFile, defaultFileName, false, logger);
        }

        YamlConfiguration newConfig = YamlConfiguration.loadConfiguration(configFile);

        if (defaultFileName != null) {
            InputStream defConfRes = null;
            try {
                defConfRes = getResource(defaultFileName);
            } catch (IOException ignored) {}
            if (defConfRes == null) {
                logger.mWarning("Could not load default config file: "+defaultFileName);
            } else {
                InputStreamReader defReader = new InputStreamReader(defConfRes, Charsets.UTF_8);
                newConfig.setDefaults(YamlConfiguration.loadConfiguration(defReader));
            }
        }

        return newConfig;
    }

    public static void saveResource(File outFile, String resourcePath, boolean replace, ModuleLogger logger) {
        if (outFile == null)
            throw new IllegalArgumentException("Destination file cannot be null");
        if (resourcePath == null || resourcePath.isEmpty())
            throw new IllegalArgumentException("Resource path cannot be null or empty");

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = null;
        try {
            in = getResource(resourcePath);
        } catch (IOException ex) {
            logger.log(Level.WARNING, null, "Could not load JAR resource: " + resourcePath, ex);
        }
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found.");
        }

        File outDir = outFile.getParentFile();
        if (!outDir.exists()) {
            if (!outDir.mkdirs())
                logger.mWarning("Couldn't create parents directories");
        }

        try {
            if (!outFile.exists() || replace) {
                saveFile(outFile, in);
            } else {
                logger.mWarning("Could not save " + resourcePath + " to " + outFile + " because " +
                        outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, "Could not save " + resourcePath + " to " + outFile, ex);
        }
    }

    public static InputStream getResource(String filename) throws IOException {
        if (filename == null) {
            throw new IllegalArgumentException("Filename cannot be null");
        }

        URL url = AquilonThings.instance.getModuleClassLoader().getResource(filename);

        if (url == null) {
            return null;
        }

        URLConnection connection = url.openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }
}
