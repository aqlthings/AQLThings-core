package fr.aquilon.minecraft.aquilonthings.modules.AQLCalendar;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.annotation.InPacket;
import fr.aquilon.minecraft.aquilonthings.annotation.OutPacket;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.messaging.PluginMessageRecipient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Time management module
 * @author Billi
 * @author Dniektr
 */
@AQLThingsModule(
		name = "AQLCalendar",
		cmds = @Cmd(value = AQLCalendar.COMMAND, desc = "Manage worlds time"),
		inPackets = @InPacket(AquilonThings.CHANNEL_READY),
		outPackets = {@OutPacket(AQLCalendar.CHANNEL_WORLD), @OutPacket(AQLCalendar.CHANNEL_TYPE)}
)
public class AQLCalendar implements IModule {
    public static final ModuleLogger LOGGER = ModuleLogger.get();
	public static final String COMMAND = "calendar";
	public static final String CHANNEL_WORLD = "cal_world";
	public static final String CHANNEL_TYPE = "cal_type";

	public static final String PERM_CALENDAR_EDIT = AquilonThings.PERM_ROOT+".calendar.edit.";
	public static final String PERM_CALENDAR_READ = AquilonThings.PERM_ROOT+".calendar.read.";

	private DatabaseConnector db;
	private Map<String, WorldCalendar> worlds;
	private Map<String, CalendarType> types;

	@Override
	public boolean onStartUp(DatabaseConnector db) {
		this.db = db;
		this.worlds = new HashMap<>();
		this.types = new HashMap<>();
		sendUpdatePackets();
		return true;
	}

	@Override
	public boolean onStop() {
		return true;
	}

	public WorldCalendar getWorldCalendar(String worldName) {
		return worlds.computeIfAbsent(worldName, w -> {
			WorldCalendar cal = retrieveWorldCalendar(w);
			if (cal == null) cal = new WorldCalendar(w, null);
			return cal;
		});
	}

	public CalendarType getCalendarType(String type) {
		return types.computeIfAbsent(type, this::retrieveCalendarType);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
		if (!cmd.getName().equalsIgnoreCase(COMMAND)) return false;

		if (!sender.hasPermission(PERM_CALENDAR_EDIT)) {
			sender.sendMessage(ChatColor.RED + "Seuls les staffeux sont maîtres du temps.");
			return true;
		}

		String USAGE = ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/"+COMMAND+" (get | set) [args...]";
		if (args.length == 0) {
			sender.sendMessage(USAGE);
			return true;
		}

		if (args[0].equalsIgnoreCase("get")) return runGetWorldCalendar(sender, args);
		else if (args[0].equalsIgnoreCase("set")) return runSetWorldFixedSetting(sender, args);
		else sender.sendMessage(USAGE);
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		if (!cmd.getName().equalsIgnoreCase(COMMAND)) return null;
		if (!sender.hasPermission(PERM_CALENDAR_EDIT)) return null;
		if (args.length == 1) return Stream.of("get", "set")
				.filter(s -> args[0].length() < 1 || s.startsWith(args[0])).collect(Collectors.toList());
		if (args.length == 2 && args[0].equals("get")) return Bukkit.getWorlds().stream().map(World::getName)
				.filter(s -> args[1].length() < 1 || s.startsWith(args[1])).collect(Collectors.toList());
		return null;
	}

	// ---- Commands ----

	private boolean runGetWorldCalendar(CommandSender sender, String[] args) {
		World world = null;
		if (args.length > 1) world = Bukkit.getWorld(args[1]);
		else if (sender instanceof Player) world = ((Player) sender).getWorld();
		if (world == null) {
			sender.sendMessage(ChatColor.YELLOW+"Missing or invalid world name");
			sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/"+COMMAND+" get [<world>]");
			return true;
		}
		if (!sender.hasPermission(PERM_CALENDAR_READ.concat(world.getName()))) {
			sender.sendMessage(ChatColor.YELLOW+"You are not allowed to get the season info for this world");
			return true;
		}
		WorldCalendar info = getWorldCalendar(world.getName());
		sender.sendMessage(ChatColor.YELLOW+"Calendar info for "+ChatColor.WHITE+world.getName()+ChatColor.YELLOW+":");
		if (info.hasCalendar()) {
			sender.sendMessage(ChatColor.YELLOW + "  Calendar type: " + ChatColor.WHITE + info.getType().getName());
			sender.sendMessage(ChatColor.YELLOW + "  Year: " + ChatColor.WHITE + info.getYear() +
					ChatColor.GRAY + (info.isFixedYear() ? "" : " (auto)"));
			sender.sendMessage(ChatColor.YELLOW + "  Season: " + ChatColor.WHITE + info.getSeason().getId() +
					ChatColor.GRAY + (info.isFixedSeason() ? "" : " (auto)"));
			sender.sendMessage(ChatColor.YELLOW + "  Month: " + ChatColor.WHITE + info.getMonth().getName() +
					ChatColor.GRAY + (info.isFixedMonth() ? "" : " (auto)"));
			sender.sendMessage(ChatColor.YELLOW + "  Date: " + ChatColor.WHITE + info.getDate());
			sender.sendMessage(ChatColor.YELLOW + "  Time: " + ChatColor.WHITE + info.getTime());
			sender.sendMessage(ChatColor.YELLOW + "  Percentage: " + ChatColor.WHITE + info.getCurrentDayRatio()*100+"%");
			sender.sendMessage(ChatColor.YELLOW + "  Day ticks: " + ChatColor.WHITE + info.getCurrentDayTicks() +
					ChatColor.YELLOW + "/" + ChatColor.WHITE + info.getTotalDayLength());
			sender.sendMessage(ChatColor.YELLOW + "  Total ticks: " + ChatColor.GRAY + info.getWorldTime());
		} else {
			sender.sendMessage(ChatColor.GRAY+""+ChatColor.ITALIC+"  No calendar");
		}
		return true;
	}

	private boolean runSetWorldFixedSetting(CommandSender sender, String[] args) {
		String USAGE = ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/"+COMMAND+
				" set (month | season | year) (auto | <value>) [<world>]";
		if (args.length < 3) {
			sender.sendMessage(USAGE);
			return true;
		}
		World world = args.length > 3 ? Bukkit.getWorld(args[3]) : null;
		if (world == null && sender instanceof Player) {
			world = ((Player) sender).getWorld();
		}
		if (world == null) {
			sender.sendMessage(ChatColor.YELLOW+"Missing or invalid world name");
			return true;
		}
		if (!sender.hasPermission(PERM_CALENDAR_EDIT.concat(world.getName()))) {
			sender.sendMessage(ChatColor.YELLOW+"You are not allowed to get the season info for this world");
			return true;
		}
		WorldCalendar info = getWorldCalendar(world.getName());
		// TODO: update month/season/year
		if (updateWorldCalendar(info)) {
			sender.sendMessage(ChatColor.YELLOW+"Saved calendar for "+ChatColor.WHITE+world.getName());
		} else {
			sender.sendMessage(ChatColor.GOLD+"Error: "+ChatColor.YELLOW+"Couldn't save calendar !");
		}
		return true;
	}
	
	// ---- Database ----

	private WorldCalendar retrieveWorldCalendar(String worldName) {
		/* // FIXME
		Connection con = db.startTransaction();
		WorldCalendar res = null;
		try {
			PreparedStatement stmt = db.prepare(con, SQL_GET_SEASON);
			stmt.setString(1, worldName);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				res = new WorldCalendar(worldName, rs.getLong("day_length"));
				String season = rs.getString("season");
				res.setSeasons(season != null ? Season.valueOf(season) : null);
				res.setYear(rs.getInt("year"));
			}
		} catch (SQLException e) {
			db.endTransaction(con, e, SQL_GET_SEASON);
			return null;
		}
		db.endTransaction(con);
		return res;
		*/
		return new WorldCalendar(worldName, getCalendarType("test"));
	}

	private boolean updateWorldCalendar(WorldCalendar info) {
		/* // FIXME
		Connection con = db.startTransaction();
		try {
			PreparedStatement stmt = db.prepare(con, SQL_UPDATE_SEASON);
			stmt.setString(1, info.getWorldName());
			stmt.setLong(2, info.getTotalDayLength());
			stmt.setLong(5, info.getTotalDayLength());
			stmt.setInt(3, info.getYear());
			stmt.setInt(6, info.getYear());
			stmt.setString(4, info.getSeasons().name());
			stmt.setString(7, info.getSeasons().name());
			stmt.executeUpdate();
		} catch (SQLException e) {
			db.endTransaction(con, e, SQL_UPDATE_SEASON);
			return false;
		}
		db.endTransaction(con);
		*/
		return true;
	}

	private CalendarType retrieveCalendarType(String type) {
		// TODO
		return new CalendarBuilder(type)
				.dayLength(144000)
				.daysPerMonth(30)
				.seasonDaysOffset(80)
				.addSeason("winter", "Hiver", 0.5f)
				.addSeason("spring", "Spring", 0.58f)
				.addSeason("summer", "Summer", 0.66f)
				.addSeason("autumn", "Autumn", 0.58f)
				.addMonth("january", "January", 30)
				.addMonth("february", "February", 30)
				.addMonth("march", "March", 30)
				.addMonth("april", "April", 30)
				.addMonth("may", "May", 30)
				.addMonth("june", "June", 30)
				.addMonth("july", "July", 30)
				.addMonth("august", "August", 30)
				.addMonth("september", "September", 30)
				.addMonth("october", "October", 30)
				.addMonth("november", "November", 30)
				.addMonth("december", "December", 30)
				.build();
	}

	public boolean updateCalendarType(CalendarType type) {
		// TODO
		return true;
	}
	
	// ---- Misc ---- //

    /**
     * Send a calendar update packet for this world
	 * @param w The world whose info should be sent
	 * @param target The update packet recipient
     */
    private void sendUpdatePacket(World w, PluginMessageRecipient target) {
    	WorldCalendar info = getWorldCalendar(Objects.requireNonNull(w).getName());
    	if (info == null || !info.hasCalendar()) return;
		AquilonThings.sendPluginMessage(target, CHANNEL_TYPE,
				info.getType().getUpdatePacketData().getBytes());
		AquilonThings.sendPluginMessage(target, CHANNEL_WORLD,
				info.getUpdatePacketData().getBytes());
    }

	/**
	 * Send an update packet to the player and to the server for the player current world
	 * @param p A player
	 */
	private void sendUpdatePacketWorld(Player p) {
		sendUpdatePacket(p.getWorld(), null);
		sendUpdatePacket(p.getWorld(), p);
	}

	/**
	 * Send a calendar update to all players and to server
	 */
	private void sendUpdatePackets() {
		Set<WorldCalendar> calendars = Bukkit.getWorlds().stream()
				.map(World::getName).map(this::getWorldCalendar).filter(Objects::nonNull)
				.filter(WorldCalendar::hasCalendar)
				.collect(Collectors.toSet());
		AquilonThings.sendServerMessage(CHANNEL_TYPE, String.join(";", calendars.stream()
				.map(WorldCalendar::getType).map(CalendarType::getUpdatePacketData)
				.collect(Collectors.toSet())).getBytes());
		AquilonThings.sendServerMessage(CHANNEL_WORLD, String.join(";", calendars.stream()
				.map(WorldCalendar::getUpdatePacketData)
				.collect(Collectors.toSet())).getBytes());
		Bukkit.getOnlinePlayers().forEach(p -> sendUpdatePacket(p.getWorld(), p));
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] data) {
    	if (channel.equals(AquilonThings.CHANNEL_READY))
			sendUpdatePacketWorld(player);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent evt) {
		sendUpdatePacketWorld(evt.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void onWorldLoaded(WorldLoadEvent evt) {
		sendUpdatePacket(evt.getWorld(), null);
	}
}