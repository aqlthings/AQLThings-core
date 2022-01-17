package fr.aquilon.minecraft.aquilonthings.modules;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.annotation.InPacket;
import fr.aquilon.minecraft.aquilonthings.annotation.OutPacket;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
@AQLThingsModule(
		name = "AQLRegionBorder",
		cmds = @Cmd(value = AQLRegionBorder.COMMAND, desc = "Recharge les bordures de carte"),
		inPackets = @InPacket(AquilonThings.CHANNEL_READY),
		outPackets = @OutPacket(AQLRegionBorder.CHANNEL_REGION_BORDER)
)
public class AQLRegionBorder implements IModule {
	public static final ModuleLogger LOGGER = ModuleLogger.get();
	public static final String COMMAND = "reloadborders";
	public static final String CHANNEL_REGION_BORDER = "mapborder";
	public static final String PERM_REGIONBORDER_RELOAD = AquilonThings.PERM_ROOT+".regionborder.reload";
	public static final File WORLDGUARD_FOLDER = new File(AquilonThings.instance.getDataFolder(),"../WorldGuard/worlds/");

	private FileConfiguration config;
	private Map<String, List<Point2D>> borders;

	public AQLRegionBorder() {
		this.config = AquilonThings.instance.getConfig();
		borders = new HashMap<>();
	}
	
	@Override
	public boolean onStartUp(DatabaseConnector db) {
		return loadRegionBorders();
	}

	@Override
	public boolean onStop() {
		return true;
	}

	private boolean loadRegionBorders() {
		ConfigurationSection borderWorlds = config.getConfigurationSection("regionBorders");
		if (borderWorlds==null) {
			LOGGER.mWarning("Couldn't find list of worlds (config key: regionBorders), shutting down.");
			return false;
		}

		borders.clear();
		for (String w : borderWorlds.getKeys(false)) {
			String regionName = borderWorlds.getString(w);
			if (regionName==null) {
				LOGGER.mWarning("Region name not defined for map '"+w+"'");
				continue;
			}
			List<Point2D> points = new ArrayList<>();
			borders.put(w, points);
			File mapRegionsFile = new File(WORLDGUARD_FOLDER, w+"/regions.yml");
			if (!mapRegionsFile.exists()) {
				LOGGER.mWarning("No worldguard region config found for map '"+w+"'");
				continue;
			}
			FileConfiguration regionsData = YamlConfiguration.loadConfiguration(mapRegionsFile);
			List<?> regionPoints = regionsData.getList("regions."+regionName+".points");
			if (regionPoints==null) {
				LOGGER.mWarning("Worldguard region undefined. Map: '"+w+"', region: '"+regionName+"'");
				continue;
			}
			for (Object p : regionPoints) {
				Map point = (Map) p;
				points.add(new Point2D((int) point.get("x"), (int) point.get("z")));
			}
			LOGGER.mInfo("Loaded region border for map '"+w+"'");
		}
		LOGGER.mInfo("Found "+borders.size()+" region borders.");
		return true;
	}

	@Override
	public void onPluginMessageReceived(String arg0, Player arg1, byte[] arg2) {
		// When a player joins
		senderPlayerBorder(arg1);
	}

	@EventHandler
	public void onChangeWorld(PlayerChangedWorldEvent event) {
		senderPlayerBorder(event.getPlayer());
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg1, String[] args) {
		if (arg1.equalsIgnoreCase("reloadborders")) {
			if (!sender.hasPermission(PERM_REGIONBORDER_RELOAD)) {
				sender.sendMessage(ChatColor.YELLOW+"Touche pas à ça !");
				return true;
			}

			if (loadRegionBorders()) {
				for (Player p : Bukkit.getOnlinePlayers())
					senderPlayerBorder(p);
				sender.sendMessage(ChatColor.YELLOW+"Bordures de map rechargées !");
			} else {
				sender.sendMessage(ChatColor.RED+"Impossible de charger les bordures !");
			}
			return true;
		}
		return false;
	}

	private void senderPlayerBorder(Player p) {
		String map = p.getWorld().getName();
		List<Point2D> border = borders.get(map);
		ByteBuffer buf;
		if (border==null) {
			buf = ByteBuffer.allocate(2);
			buf.putChar('0');
		} else {
			int size = border.size()*2*4+4+2;
			buf = ByteBuffer.allocate(size);
			buf.putChar('1');
			buf.putInt(border.size());
			for (Point2D point : border) {
				buf.putInt(point.x).putInt(point.z);
			}
		}

		AquilonThings.sendPluginMessage(p, CHANNEL_REGION_BORDER, buf.array());
	}

	public static class Point2D {
		public final int x;
		public final int z;

		public Point2D(int x, int z) {
			this.x = x;
			this.z = z;
		}
	}
	
}
