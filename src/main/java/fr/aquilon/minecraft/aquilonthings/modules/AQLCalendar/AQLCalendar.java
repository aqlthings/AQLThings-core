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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
	public static final String PERM_CALENDAR_RELOAD = AquilonThings.PERM_ROOT+".calendar.reload";

	private static final String SQL_UPDATE_MONTH = "INSERT INTO aqlcalendar_months VALUES (?, ?, ?, ?, ?) " +
			"ON DUPLICATE KEY UPDATE name = ?, days = ?";
	private static final String SQL_UPDATE_SEASON = "INSERT INTO aqlcalendar_seasons VALUES (?, ?, ?, ?, ?) " +
			"ON DUPLICATE KEY UPDATE name = ?, day_ratio = ?";
	private static final String SQL_UPDATE_TYPE = "INSERT INTO aqlcalendar_types VALUES (?, ?, ?) " +
			"ON DUPLICATE KEY UPDATE day_length = ?, seasons_offset = ?";
	private static final String SQL_UPDATE_WORLD = "INSERT INTO aqlcalendar_worlds" +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
			"ON DUPLICATE KEY UPDATE type = ?, y_year = ?, fixed_year = ?, s_season = ?, fixed_season = ?, " +
			"m_month = ?, fixed_month = ?, d_day = ?, fixed_day = ?";

	private static final String SQL_FIND_WORLD = "SELECT * FROM aqlcalendar_worlds WHERE world = ?";
	private static final String SQL_FIND_ALL_WORLDS = "SELECT * FROM aqlcalendar_worlds";
	private static final String SQL_FIND_MONTH = "SELECT * FROM aqlcalendar_months WHERE type = ? AND id = ?";
	private static final String SQL_FIND_TYPE_MONTHS = "SELECT * FROM aqlcalendar_months WHERE type = ? " +
			"ORDER BY i_index";
	private static final String SQL_FIND_SEASON = "SELECT * FROM aqlcalendar_seasons WHERE type = ? AND id = ?";
	private static final String SQL_FIND_TYPE_SEASONS = "SELECT * FROM aqlcalendar_seasons WHERE type = ? " +
			"ORDER BY i_index";
	private static final String SQL_FIND_TYPE = "SELECT * FROM aqlcalendar_types WHERE type = ?";

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

		String USAGE = ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/"+COMMAND+" (get | set) [args...]";
		if (args.length == 0) {
			sender.sendMessage(USAGE);
			return true;
		}

		if (args[0].equals("reload")) {
			this.worlds.clear();
			this.types.clear();
			sendUpdatePackets();
			sender.sendMessage(ChatColor.YELLOW+"Reloaded all calendars");
			return true;
		}

		if (args[0].equalsIgnoreCase("get")) return runGetWorldCalendar(sender, args);
		else if (args[0].equalsIgnoreCase("set")) {
			USAGE = ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/"+COMMAND+" set (world | type) [args...]";
			if (args.length < 2) {
				sender.sendMessage(USAGE);
				return true;
			}
			if (args[1].equals("world")) return runSetWorldFixedSetting(sender, args);
			else if (args[1].equals("type")) return runSetTypeSetting(sender, args);
			else sender.sendMessage(USAGE);
		} else sender.sendMessage(USAGE);
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
		if (!cmd.getName().equalsIgnoreCase(COMMAND)) return null;
		WorldCalendar cal = null;
		if (sender instanceof Player) {
			Player player = ((Player) sender);
			if (!sender.hasPermission(PERM_CALENDAR_EDIT.concat(player.getWorld().getName()))) return null;
			cal = getWorldCalendar(player.getWorld().getName());
		}

		if (args.length == 1) return Stream.of("get", "set")
				.filter(s -> args[0].length() < 1 || s.startsWith(args[0])).collect(Collectors.toList());
		if (args.length == 2 && args[0].equals("get")) return Bukkit.getWorlds().stream().map(World::getName)
				.filter(s -> args[1].length() < 1 || s.startsWith(args[1])).collect(Collectors.toList());
		if (args.length == 2 && args[0].equals("set")) return Stream.of("world", "type")
				.filter(s -> args[1].length() < 1 || s.startsWith(args[1])).collect(Collectors.toList());
		if (args.length == 3 && args[0].equals("set") && args[1].equals("world"))
			return Stream.of("day", "month", "season", "year")
				.filter(s -> args[2].length() < 1 || s.startsWith(args[2])).collect(Collectors.toList());
		if (cal != null) {
			if (args.length == 4 && args[0].equals("set") && args[1].equals("world") && args[2].equals("month")) {
				List<String> res = cal.getType().getMonths().stream().map(Month::getId)
						.filter(s -> args[3].length() < 1 || s.startsWith(args[3])).collect(Collectors.toList());
				res.add("auto");
				return res;
			}
			if (args.length == 4 && args[0].equals("set") && args[1].equals("world") && args[2].equals("season")) {
				List<String> res = cal.getType().getSeasons().stream().map(Season::getId)
						.filter(s -> args[3].length() < 1 || s.startsWith(args[3])).collect(Collectors.toList());
				res.add("auto");
				return res;
			}
		}
		if (args.length == 5 && args[0].equals("set") && args[1].equals("world"))
			return Bukkit.getWorlds().stream().map(World::getName)
					.filter(s -> args[4].length() < 1 || s.startsWith(args[4])).collect(Collectors.toList());
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
			sender.sendMessage(ChatColor.YELLOW + "  Month: " + ChatColor.WHITE + info.getMonth().getId() +
					ChatColor.GRAY + (info.isFixedMonth() ? "" : " (auto)"));
			sender.sendMessage(ChatColor.YELLOW + "  Day: " + ChatColor.WHITE + info.getDay() +
					ChatColor.GRAY + (info.isFixedDay() ? "" : " (auto)"));
			sender.sendMessage(ChatColor.YELLOW + "  Date: " + ChatColor.WHITE + info.getDate());
			sender.sendMessage(ChatColor.YELLOW + "  Time: " + ChatColor.WHITE + info.getTime());
			sender.sendMessage(ChatColor.YELLOW + "  Percentage: " + ChatColor.WHITE + info.getCurrentDayRatio()*100+"%");
			sender.sendMessage(ChatColor.YELLOW + "  Day ticks: " + ChatColor.WHITE + info.getCurrentDayTicks() +
					ChatColor.YELLOW + "/" + ChatColor.WHITE + info.getTotalDayLength());
			sender.sendMessage(ChatColor.YELLOW + "  Total ticks: " + ChatColor.GRAY + info.getWorld().getFullTime());
		} else {
			sender.sendMessage(ChatColor.GRAY+""+ChatColor.ITALIC+"  No calendar");
		}
		return true;
	}

	private boolean runSetTypeSetting(CommandSender sender, String[] args) {
		String USAGE = ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/"+COMMAND+
				" set type <type> <key> <value>";
		if (args.length < 4) {
			sender.sendMessage(USAGE);
			return true;
		}
		// TODO
		sender.sendMessage("Not implemented yet !");
		return true;
	}

	private boolean runSetWorldFixedSetting(CommandSender sender, String[] args) {
		String USAGE = ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/"+COMMAND+
				//" set world ((type <type>) | ((month | season | year) (auto | <value>))) [<world>]";
				" set world (day | month | season | year) (auto | <value>) [<world>]";
		if (args.length < 4) {
			sender.sendMessage(USAGE);
			return true;
		}
		World world = args.length > 4 ? Bukkit.getWorld(args[4]) : null;
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
		String field = args[2];
		String value = args[3];
		if (field.equals("day")) {
			if (value.equals("auto")) info.setAutoDay();
			else try {
				info.setFixedDay(Integer.parseUnsignedInt(value)-1);
			} catch (Exception err) {
				sender.sendMessage(ChatColor.YELLOW+"Invalid day, expected a number");
				return true;
			}
		} else if (field.equals("month")) {
			if (value.equals("auto")) info.setAutoMonth();
			else {
				Month month = info.getType().getMonths().get(value);
				if (month == null) {
					sender.sendMessage(ChatColor.YELLOW + "Invalid month");
					return true;
				}
				info.setFixedMonth(month);
			}
		} else if (field.equals("season")) {
			if (value.equals("auto")) info.setAutoSeason();
			else if (value.equals("next")) info.nextSeason();
			else if (value.equals("previous")) info.previousSeason();
			else {
				Season season = info.getType().getSeasons().get(value);
				if (season == null) {
					sender.sendMessage(ChatColor.YELLOW + "Invalid season");
					return true;
				}
				info.setFixedSeason(season);
			}
		} else if (field.equals("year")) {
			if (value.equals("auto")) info.setAutoYear();
			else try {
				info.setFixedYear(Integer.parseUnsignedInt(value));
			} catch (Exception err) {
				sender.sendMessage(ChatColor.YELLOW+"Invalid year, expected a number");
				return true;
			}
		} else {
			sender.sendMessage(ChatColor.YELLOW+"Unknown field");
			return true;
		}
		if (updateWorldCalendar(info)) {
			sender.sendMessage(ChatColor.YELLOW+"Saved calendar for "+ChatColor.WHITE+world.getName());
			sendUpdatePacketWorld(world);
		} else {
			sender.sendMessage(ChatColor.GOLD+"Error: "+ChatColor.YELLOW+"Couldn't save calendar !");
		}
		return true;
	}
	
	// ---- Database ----

	private WorldCalendar retrieveWorldCalendar(String worldName) {
		Connection con = db.startTransaction();
		String calType = null;
		int year = 0, day = 0;
		String monthId = null, seasonId = null;
		boolean fixedYear, fixedMonth, fixedSeason, fixedDay;
		fixedYear = fixedMonth = fixedSeason = fixedDay = false;
		try {
			PreparedStatement stmt = db.prepare(con, SQL_FIND_WORLD);
			stmt.setString(1, worldName);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				calType = rs.getString("type");
				year = rs.getInt("y_year");
				fixedYear = rs.getBoolean("fixed_year");
				day = rs.getInt("d_day");
				fixedDay = rs.getBoolean("fixed_day");
				monthId = rs.getString("m_month");
				fixedMonth = rs.getBoolean("fixed_month");
				seasonId = rs.getString("s_season");
				fixedSeason = rs.getBoolean("fixed_season");
			}
		} catch (SQLException e) {
			db.endTransaction(con, e, SQL_FIND_WORLD);
			return null;
		}
		db.endTransaction(con);
		if (calType == null) return null;
		CalendarType type = getCalendarType(calType);
		if (type == null) return null;
		return new WorldCalendar(worldName, type)
				.setYear(year, fixedYear)
				.setSeason(type.getSeasons().get(seasonId), fixedSeason)
				.setMonth(type.getMonths().get(monthId), fixedMonth)
				.setDay(day, fixedDay);
	}

	private boolean updateWorldCalendar(WorldCalendar info) {
		// TODO
		return true;
	}

	private CalendarType retrieveCalendarType(String type) {
		CalendarTypeBuilder builder = new CalendarTypeBuilder(type);
		Connection con = db.startTransaction();
		try {
			PreparedStatement stmt = db.prepare(con, SQL_FIND_TYPE);
			stmt.setString(1, builder.getType());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				builder.dayLength(rs.getLong("day_length"))
						.seasonDaysOffset(rs.getInt("seasons_offset"));
			}
		} catch (SQLException e) {
			db.endTransaction(con, e, SQL_FIND_TYPE);
			return null;
		}
		db.endTransaction(con);
		if (!retrieveCalendarMonths(builder)) return null;
		if (!retrieveCalendarSeasons(builder)) return null;
		return builder.build();
	}

	private boolean retrieveCalendarMonths(CalendarTypeBuilder builder) {
		Connection con = db.startTransaction();
		try {
			PreparedStatement stmt = db.prepare(con, SQL_FIND_TYPE_MONTHS);
			stmt.setString(1, builder.getType());
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				builder.addMonth(rs.getString("id"),
						rs.getString("name"), rs.getInt("days"));
			}
		} catch (SQLException e) {
			db.endTransaction(con, e, SQL_FIND_TYPE_MONTHS);
			return false;
		}
		db.endTransaction(con);
		return true;
	}

	private boolean retrieveCalendarSeasons(CalendarTypeBuilder builder) {
		Connection con = db.startTransaction();
		try {
			PreparedStatement stmt = db.prepare(con, SQL_FIND_TYPE_SEASONS);
			stmt.setString(1, builder.getType());
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				builder.addSeason(rs.getString("id"),
						rs.getString("name"), rs.getFloat("day_ratio"));
			}
		} catch (SQLException e) {
			db.endTransaction(con, e, SQL_FIND_TYPE_SEASONS);
			return false;
		}
		db.endTransaction(con);
		return true;
	}

	public boolean updateCalendarType(CalendarType type) {
		// TODO
		return true;
	}
	
	// ---- Misc ---- //

    /**
     * Send a calendar update packet for this world
	 * @param w The world whose info should be sent
	 * @param target The update packet recipient (A player, or <code>null</code> for the server)
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
	private void sendUpdatePacketPlayer(Player p) {
		sendUpdatePacket(p.getWorld(), null);
		sendUpdatePacket(p.getWorld(), p);
	}

	/**
	 * Send an update packet to the server and all players in a world
	 * @param w A world
	 */
	private void sendUpdatePacketWorld(World w) {
		sendUpdatePacket(w, null);
		w.getPlayers().forEach(p -> sendUpdatePacket(w, p));
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
			sendUpdatePacketPlayer(player);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent evt) {
		sendUpdatePacketPlayer(evt.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void onWorldLoaded(WorldLoadEvent evt) {
		sendUpdatePacket(evt.getWorld(), null);
	}
}