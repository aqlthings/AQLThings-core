package fr.aquilon.minecraft.aquilonthings.modules.AQLMarkers;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import fr.aquilon.minecraft.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

@AQLThingsModule(
		name = "AQLMarkers",
		cmds = {
		        @Cmd(value = "marker", desc = "Gestion des marqueurs"),
                @Cmd(value = "goto", desc = "Téléportation à un marqueur")
		}
)
public class AQLMarkers implements IModule {
    private MarkerGroup mainGroup;
    private Map<Integer, MarkerGroup> groupsByID;
    private Map<String, MarkerGroup> groupsByName;
	private static final ModuleLogger LOGGER = ModuleLogger.get();

	public static final String DB_TABLE_MARKERS = "aqlmarkers_markers";
	public static final String DB_TABLE_GROUPS = "aqlmarkers_groups";

	@Override
	public boolean onStartUp(DatabaseConnector db) {
		loadMarkers();
		return true;
	}

	@Override
	public boolean onStop() {
		return true;
	}

	private void loadMarkers() {
		DatabaseConnector db = AquilonThings.instance.getNewDatabaseConnector();
		Connection conn = db.startTransaction();
        final String sqlGroups = "SELECT * FROM "+DB_TABLE_GROUPS;
        ResultSet rs = db.query(conn, sqlGroups);
        if (rs == null) {
            db.endTransaction(conn);
            return;
        }
        groupsByID = new HashMap<>();
        groupsByName = new HashMap<>();
        try {
            while (rs.next()) {
                MarkerGroup g = MarkerGroup.build(rs);
                groupsByID.put(g.getId(), g);
                groupsByName.put(g.getName(), g);
            }
            rs.close();
        } catch (SQLException e) {
            db.endTransaction(conn, e, sqlGroups);
            LOGGER.log(Level.WARNING, null, "Couldn't load groups:", e);
            return;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, null, "Couldn't load groups:", e);
            return;
        }
		final String sqlMarkers = "SELECT * FROM "+DB_TABLE_MARKERS;
        rs = db.query(conn, sqlMarkers);
        if (rs == null) {
            db.endTransaction(conn);
            return;
        }
        mainGroup = new MarkerGroup(0, "main", null);
        try {
            while (rs.next()) {
                Marker m = Marker.build(rs);
                m.setGroup(groupsByID.get(rs.getInt("group_id")));
                mainGroup.add(m);
            }
            rs.close();
        } catch (SQLException e) {
            db.endTransaction(conn, e, sqlMarkers);
            LOGGER.log(Level.WARNING, null, "Couldn't load markers:", e);
            return;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, null, "Couldn't load markers:", e);
            return;
        }
        db.endTransaction(conn);
	}

    /**
     * API method to add a dynamic marker
     * @param marker The marker to add
     * @param group If not null, the name of a group to add the new marker to
     * @throws IllegalArgumentException If the given group name cannot be found
     */
	public void addMarker(IMarker marker, String group) {
	    mainGroup.add(marker);
	    if (group != null) {
	        MarkerGroup grp = groupsByName.get(group);
            if (grp == null) throw new IllegalArgumentException("Unknown group");
            grp.add(marker);
        }
    }

    /**
     * API method to add a dynamic marker group and it's childs
     * @param group The group to add
     * @throws IllegalArgumentException If the name is already used
     */
    public void addGroup(MarkerGroup group) {
        if (getGroup(group.getName()) != null)
            throw new IllegalArgumentException("Name already used");
        groupsByName.put(group.getName(), group);
        if (group.getMarkers().size() != 0) for (IMarker m : group) {
            if (m.getGroup() != group) continue;
            mainGroup.add(m);
        }
    }

    /**
     * @return A virtual group containing all markers
     */
    public MarkerGroup getMainGroup() {
        return mainGroup;
    }

    public MarkerGroup getGroup(int id) {
        return groupsByID.get(id);
    }

    public MarkerGroup getGroup(String name) {
        return groupsByName.get(name);
    }

    public Set<MarkerGroup> getGroups() {
        return Collections.unmodifiableSet(new HashSet<>(groupsByName.values()));
    }

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String arg1, String[] args) {
		if (arg1.equals("marker")) return commandMarker(sender, args);
		if (arg1.equals("goto")) return commandGoTo(sender, args);
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (alias.equals("marker")) return tabCompleteMarker(sender, args);
		if (alias.equals("goto")) return tabCompleteGoto(sender, args);
		return null;
	}

	public boolean commandMarker(CommandSender sender, String[] args) {
	    final String usage = ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/marker (new | edit | delete | list | group) [<args>]";
		if (args.length < 1) {
			sender.sendMessage(usage);
			return true;
		}
        if (args[0].equals("reload")) {
            if (!sender.hasPermission(AquilonThings.PERM_ROOT+".markers.reload")) {
                sender.sendMessage(ChatColor.YELLOW+"Forbidden (missing permissions");
                return true;
            }
            loadMarkers();
            sender.sendMessage(ChatColor.YELLOW+"Reloaded all markers and groups ...");
        } else if (args[0].equals("list")) {
            if (!sender.hasPermission(AquilonThings.PERM_ROOT+".markers.list")) {
                sender.sendMessage(ChatColor.YELLOW+"Forbidden (missing permissions");
                return true;
            }
            int page = 1;
            if (args.length > 2) try {
                page = Integer.parseUnsignedInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.YELLOW+"Invalid page number");
                return true;
            }
            sender.sendMessage(markerList(sender, args.length > 1 ? args[1] : null, page, 10));
        } else if (args[0].equals("new")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.YELLOW+"Player only command");
                return true;
            }
            if (!sender.hasPermission(AquilonThings.PERM_ROOT+".markers.edit")) {
                sender.sendMessage(ChatColor.YELLOW+"Forbidden (missing permissions");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/marker new <name> [<args>]");
                return true;
            }
            String name = args[1];
            if (mainGroup.getMarker(name) != null) {
                sender.sendMessage(ChatColor.YELLOW+"Name already used");
                return true;
            }
            Location pos = ((Player) sender).getLocation();
            Map<String, String> opts = Utils.readArguments(args, 2);
            String icon = opts.getOrDefault("icon", Marker.DEFAULT_ICON);
            String label = opts.getOrDefault("label", null);
            String desc = opts.getOrDefault("desc", null);
            String perm = opts.getOrDefault("perm", null);
            String groupArg = opts.getOrDefault("group", null);
            MarkerGroup group = null;
            if (groupArg != null) {
                group = argToGroup(groupArg);
                if (group == null) {
                    sender.sendMessage(ChatColor.YELLOW+"Unknown group");
                    return true;
                }
            }
            Marker m = new Marker(-1, pos, name, icon, label, desc, perm, group);
            DatabaseConnector db = AquilonThings.instance.getNewDatabaseConnector();
            Connection conn = db.startTransaction(false);
            try {
                m.saveToDB(conn);
            } catch (Exception e) {
                db.endTransaction(conn, false);
                sender.sendMessage(ChatColor.YELLOW+"Error while saving marker");
                LOGGER.log(Level.INFO, null, "Couldn't save marker:", e);
                return true;
            }
            db.endTransaction(conn);
            mainGroup.add(m);
            sender.sendMessage(ChatColor.YELLOW+"Saved marker: "+ChatColor.WHITE+m.asString());
            if (group != null)
                sender.sendMessage(ChatColor.YELLOW+"Added to group: "+ChatColor.WHITE+group.getName());
        } else if (args[0].equals("edit")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.YELLOW+"Player only command");
                return true;
            }
            if (!sender.hasPermission(AquilonThings.PERM_ROOT+".markers.edit")) {
                sender.sendMessage(ChatColor.YELLOW+"Forbidden (missing permissions");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/marker edit <name> [<args>]");
                return true;
            }
            IMarker marker = argToMarker(args[1]);
            if (marker == null) {
                sender.sendMessage(ChatColor.YELLOW+"Marker not found");
                return true;
            }
            if (!(marker instanceof Marker)) {
                sender.sendMessage(ChatColor.YELLOW+"This marker cannot be modified");
                return true;
            }
            Marker m = (Marker) marker;

            Map<String, String> opts = Utils.readArguments(args, 2);
            m.setIcon(opts.getOrDefault("icon", m.getIcon()));
            m.setLabel(opts.getOrDefault("label", m.getLabel()));
            m.setDescription(opts.getOrDefault("desc", m.getDescription()));
            m.setPermission(opts.getOrDefault("perm", m.getPermission()));
            if (opts.getOrDefault("move", "false").equals("true")) {
                m.setPosition(((Player) sender).getLocation());
            }
            DatabaseConnector db = AquilonThings.instance.getNewDatabaseConnector();
            Connection conn = db.startTransaction(false);
            try {
                m.saveToDB(conn);
            } catch (Exception e) {
                db.endTransaction(conn, false);
                sender.sendMessage(ChatColor.YELLOW+"Error while saving marker");
                LOGGER.log(Level.INFO, null, "Couldn't save marker:", e);
                return true;
            }
            db.endTransaction(conn);
            sender.sendMessage(ChatColor.YELLOW+"Saved marker: "+ChatColor.WHITE+m.asString());
        } else if (args[0].equals("group")) {
            if (!sender.hasPermission(AquilonThings.PERM_ROOT+".markers.edit")) {
                sender.sendMessage(ChatColor.YELLOW+"Forbidden (missing permissions");
                return true;
            }
			commandMarkerGroup(sender, args);
			return true;
		} else if (args[0].equals("delete")) {
            if (!sender.hasPermission(AquilonThings.PERM_ROOT+".markers.edit")) {
                sender.sendMessage(ChatColor.YELLOW+"Forbidden (missing permissions");
                return true;
            }
		    if (args.length < 3) {
		        sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/marker delete <id> <name>");
		        return true;
            }
		    int id;
		    try {
		        id = Integer.parseUnsignedInt(args[1]);
            } catch (NumberFormatException e) {
		        sender.sendMessage(ChatColor.YELLOW+"Invalid ID");
		        return true;
            }
		    IMarker marker = mainGroup.getMarker(id);
		    if (!(marker instanceof Marker)) {
		        sender.sendMessage(ChatColor.YELLOW+"No marker with this id");
		        return true;
            }
		    if (!marker.getName().equals(args[2])) {
		        sender.sendMessage(ChatColor.YELLOW+"Name doesn't match, aborting deletion");
		        return true;
            }
		    DatabaseConnector db = AquilonThings.instance.getNewDatabaseConnector();
		    Connection conn = db.startTransaction(false);
		    try {
                ((Marker) marker).deleteFromDB(conn);
            } catch (Exception e) {
		        db.endTransaction(conn, false);
                sender.sendMessage(ChatColor.YELLOW+"Error while deleting marker");
                return true;
            }
            db.endTransaction(conn);
		    MarkerGroup group = marker.getGroup();
		    if (group != null) group.remove(marker);
		    mainGroup.remove(marker);
            sender.sendMessage(ChatColor.YELLOW+"Marker deleted: "+ChatColor.WHITE+marker.asString());
        } else {
		    sender.sendMessage(usage);
		    return true;
        }
		return true;
	}

	public void commandMarkerGroup(CommandSender sender, String args[]) {
        final String usage = ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/marker group (new | delete | list | add | remove)";
        if (args.length < 2) {
            sender.sendMessage(usage);
            return;
        }
        if (args[1].equals("list")) {
            StringBuilder str = new StringBuilder(ChatColor.YELLOW.toString());
            str.append("All groups:\n ");
            Set<MarkerGroup> groups = getGroups();
            if (groups.size() == 0) str.append(ChatColor.GRAY).append(ChatColor.ITALIC).append("Empty list");
            else {
                str.append(groups.stream()
                        .map(g -> ChatColor.WHITE+"#"+g.getId()+" "+g.getName()+ChatColor.YELLOW)
                        .collect(Collectors.joining("\n ")));
            }
            sender.sendMessage(str.toString());
        } else if (args[1].equals("new")) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/marker group new <name> [<args>]");
                return;
            }
            String name = args[2];
            if (groupsByName.get(name) != null) {
                sender.sendMessage(ChatColor.YELLOW+"Name already used");
                return;
            }
            Map<String, String> opts = Utils.readArguments(args, 2);
            String label = opts.getOrDefault("label", null);
            MarkerGroup group = new MarkerGroup(-1, name, label);
            DatabaseConnector db = AquilonThings.instance.getNewDatabaseConnector();
            Connection conn = db.startTransaction(false);
            try {
                group.saveToDB(conn);
            } catch (Exception e) {
                db.endTransaction(conn, false);
                sender.sendMessage(ChatColor.YELLOW+"Error while saving group: "+e.getMessage());
                LOGGER.log(Level.INFO, null, "Couldn't save group:", e);
                return;
            }
            db.endTransaction(conn);
            groupsByID.put(group.getId(), group);
            groupsByName.put(group.getName(), group);
            sender.sendMessage(ChatColor.YELLOW+"Saved group: "+ChatColor.WHITE+"#"+group.getId()+" "+group.getDisplayName());
        } else if (args[1].equals("delete")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/marker group delete <id> <name>");
                return;
            }
            MarkerGroup group = argToGroup(args[2]);
            if (group == null) {
                sender.sendMessage(ChatColor.YELLOW+"Group not found");
                return;
            }
            if (!group.getName().equals(args[3])) {
                sender.sendMessage(ChatColor.YELLOW+"Name doesn't match, aborting deletion");
                return;
            }
            DatabaseConnector db = AquilonThings.instance.getNewDatabaseConnector();
            Connection conn = db.startTransaction(false);
            try {
                group.deleteFromDB(conn);
            } catch (Exception e) {
                db.endTransaction(conn, false);
                sender.sendMessage(ChatColor.YELLOW+"Error while deleting group");
                return;
            }
            db.endTransaction(conn);
            groupsByID.remove(group.getId());
            groupsByName.remove(group.getName());
            sender.sendMessage(ChatColor.YELLOW+"Group deleted: "+ChatColor.WHITE+"#"+group.getId()+" "+group.getDisplayName());
        } else if (args[1].equals("add") || args[1].equals("remove")) {
            if (args.length < 4) {
                sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/marker group (add|remove) <marker> <group>");
                return;
            }
            IMarker marker = argToMarker(args[2]);
            if (marker == null) {
                sender.sendMessage(ChatColor.YELLOW+"Marker "+ChatColor.WHITE+args[2]+ChatColor.YELLOW+" not found");
                return;
            }
            MarkerGroup group = argToGroup(args[3]);
            if (group == null) {
                sender.sendMessage(ChatColor.YELLOW+"Group "+ChatColor.WHITE+args[2]+ChatColor.YELLOW+" not found");
                return;
            }
            boolean present = args[1].equals("add");
            try {
                group.updateMarkerInDB(marker, present);
            } catch (Exception e) {
                sender.sendMessage(ChatColor.YELLOW+"Error while "+(present ? "adding" : "removing")+" marker");
                return;
            }
            if (present) group.add(marker);
            else group.remove(marker);
            sender.sendMessage(ChatColor.YELLOW+"Successfully "+(present ? "added" : "removed")+" marker "+
                    ChatColor.WHITE+marker.getDisplayName()+ChatColor.YELLOW+" to group "+
                    ChatColor.WHITE+group.getDisplayName());
        } else {
            sender.sendMessage(usage);
            return;
        }
    }

	public String markerList(CommandSender sender, String groupName, int page, int pageSize) {
		if (page < 1 || pageSize < 1) return ChatColor.YELLOW+"Invalid page number";
		MarkerGroup group = groupName == null || groupName.equals("all") ? mainGroup : getGroup(groupName);
		if (group == null) return ChatColor.YELLOW+"Unknown group";
		StringBuilder str = new StringBuilder(ChatColor.YELLOW.toString());
		if (group == mainGroup) str.append("All markers");
		else str.append("Markers in group ").append(ChatColor.WHITE).append(group.getName()).append(ChatColor.YELLOW);
		Set<IMarker> set = group.getMarkers();
		str.append(" (page ").append(page).append("/").append((int) Math.ceil(set.size()/(1d*pageSize))).append(")");
		str.append(":\n");
		List<String> markers = set.stream()
                .filter(m -> m.checkPermission(sender))
                .sorted(Comparator.comparing(IMarker::getName))
				.skip((page-1)*pageSize).limit(pageSize)
				.map(IMarker::asString)
				.collect(Collectors.toList());
		if (markers.size() == 0) str.append("  ").append(ChatColor.GRAY).append(ChatColor.ITALIC).append("Empty list");
		else {
			str.append(" ").append(ChatColor.WHITE);
			str.append(String.join("\n "+ChatColor.WHITE, markers));
		}
		return str.toString();
	}

	private List<String> tabCompleteMarker(CommandSender sender, String[] args) {
		if (args.length == 1) return Arrays.asList("reload", "new", "delete", "list", "group");
		if (args.length == 2 && args[0].equals("group")) return Arrays.asList("new", "delete", "list", "add", "remove");
		return null;
	}

	public boolean commandGoTo(CommandSender sender, String[] args) {
        if (!sender.hasPermission(AquilonThings.PERM_ROOT+".markers.use")) {
            sender.sendMessage(ChatColor.YELLOW+"Forbidden (missing permissions");
            return true;
        }
		if (args.length < 1) {
			sender.sendMessage(ChatColor.YELLOW+"Usage: "+ChatColor.WHITE+"/goto (<marker-name> | <group-name> <marker-name> [<targets>])");
			return true;
		}
		MarkerGroup group = mainGroup;
		List<Player> targets = sender instanceof Player ? Collections.singletonList((Player) sender) : null;
		String markerName;
		if (args.length == 1) markerName = args[0];
		else {
            if (!args[0].equals("main")) group = getGroup(args[0]);
			markerName = args[1];
			if (args.length > 2) try {
				targets = Bukkit.selectEntities(sender, args[2]).stream()
						.filter(e -> e instanceof Player).map(e -> (Player) e).collect(Collectors.toList());
			} catch (IllegalArgumentException e) {
				sender.sendMessage(ChatColor.YELLOW+"Invalid target selector: "+e.getMessage());
				return true;
			}
		}
		if (group == null) {
			sender.sendMessage(ChatColor.YELLOW+"Unknown marker group");
			return true;
		}
		if (targets == null || targets.size() == 0) {
			sender.sendMessage(ChatColor.YELLOW+"No targets");
			return true;
		}
		IMarker marker = group.getMarker(markerName);
		if (marker == null) {
			sender.sendMessage(ChatColor.YELLOW+"No marker with this name in this group");
			return true;
		}
		if (!marker.checkPermission(sender)) {
			sender.sendMessage(ChatColor.YELLOW+"Forbidden (invalid permissions)");
			return true;
		}
		long count = targets.stream()
				.map(p -> p.teleport(marker.getPosition(), PlayerTeleportEvent.TeleportCause.COMMAND))
				.filter(Boolean::booleanValue).count();
		sender.sendMessage(ChatColor.YELLOW+"Teleported "+count+"/"+targets.size()+" players");
		return true;
	}

	private List<String> tabCompleteGoto(CommandSender sender, String[] args) {
        if (!sender.hasPermission(AquilonThings.PERM_ROOT+".markers.use")) return null;
        if (args.length == 1 && args[0].length() >= 2) {
            return mainGroup.getMarkers().stream().filter(m -> m.getName().startsWith(args[0]))
                    .map(IMarker::getName).collect(Collectors.toList());
        }
		return null;
	}

	private IMarker argToMarker(String arg) {
        int id = 0;
        try {
            id = Integer.parseUnsignedInt(arg);
        } catch (NumberFormatException ignored) {}
        if (id > 0) return mainGroup.getMarker(id);
        return mainGroup.getMarker(arg);
    }

	private MarkerGroup argToGroup(String arg) {
        int id = 0;
        try {
            id = Integer.parseUnsignedInt(arg);
        } catch (NumberFormatException ignored) {}
        if (id > 0) return getGroup(id);
        return getGroup(arg);
    }
}
