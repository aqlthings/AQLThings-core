package fr.aquilon.minecraft.aquilonthings.modules.AQLSeason;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.annotation.InPacket;
import fr.aquilon.minecraft.aquilonthings.annotation.OutPacket;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Gestion des années et des saisons
 * Created on 06/02/2019
 * @author Dniektr
 */
@AQLThingsModule(
		name = "AQLSeason",
		cmds = @Cmd(value = AQLSeason.COMMAND, desc = "Gestion du temps"),
		inPackets = @InPacket(AquilonThings.CHANNEL_READY),
		outPackets = @OutPacket(AQLSeason.CHANNEL_SAISON)
)
public class AQLSeason implements IModule {
	public static final String COMMAND = "saison";
	public static final String CHANNEL_SAISON = "season";
	
	public static final String PERM_SEASON_EDIT = AquilonThings.PERM_ROOT+".saison.edit";
	
	// Libellé des saisons
	public static final String[] SEASON_LIST = new String[] {"Printemps", "Eté", "Automne", "Hiver"};
	// Durées en ticks des jours|nuits
	public static final String[] DN_LENGTH_LIST = new String[] {"84000:60000", "96000:48000", "84000:60000", "72000:72000"};
	public static int DUREE_JOUR_EN_TICKS = 144000; //~2 heures
	
	public static final String SQL_UPDATE_SEASON = "UPDATE aqlseason SET season = ?;";
	public static final String SQL_ADD_YEAR = "UPDATE aqlseason SET year = year + ?;";
	public static final String SQL_GET_SEASON = "SELECT season FROM aqlseason;";
	public static final String SQL_GET_YEAR = "SELECT year FROM aqlseason;";

	private DatabaseConnector db;
	
	private int seasonID = 0;
	private int yearValue = 0;

	@Override
	public boolean onStartUp(DatabaseConnector db) {
		this.db = db;
		init();
		AquilonThings.instance.sendServerMessage(CHANNEL_SAISON, getDayNightLength(getDbSeason()).getBytes());
		return true;
	}

	@Override
	public boolean onStop() {
		return true;
	}
	
	public void init() {
		seasonID = getDbSeason();
		yearValue = getDbYear();
		for (Player p : Bukkit.getOnlinePlayers()) {
			sendUpdatePacket(p);
		}
	}

    public int getSeason() {
        return seasonID;
    }

    public String getSeasonLabel() {
        return getSeasonLabelFromID(seasonID);
    }

    public int getYear() {
        return yearValue;
    }

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String scmd, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "Argument obligatoire.");
			return false;
		}

		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (!player.hasPermission(PERM_SEASON_EDIT)) {
				player.sendMessage(ChatColor.RED + "Seuls les staffeux sont maîtres du temps.");
			}
		}

		if (cmd.getName().equalsIgnoreCase("saison")) {
			if (args[0].equalsIgnoreCase("avancer") && args.length == 3) {
				if (args[2].equalsIgnoreCase("saison")) {
					return moveOnXSeasons(sender, args[1]);
				} else if (args[2].equalsIgnoreCase("annee")) {
					return moveOnXYears(sender, args[1]);
				}
			} else if (args[0].equalsIgnoreCase("reculer") && args.length == 3) {
				if (args[2].equalsIgnoreCase("saison")) {
					return moveBackXSeasons(sender, args[1]);
				} else if (args[2].equalsIgnoreCase("annee")) {
					return moveBackXYears(sender, args[1]);
				}
			}
		}
		return false;
	}

	// --------- //
	// COMMANDES //
	// --------- //

	public boolean moveOnXSeasons(CommandSender sender, String arg) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			int quantity = checkQuantityValue(player, arg);
			if (quantity==0) return true;
			incrementXSeasons(quantity);
			sendUpdatePackets();
			player.sendMessage(ChatColor.DARK_AQUA + "La saison a été incrémentée de " + quantity + ".");
			sendUpdatedDateLogToPlayer(player);
		}
		return true;
	}
	
	public boolean moveBackXSeasons(CommandSender sender, String arg) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			int quantity = checkQuantityValue(player, arg);
			if (quantity==0) return true;
			incrementXSeasons(-quantity);
			sendUpdatePackets();
			player.sendMessage(ChatColor.DARK_AQUA + "La saison a été décrémentée de " + quantity + ".");
            sendUpdatedDateLogToPlayer(player);
		}
		return true;
	}
	
	public boolean moveOnXYears(CommandSender sender, String arg) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			int quantity = checkQuantityValue(player, arg);
			if (quantity==0) return true;
			incrementXYears(quantity);
			sendUpdatePackets();
			player.sendMessage(ChatColor.DARK_AQUA + "L'année a été incrémentée de " + quantity + ".");
            sendUpdatedDateLogToPlayer(player);
		}
		return true;
	}
	
	public boolean moveBackXYears(CommandSender sender, String arg) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			int quantity = checkQuantityValue(player, arg);
			if (quantity==0) return true;
			incrementXYears(-quantity);
			sendUpdatePackets();
			player.sendMessage(ChatColor.DARK_AQUA + "L'année a été décrémentée de " + quantity + ".");
            sendUpdatedDateLogToPlayer(player);
		}
		return true;
	}
	
	
	// ----------- //
	// UTILITAIRES //
	// ----------- //

	public int checkQuantityValue(Player player, String arg) {
		int quantity;
		try {
			quantity = Integer.parseInt(arg);
		} catch (Exception e) {
			player.sendMessage(ChatColor.RED + "L'argument " + arg + " doit être de type numérique.");
			return 0;
		}
		if (quantity <= 0) {
			player.sendMessage(ChatColor.RED + "Le nombre de saisons doit être strictement positif.");
			return 0;
		}
		return quantity;
	}
	
	public void sendUpdatedDateLogToPlayer(Player player) {
		player.sendMessage(ChatColor.DARK_GREEN + "La date est désormais fixée à : " + getSeasonLabel() + " " + yearValue + ".");
	}
	
	public void incrementXSeasons(int nombre) {
		int actualSeason = getDbSeason();
		int newSeason = actualSeason + nombre;
		
		if (newSeason >= SEASON_LIST.length) {
			incrementXYears(newSeason / SEASON_LIST.length);
			newSeason = newSeason % SEASON_LIST.length;
		} else if (newSeason < 0) {
			int shiftedYears = (newSeason - SEASON_LIST.length + 1) / SEASON_LIST.length;
			incrementXYears(shiftedYears);
			newSeason = newSeason % SEASON_LIST.length;
			if (newSeason < 0) newSeason += SEASON_LIST.length;
		}
		updateDbSeason(newSeason);
		this.seasonID = newSeason;
	}
	
	public void incrementXYears(int nombre) {
		addDbYear(nombre);
		this.yearValue = getDbYear();
	}
	
	public String getSeasonLabelFromID(int seasonID) {
		return SEASON_LIST[seasonID];
	}
	
	public String getDayNightLength(int seasonID) {
		return DN_LENGTH_LIST[seasonID];
	}

	public int getDayLength(int seasonID) {
		String dnLength = DN_LENGTH_LIST[seasonID];
		return Integer.parseInt(dnLength.split(":")[0]);
	}

	public int getNightLength(int seasonID) {
		String dnLength = DN_LENGTH_LIST[seasonID];
		return Integer.parseInt(dnLength.split(":")[1]);
	}

	public String getHoraire(long time) {
		String zeroH = getHour(time) < 10 ? "0" : "";
		String zeroM = getMinute(time) < 10 ? "0" : "";
		return "Il est " + zeroH + getHour(time) + ":" + zeroM + getMinute(time);
	}

	public int getHour(long time) {
		long temps = time + DUREE_JOUR_EN_TICKS / 3;
		return (int)((float)(temps % DUREE_JOUR_EN_TICKS) / ((float)DUREE_JOUR_EN_TICKS / 24.0F));
	}

	public int getMinute(long time) {
		long temps = time + DUREE_JOUR_EN_TICKS / 3;
		return (int)((float)(temps % (DUREE_JOUR_EN_TICKS / 24)) / ((float)DUREE_JOUR_EN_TICKS / 24.0F / 60.0F));
	}
	
	
	// ----------- //
	// APPELS BDD  //
	// ----------- //
	
	private void updateDbSeason(int season) {
		Connection con = db.startTransaction();
		try {
            PreparedStatement stmt = db.prepare(con, SQL_UPDATE_SEASON);
            stmt.setInt(1, season);
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_UPDATE_SEASON);
            return;
        }
		db.endTransaction(con);
	}

    private void addDbYear(int year) {
		Connection con = db.startTransaction();
		try {
            PreparedStatement stmt = db.prepare(con, SQL_ADD_YEAR);
            stmt.setInt(1, year);
            stmt.executeUpdate();
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_ADD_YEAR);
            return;
        }
		db.endTransaction(con);
	}

    private int getDbSeason() {
		int res = 0;
		Connection con = db.startTransaction();
		try {
            PreparedStatement stmt = db.prepare(con, SQL_GET_SEASON);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = rs.getInt(1);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_GET_SEASON);
            return 0;
        }
		db.endTransaction(con);
		return res;
	}

    private int getDbYear() {
		int res = 0;
		Connection con = db.startTransaction();
		try {
            PreparedStatement stmt = db.prepare(con, SQL_GET_YEAR);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                res = rs.getInt(1);
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, SQL_GET_YEAR);
            return 0;
        }
		db.endTransaction(con);
		return res;
	}
	
	
	// ------ //
	// DIVERS //
	// ------ //

    /**
     * Cette méthode envoit un paquet à chaque client connecté.
     */
    private void sendUpdatePackets() {
        for (Player joueur : Bukkit.getOnlinePlayers()) {
            joueur.sendPluginMessage(AquilonThings.instance, AquilonThings.CHANNEL_PREFIX+':'+CHANNEL_SAISON,
                    (
                            getSeasonLabelFromID(getDbSeason()) + ":" +
									getDbYear() + ":" +
                                    getDayNightLength(getDbSeason())
                    ).getBytes()
            );
        }
    }

    /**
     * Cette méthode envoie au client un paquet contenant la date et la saison mise à jour
     * @param p Le joueur a qui envoyer le packet
     */
    private void sendUpdatePacket(Player p) {
        p.sendPluginMessage(AquilonThings.instance, AquilonThings.CHANNEL_PREFIX+':'+CHANNEL_SAISON,
                (
                        getSeasonLabelFromID(getDbSeason()) + ":" +
								getDbYear() + ":" +
                                getDayNightLength(getDbSeason())
                ).getBytes()
        );
    }

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] data) {
    	if (channel.equals(AquilonThings.CHANNEL_PREFIX+':'+AquilonThings.CHANNEL_READY))
			sendUpdatePacket(player);
	}
}