package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.annotation.InPacket;
import fr.aquilon.minecraft.aquilonthings.annotation.OutPacket;
import fr.aquilon.minecraft.aquilonthings.module.IModule;
import fr.aquilon.minecraft.aquilonthings.module.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.Place;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.PlaceFactory;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.PlaceRadius;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.PlaceTypeEnum;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.PlayerEntersBiomeEvent;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.PlayerEntersPlaceEvent;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.PlayerLeavesPlaceEvent;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers.Trigger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers.TriggerCommand;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers.TriggerCommandCyclic;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers.TriggerCommandTickControl;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers.TriggerEventInfo;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers.TriggerExitCommand;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers.TriggerFactory;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers.TriggerFailedException;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers.TriggerLocalisation;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers.TriggerTypeEnum;
import fr.aquilon.minecraft.aquilonthings.utils.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Termm
 * @author BilliAlpha (billi.pamege.300@gmail.com)
 */
@AQLThingsModule(
		name = "AQLPlaces",
		cmds = {
				@Cmd(value = AQLPlaces.COMMAND, desc = "Gestion des lieux"),
				@Cmd(value = AQLPlaces.COMMAND_TRIGGER, desc = "Gestion des déclencheurs")
		},
		inPackets = @InPacket(AquilonThings.CHANNEL_READY),
		outPackets = @OutPacket(AQLPlaces.CHANNEL_PLACE)
)
public class AQLPlaces implements IModule {
	public static final ModuleLogger LOGGER = ModuleLogger.get();

	public static final String CHANNEL_PLACE = "place";
	public static final String DB_PREFIX = "aqlplaces_";
	public static final String PERM_PLACES = AquilonThings.PERM_ROOT+".places.edit"; // TODO: split view and edit
	public static final String PERM_EVENT_INFO = AquilonThings.PERM_ROOT+".places.eventinfo";
	public static final String COMMAND = "place";
	public static final String COMMAND_TRIGGER = "placetrg";

	public static final String[] INCOMING_PACKET_REGISTER_LIST = new String[] {AquilonThings.CHANNEL_READY};
	public static final String[] OUTGOING_PACKET_REGISTER_LIST = new String[] {CHANNEL_PLACE};
	public static final String[] COMMANDS_REGISTER_LIST = new String[] {"place", "placetrg"};

	public static final int PAGE_SIZE = 6;

	public DatabaseConnector db;
	public HashMap<String, Place> places = new HashMap<>();
	public HashMap<String, String> playerBiomes = new HashMap<>();
	
	@Override
	public boolean onStartUp(DatabaseConnector db) {
		this.db = db;
		loadPlacesFromDB();
		LOGGER.mInfo("Loaded "+places.size()+" places.");
		return true;
	}

	@Override
	public boolean onStop() {
		return true;
	}

	/*
	 * Si un joueur est dans la liste active d'une place
	 * On retrigger la place lors de sa connexion automatiquement
	 * (pour éviter les connards qui vont buguse en ne faisant rien.
	 */
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();

		Biome b = p.getLocation().getBlock().getBiome();
		PlayerEntersBiomeEvent e = new PlayerEntersBiomeEvent(p, b, null);
		playerBiomes.put(p.getUniqueId().toString(), b.name());
		e.call(this);

		for(Place place : places.values()){
			if(place.getActivePlayersList().contains(p)){
				place.trigger(this, p, true);
			}
		}
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if (!(cmd.equalsIgnoreCase(COMMAND) || cmd.equalsIgnoreCase(COMMAND_TRIGGER)))
		    return false;

        if (!sender.hasPermission(PERM_PLACES)) {
            LOGGER.mInfo("Accès interdit à " +sender.getName());
            sender.sendMessage(ChatColor.YELLOW + "AQLPlaces: Accès interdit\nTentative d'accès enregistrée.");
            return true;
        }
		
		if (cmd.equalsIgnoreCase(COMMAND)) {
			if (args.length<1) return false;
			if (args[0].equalsIgnoreCase("add")) {
				return addPlace(sender, args);
			}

			if (args[0].equalsIgnoreCase("remove")) {
				return removePlace(sender, args);
			}

			if (args[0].equalsIgnoreCase("list")) {
				return listPlaces(sender,args);
			}

			if (args[0].equalsIgnoreCase("info")) {
				return getPlaceInformation(sender, args);
			}

			if (args[0].equalsIgnoreCase("trigger")) {
				return triggerPlace(sender, args);
			}
			
			if (args[0].equalsIgnoreCase("reload")) {
				return reloadConfiguration(sender, args);
			}

			return false;
		}
		
		if (cmd.equalsIgnoreCase(COMMAND_TRIGGER)) {
			if (args.length<1) return false;
			if (args[0].equalsIgnoreCase("add")) {
				return addTrigger(sender, args);
			}

			if (args[0].equalsIgnoreCase("remove")) {
				return removeTrigger(sender, args);
			}
			
			if (args[0].equalsIgnoreCase("enable")) {
				return setTriggerState(sender, true, args);
			}
			
			if (args[0].equalsIgnoreCase("disable")) {
				return setTriggerState(sender, false, args);
			}
			
			if (args[0].equalsIgnoreCase("list")) {
				return listTriggers(sender, args);
			}

			if (args[0].equalsIgnoreCase("info")) {
				return getTriggerInformation(sender, args);
			}
			
			if (args[0].equalsIgnoreCase("trigger")) {
				return triggerTrigger(sender, args);
			}

			return false;
		}
		return false;
	}

	private boolean reloadConfiguration(CommandSender sender, String[] args) {
		places.clear();
		loadPlacesFromDB();
		sender.sendMessage(ChatColor.YELLOW + "AQLPlaces: " + places.size() + " places rechargées.");
		return true;
	}

	private boolean getTriggerInformation(CommandSender sender, String[] args) {
		String usage = "/placetrg info <place> <trigger>";
		if (args.length<3) {
			sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
			return true;
		}
		Place p = retrievePlaceFromName(args[1]);
		if (p==null) {
			sender.sendMessage(ChatColor.YELLOW + "Place inexistante, verifiez le nom fournis (" + ChatColor.WHITE +args[1]+ChatColor.YELLOW + ").");
			sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
			return true;
		}
		Trigger t = p.getTrigger(args[2]);
		if (t==null) {
			sender.sendMessage(ChatColor.YELLOW + "Trigger inexistant, verfiez le nom fournis (" + ChatColor.WHITE +args[2]+ChatColor.YELLOW + ").");
			sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
			return true;
		}
		sender.sendMessage(ChatColor.YELLOW + "Trigger "+ChatColor.WHITE+t.getName()+ChatColor.YELLOW +
                " (Place "+ChatColor.WHITE+p.getName()+ChatColor.YELLOW+"):\n    "+
                t.getInfos().replaceAll("\n", "\n    "));
		return true;
	}

	private boolean listTriggers(CommandSender sender, String[] args) {
		String usage = "/placetrg list <place> [page]";
		if (args.length<2) {
			sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
			return true;
		}
		Place p = retrievePlaceFromName(args[1]);
		if (p==null) {
			sender.sendMessage(ChatColor.YELLOW + "Place inexistante, verifiez le nom fournis (" + ChatColor.WHITE +args[1]+ChatColor.YELLOW + ").");
			sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
			return true;
		}
		List<Trigger> list = p.getTriggers();
		//*
		int page = 0;
		int nbPages = Math.round(list.size() / PAGE_SIZE);
		if (list.size()<PAGE_SIZE) nbPages=0;
		if (args.length>2) {
			try {
				page = Integer.parseInt(args[2])-1;
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatColor.YELLOW + "Numero de page invalide. Veuillez fournir un chiffre entre 1 et " +(nbPages+1)+ "." + ChatColor.WHITE);
				sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
				return true;
			}
		}
		if (page>nbPages || page<0) {
			sender.sendMessage(ChatColor.YELLOW + "Numero de page invalide. Veuillez fournir un chiffre entre 1 et " +(nbPages+1)+ "." + ChatColor.WHITE);
			sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
			return true;
		}
		sender.sendMessage(ChatColor.YELLOW + "Voici les triggers disponibles sur " + ChatColor.WHITE +p.getName() +ChatColor.YELLOW + " :");
		if (places.size()>PAGE_SIZE) {
			sender.sendMessage(ChatColor.YELLOW + " --- Page " +(page +1)+ "/" +(nbPages+1)+ " ---" + ChatColor.WHITE);
			list = new ArrayList<>(list.subList(PAGE_SIZE*page, Math.min(PAGE_SIZE*page +PAGE_SIZE, list.size())));
		}
		//*/
		for (Trigger t: list) {
			sender.sendMessage(ChatColor.YELLOW + "   - " + ChatColor.WHITE +t.getName() + " --> " +TriggerTypeEnum.fromClassName(t.getClass().getSimpleName()));
		}
		return true;
	}
	
	private boolean triggerTrigger(CommandSender sender, String[] args) {
		String usage = "/placetrg trigger <place> <trigger>";
		if (args.length<3) {
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		Place p = retrievePlaceFromName(args[1]);
		if (p==null) {
			sender.sendMessage(ChatColor.YELLOW + "Place inexistante, verifiez le nom fournis.");
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		Trigger t = p.getTrigger(args[2]);
		if (t==null) {
			sender.sendMessage(ChatColor.YELLOW + "Trigger inexistant, verfiez le nom fournis.");
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		int i = 0;
		for(Player player : p.getActivePlayersList())
			try {
				t.trigger(player, true);
				i++;
			} catch (TriggerFailedException e) {
				LOGGER.mDebug("Erreur d'exécution d'un trigger: " +e.trigger.getName() +
						" [id:" +e.trigger.getID()+ "] place : " +p.getName());
				e.printStackTrace();
			}
		sender.sendMessage(ChatColor.YELLOW + "Exécution sur " + ChatColor.WHITE +i+ChatColor.YELLOW + " joueurs.");
		return true;
	}

	private boolean setTriggerState(CommandSender sender, boolean state, String[] args) {
		String usage = "/placetrg "+(state?"enable":"disable")+" <place> <trigger>";
		if (args.length<3) {
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		Place p = retrievePlaceFromName(args[1]);
		if (p==null) {
			sender.sendMessage(ChatColor.YELLOW + "Place inexistante, verifiez le nom fournis.");
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		Trigger t = p.getTrigger(args[2]);
		if (t==null) {
			sender.sendMessage(ChatColor.YELLOW + "Trigger inexistant, verfiez le nom fournis.");
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		String stateText = (state?"":"dés")+"activ";
        if (updateTriggerStateInDB(t, state)) {
            sender.sendMessage(ChatColor.GREEN+"OK"+ChatColor.YELLOW+", trigger "+ChatColor.WHITE+t.getName() +
                    ChatColor.YELLOW+" "+stateText+"é (Place "+ChatColor.WHITE+p.getName()+ChatColor.YELLOW+") !");
            LOGGER.mInfo("Trigger "+t.getName()+" "+stateText+"é par "+sender.getName()+" sur la place "+p.getName());
        } else {
            sender.sendMessage(ChatColor.GOLD+"Erreur"+ChatColor.YELLOW + ", "+stateText+"ation impossible du trigger !");
            LOGGER.mInfo("Echec, "+stateText+"ation du trigger "+t.getName()+" sur la place "+p.getName());
        }
		return true;
	}

	private boolean removeTrigger(CommandSender sender, String[] args) {
		String usage = "/placetrg remove <place> <trigger>";
		if (args.length<3) {
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		Place p = retrievePlaceFromName(args[1]);
		if (p==null) {
			sender.sendMessage(ChatColor.YELLOW + "Place inexistante, verifiez le nom fournis (" + ChatColor.WHITE +args[1]+ChatColor.YELLOW + ").");
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		Trigger t = p.getTrigger(args[2]);
		if (t==null) {
			sender.sendMessage(ChatColor.YELLOW + "Trigger inexistant, verfiez le nom fournis (" + ChatColor.WHITE +args[2]+ChatColor.YELLOW + ").");
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		if (removeTriggerFromDB(p, t)) {
            sender.sendMessage(ChatColor.GREEN+"OK"+ChatColor.YELLOW+", trigger "+ChatColor.WHITE+t.getName() +
                    ChatColor.YELLOW+" correctement supprimé de la place "+ChatColor.WHITE+p.getName() +
                    ChatColor.YELLOW+" !");
            LOGGER.mInfo("Trigger "+t.getName()+" supprimé par "+sender.getName()+" sur la place "+p.getName());
        } else {
            sender.sendMessage(ChatColor.GOLD+"Erreur"+ChatColor.YELLOW + ", impossible de supprimer le trigger !");
            LOGGER.mInfo("Echec de la suppression du trigger "+t.getName()+" sur la place "+p.getName());
        }
		return true;
	}

	private boolean addTrigger(CommandSender sender, String[] args) {
		String usage = "/placetrg add <place> <nom> <type> [arguments]";
		if (args.length<4) {
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		Place p = retrievePlaceFromName(args[1]);
		if (p==null) {
			sender.sendMessage(ChatColor.YELLOW + "Place inexistante, verifiez le nom fournis (" +args[1]+ ").");
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
        TriggerTypeEnum type;
		try {
		    type = TriggerTypeEnum.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException e) {
			sender.sendMessage(ChatColor.YELLOW + "Type inexistant, options: "+ChatColor.WHITE +
                    String.join(ChatColor.YELLOW+", "+ChatColor.WHITE, TriggerTypeEnum.names())+
                    ChatColor.YELLOW+".");
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		
		if(checkTriggerExists(args[2], p)){
			sender.sendMessage(ChatColor.YELLOW + "Le trigger " +args[2]+ " pour la place " +p.getName() + " existe déjà.");
			sender.sendMessage(ChatColor.YELLOW + "Annulation de la commande.");
			return true;
		}

		Trigger trigger = null;
        if (type==TriggerTypeEnum.COMMAND) {
            usage = "/placetrg add <place> <nom> "+type.name()+" <command_line>";
            if (args.length<5) {
                sender.sendMessage(ChatColor.YELLOW + usage);
                return true;
            }

            String cmdLine = Utils.joinStrings(args," ",4);
            trigger = new TriggerCommand(0, p, args[2], true, LOGGER, cmdLine);

        } else if (type==TriggerTypeEnum.EXIT_COMMAND) {
            usage = "/placetrg add <place> <nom> "+type.name()+" <command_line>";
            if (args.length<5) {
                sender.sendMessage(ChatColor.YELLOW + usage);
                return true;
            }

            String cmdLine = Utils.joinStrings(args," ",4);
            trigger = new TriggerExitCommand(0, p, args[2], true, LOGGER, cmdLine);

        } else if (type==TriggerTypeEnum.COMMAND_CYCLIC) {
			usage = "/placetrg add <place> <nom> "+type.name()+" <interval> <variation max> <discriminator ? 1 ou 0> <command_line>";
			if (args.length<7) {
				sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
				return true;
			}
			int interval;
			int interval_variation;
			String cmdLine = Utils.joinStrings(args," ",7);
			
			try {
				interval = Integer.parseUnsignedInt(args[4]);
				interval_variation = Integer.parseUnsignedInt(args[5]);
				
				if(interval==0 || interval_variation>interval)
					throw new NumberFormatException();
				
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatColor.YELLOW + "Interval et/ou variation pas bon. Veuillez arrêter vos conneries.");
				sender.sendMessage(ChatColor.YELLOW + usage);
				return true;
			}
			
			boolean discriminate;
			discriminate = args[6].equals("1");

			trigger = new TriggerCommandCyclic(0, p, args[2], true, LOGGER, discriminate, cmdLine, interval, interval_variation);
			
		} else if (type==TriggerTypeEnum.LOCALISATION) {
			usage = "/placetrg add <place> <nom> "+type.name()+" <nom> <type> <duree> <description>";
			if (args.length<8) {
				sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
				return true;
			}
			int duree;
			try {
				duree = Integer.parseUnsignedInt(args[6]);
			} catch(NumberFormatException e) {
				sender.sendMessage(ChatColor.YELLOW + "Duree invalide. Veuillez fournir un nombre en secondes." + ChatColor.WHITE);
				sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
				return true;
			}
			String desc = Utils.joinStrings(args," ",7);
			trigger = new TriggerLocalisation(0, p, args[2], true, LOGGER, args[4], args[5], duree, desc);

		} else if (type==TriggerTypeEnum.EVENT_INFO) {
			usage = "/placetrg add <place> <nom> "+type.name()+" <nom> <url> <description>";
			if (args.length<7) {
				sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
				return true;
			}
			
			String desc = Utils.joinStrings(args," ",6);
			trigger = new TriggerEventInfo(0, p, args[2], true, LOGGER, args[4], desc, args[5]);
		}

        if (trigger==null) {
            sender.sendMessage(ChatColor.YELLOW + "Erreur !");
            return true;
        }

        if (insertTriggerIntoDB(p, trigger)) {
            sender.sendMessage(
                    ChatColor.GREEN+"OK"+ChatColor.YELLOW+", trigger " +
                    ChatColor.WHITE+trigger.getName()+ChatColor.YELLOW+" ajouté sur la place " +
                    ChatColor.WHITE+p.getName()+ChatColor.YELLOW+"."
            );
            LOGGER.mInfo("Trigger "+trigger+" ajouté par "+sender.getName()+" sur la place "+p+".");
        } else {
            sender.sendMessage(ChatColor.RED+"Erreur"+ChatColor.YELLOW+", impossible d'enregistrer le trigger.");
        }
		return true;
	}
	
	private boolean listPlaces(CommandSender sender, String[] args) {
		/*
		sender.sendMessage(ChatColor.YELLOW + "Voici les places disponibles :");
		for (Place p: places) {
			sender.sendMessage(ChatColor.YELLOW + "   - " + ChatColor.WHITE +p.name);
		}
		
		return true;
		*/
		
		// /*
		String usage = "/place list [page]";
		List<Place> list;
		int page = 0;
		int nbPages = Math.round(places.size() / PAGE_SIZE);
		if (places.size()<PAGE_SIZE) nbPages=0;
		if (args.length>1) {
			try {
				 page = Integer.parseInt(args[1])-1;
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatColor.YELLOW + "Numero de page invalide. Veuillez fournir un chiffre entre 1 et " +(nbPages+1)+ "." + ChatColor.WHITE);
				sender.sendMessage(ChatColor.YELLOW + usage);
				return true;
			}
		}
		if (page>nbPages || page<0) {
			sender.sendMessage(ChatColor.YELLOW + "Numero de page invalide. Veuillez fournir un chiffre entre 1 et " +(nbPages+1)+ "." + ChatColor.WHITE);
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		sender.sendMessage(ChatColor.YELLOW + "Voici les places disponibles :");
        list = new ArrayList<>(places.values());
		if (places.size()>PAGE_SIZE) {
			sender.sendMessage(ChatColor.YELLOW + " --- Page " +(page +1)+ "/" +(nbPages+1)+ " ---" + ChatColor.WHITE);
			list = list.subList(PAGE_SIZE*page, Math.min(PAGE_SIZE*page +PAGE_SIZE, places.size()));
		}
		for (Place p: list) {
			sender.sendMessage(ChatColor.YELLOW + "   - " + ChatColor.WHITE +p.getName() + "(" +p.getWorldName()+ ")");
		}
		return true;
		//*/
	}

	private boolean triggerPlace(CommandSender sender, String[] args) {
		String usage = "/place trigger <place>";
		if (args.length<2) {
			sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
			return true;
		}
		Place p = retrievePlaceFromName(args[1]);
		if (p==null) {
			sender.sendMessage(ChatColor.YELLOW + "Place inexistante, verifiez le nom fournis (" + ChatColor.WHITE +args[1]+ChatColor.YELLOW + ").");
			sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
			return true;
		}
		int i = 0;
		for(Player player : p.getActivePlayersList()){
			p.trigger(this, player, true);
			i++;
		}
		sender.sendMessage(ChatColor.YELLOW + "Exécution sur " + ChatColor.WHITE +i+ChatColor.YELLOW + " joueurs.");
		return true;
	}
	
	private boolean getPlaceInformation(CommandSender sender, String[] args) {
		String usage = "/place info <nom>";
		if (args.length<2){
			sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
			return true;
		}
		Place p = retrievePlaceFromName(args[1]);
		if (p==null) {
			sender.sendMessage(ChatColor.YELLOW + "Place inexistante. Verifiez le nom fournis. (" + ChatColor.WHITE +args[1]+ChatColor.YELLOW + ")");
			return true;
		}
		sender.sendMessage(ChatColor.YELLOW + "Place "+ChatColor.WHITE+p.getName()+ChatColor.YELLOW+":\n    "+
                p.getInfos().replaceAll("\n","\n    "));
		return true;
	}

	@SuppressWarnings("unchecked")
	private boolean removePlace(CommandSender sender, String[] args) {
		String usage = "/place remove <nom>";
		if (args.length<2){
			sender.sendMessage(ChatColor.YELLOW + usage +ChatColor.WHITE );
			return true;
		}
		Place p = retrievePlaceFromName(args[1]);
		if (p==null) {
			sender.sendMessage(ChatColor.YELLOW + "Place inexistante. Verifiez le nom fournis.");
			return true;
		}
        if (removePlaceTriggersFromDB(p) && removePlaceFromDB(p)) {
            sender.sendMessage(ChatColor.GREEN+"OK"+ChatColor.YELLOW+", place "+ChatColor.WHITE+p.getName() +
                    ChatColor.YELLOW+" correctement supprimé (et triggers associés) !");
            LOGGER.mInfo("Place "+p+" supprimée par "+sender.getName());
        } else {
            sender.sendMessage(ChatColor.GOLD+"Erreur"+ChatColor.YELLOW + ", impossible de supprimer la place !");
            LOGGER.mInfo("Echec de la suppression de la place "+p);
        }
		return true;
	}

	private boolean addPlace(CommandSender sender, String[] args) {
		String usage = "/place add <nom> <type> [arguments]";
		if (args.length<3){
			sender.sendMessage(ChatColor.YELLOW + usage);
			return true;
		}
		Place p = retrievePlaceFromName(args[1]);
		if (p!=null) {
			sender.sendMessage(ChatColor.YELLOW + "Nom déjà utilisé, veuillez en choisir un autre.");
			return true;
		}
        PlaceTypeEnum type;
		try {
		    type = PlaceTypeEnum.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.YELLOW + "Type inexistant, options: "+ChatColor.WHITE+
                    String.join(ChatColor.YELLOW+", "+ChatColor.WHITE, PlaceTypeEnum.names())+
                    ChatColor.YELLOW+".");
            sender.sendMessage(ChatColor.YELLOW + usage);
            return true;
        }

        Place newPlace = null;
		if (type==PlaceTypeEnum.RADIUS) {
            usage = "/place add <nom> "+type.getType()+" <rayon>";
            if (args.length<4) {
                sender.sendMessage(ChatColor.YELLOW + usage);
                return true;
            }
            Location playerLoc = sender.getServer().getPlayerExact(sender.getName()).getLocation();
            int radius;
            try {
                radius = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.YELLOW + "Rayon invalide. Veuillez fournir un chiffre.");
                sender.sendMessage(ChatColor.YELLOW + usage);
                return true;
            }
            newPlace = new PlaceRadius(0,args[1],playerLoc,radius);
        }

        if (newPlace==null) {
            sender.sendMessage(ChatColor.YELLOW + "Erreur !");
            return true;
        }

		if (insertPlaceIntoDB(newPlace)) {
            sender.sendMessage(
                    ChatColor.GREEN+"OK"+ChatColor.YELLOW+", place "+ChatColor.WHITE +
                    newPlace.getID()+ChatColor.YELLOW+"/"+ChatColor.WHITE+newPlace.getName()+ChatColor.YELLOW+" enregistrée."
            );
            LOGGER.mInfo("Place "+newPlace+" ajoutée par "+sender.getName()+".");
        } else {
            sender.sendMessage(ChatColor.RED+"Erreur"+ChatColor.YELLOW+", impossible d'enregistrer la place.");
        }
		return true;
	}
	
	/**
	 * Retrouver la première place dans la liste correspondant au nom fournis.
	 * @param name
	 * @return La place trouvée ou null sinon.
	 */
	private Place retrievePlaceFromName(String name) {
	    return places.get(name);
	}
	
	/**
	 * Récupérer les places de la BDD et les ajouter au cache
	 */
	private void loadPlacesFromDB() {
		Connection con = db.startTransaction();
		Place place;
		String sql = "SELECT id, type, name, world FROM "+DB_PREFIX+"places";
		ResultSet rs = db.query(con, sql);
		try {
			while (rs.next()) {
                place = PlaceFactory.getPlace(db,
                        rs.getInt("id"),
                        rs.getString("type"),
                        rs.getString("name"),
                        rs.getString("world"),
                        LOGGER
                );
                if (place==null) continue;
                loadTriggersFromDB(place);
                places.put(place.getName(), place);
			}
		} catch (SQLException e) {
			db.endTransaction(con, e, sql);
			return;
		}
		db.endTransaction(con);
	}
	
	/**
	 * Insérer la Place passée en argument dans la BDD et dans le cache
     * @return true on success, false on failure
	 */
	private boolean insertPlaceIntoDB(Place p) {
		String sql = "INSERT INTO "+DB_PREFIX+"places (id, type, name, world) VALUES (NULL,?,?,?)";
        Connection con = db.startTransaction();
        PreparedStatement stmt = db.prepare(con, sql, Statement.RETURN_GENERATED_KEYS);
        try {
            stmt.setString(1, p.getType().getType());
            stmt.setString(2, p.getName());
            stmt.setString(3, p.getWorldName());
            stmt.execute();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                p.setID(rs.getInt(1));
            }
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
        }
        db.endTransaction(con);
        if (!p.insertIntoDB(db, LOGGER)) return false;
        places.put(p.getName(), p);
        return true;
	}
	
	/**
	 * Retirer la Place passée en argument de la BDD et du cache
	 */
	private boolean removePlaceFromDB(Place p) {
        String sql1 = "DELETE FROM aqlplaces_places WHERE id = ?";
        String sql2 = "DELETE FROM aqlplaces_place_values WHERE id = ?";
		Connection con = db.startTransaction();
        PreparedStatement stmt1 = db.prepare(con, sql1);
        PreparedStatement stmt2 = db.prepare(con, sql2);
		try {
			stmt1.setInt(1, p.getID());
            stmt2.setInt(1, p.getID());
            stmt1.execute();
            stmt2.execute();
		} catch (SQLException e) {
			db.endTransaction(con, e);
			return false;
		}
		db.endTransaction(con);
		places.remove(p.getName());
		return true;
	}
	
	/**
	 * Récuperer les triggers de la Place passée en argument et les ajouter au cache
	 * @param place
	 * @return 
	 */
	private void loadTriggersFromDB(Place place){
		Trigger trigger;
		TriggerFactory triggerfactory = new TriggerFactory();
		String sql = "SELECT id, type, name, state FROM "+DB_PREFIX+"triggers WHERE place = ?";
		Connection con = db.startTransaction();
		PreparedStatement stmt = db.prepare(con, sql);
		try {
			stmt.setInt(1, place.getID());
			ResultSet rs = stmt.executeQuery();
			while (true) {
				if (rs.next()) {
					trigger = triggerfactory.getTrigger(db,
							rs.getInt(1),
							rs.getString(2),
							place,
							rs.getString(3),
							rs.getBoolean(4),
							LOGGER
					);
					if (trigger==null) continue;
					place.addTrigger(trigger);
					//LOGGER.mWarning("Ajout " +trigger.name + " à la place " +place.name);
					//LOGGER.mWarning(trigger.toString());
				} else {
					break;
				}
			}
		} catch (SQLException e) {
			db.endTransaction(con, e, sql);
			return;
		}	
		db.endTransaction(con);
	}
	
	/**
	 * Insérer le Trigger passé en argument dans la BDD et dans la Place
     * @return true on success, false on failure
	 */
	private boolean insertTriggerIntoDB(Place p, Trigger t) {
		String sql = "INSERT INTO "+DB_PREFIX+"triggers (id, place, type, name, state) VALUES (NULL,?,?,?,?)";
		Connection con = db.startTransaction();
		PreparedStatement stmt = db.prepare(con, sql, Statement.RETURN_GENERATED_KEYS);
		try {
			stmt.setInt(1, p.getID());
			stmt.setString(2, t.getType().name());
			stmt.setString(3, t.getName());
			stmt.setBoolean(4, t.isEnabled());
			stmt.execute();
			ResultSet rs = stmt.getGeneratedKeys();
			if (rs.next()) {
			    t.setID(rs.getInt(1));
            }
		} catch (SQLException e) {
			db.endTransaction(con, e, sql);
			return false;
		}
		db.endTransaction(con);
		boolean ok = t.insertIntoDB(db);
        if (ok) p.addTrigger(t);
        return ok;
	}
	
	private boolean checkTriggerExists(String triggerName, Place place){
		for (Place p : places.values())
			for (Trigger t : p.getTriggers())
				if((t.getName().equals(triggerName)) && (p.getName().equals(place.getName())))
					return true;
		return false;
	}

    /**
     * Retirer les trigger de la place passé en argument de la BDD et de la Place
     */
    private boolean removePlaceTriggersFromDB(Place p) {
        String sql1 = "DELETE FROM "+DB_PREFIX+"trigger_values WHERE trigger_id IN " +
                "(SELECT id FROM "+DB_PREFIX+"triggers WHERE place = ?)";
        String sql2 = "DELETE FROM "+DB_PREFIX+"triggers WHERE place = ?";
        Connection con = db.startTransaction(false);
        PreparedStatement stmt1 = db.prepare(con, sql1);
        PreparedStatement stmt2 = db.prepare(con, sql2);
        try {
            stmt1.setInt(1, p.getID());
            stmt2.setInt(1, p.getID());
            stmt1.execute();
            stmt2.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e);
            return false;
        }
        db.endTransaction(con);
        // On les vire la liste des ticks
        TriggerCommandTickControl.getInstance().triggerCommandList.removeAll(
                p.getTriggers().stream().filter(a -> a instanceof TriggerCommandCyclic).collect(Collectors.toList())
        );
        p.clearTriggers();
        return true;
    }

    /**
     * Retirer le Trigger passé en argument de la BDD et de la Place
     */
    private boolean removeTriggerFromDB(Place p, Trigger t) {
        String sql1 = "DELETE FROM "+DB_PREFIX+"trigger_values WHERE trigger_id = ?";
        String sql2 = "DELETE FROM "+DB_PREFIX+"triggers WHERE id = ?";
        Connection con = db.startTransaction(false);
        PreparedStatement stmt1 = db.prepare(con, sql1);
        PreparedStatement stmt2 = db.prepare(con, sql2);
        try {
            stmt1.setInt(1, t.getID());
            stmt2.setInt(1, t.getID());
            stmt1.execute();
            stmt2.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e);
            return false;
        }
        db.endTransaction(con);
        // On le vire la liste des ticks
        TriggerCommandTickControl.getInstance().triggerCommandList.remove(t);
        p.removeTrigger(t);
        return true;
    }

    /**
     * Mettre à jour l'état du trigger passé en argument dans la BDD et dans la Place
     * @return true on success, false on failure
     */
    private boolean updateTriggerStateInDB(Trigger t, boolean state) {
        String sql = "UPDATE "+DB_PREFIX+"triggers SET state = ? WHERE id = ?";
        Connection con = db.startTransaction();
        PreparedStatement stmt = db.prepare(con, sql);
        try {
            stmt.setBoolean(1, state);
            stmt.setInt(2, t.getID());
            stmt.execute();
        } catch (SQLException e) {
            db.endTransaction(con, e, sql);
            return false;
        }
        db.endTransaction(con);
        t.setState(state);
        return true;
    }
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Player p = event.getPlayer();
		if (p.getWorld().getTime() % 5 != 0){
			return;
		}
		Biome b = p.getLocation().getBlock().getBiome();
		String uuid = p.getUniqueId().toString();
		if (!b.name().equals(playerBiomes.get(uuid))) {
			Biome oldBiome = null;
			if (playerBiomes.containsKey(uuid)) oldBiome = Biome.valueOf(Biome.class, playerBiomes.get(uuid));
			PlayerEntersBiomeEvent e = new PlayerEntersBiomeEvent(p, b, oldBiome);
			playerBiomes.put(p.getUniqueId().toString(), b.name());
			e.call(this);
		}
		checkPlayerPlace(p, event.getTo());
	}

	private void checkPlayerPlace(Player p, Location location) {
		for (Place place : places.values()) {
			//LOGGER.mDebug("check " +place.name);

			//(int) Math.round(Math.sqrt( Math.pow(place.posX - location.getX(), 2) + Math.pow(place.posY - location.getY(), 2) + Math.pow(place.posZ - location.getZ(), 2) ));
			if (place.getActivePlayersList().contains(p)){
                // Le joueur était dans la place
                // Si il y est toujours on skip
                if(place.contains(location)) continue;

                // Sinon il sort de la place
                PlayerLeavesPlaceEvent evt = new PlayerLeavesPlaceEvent(p, place);
                evt.call(this);
                place.trigger(this, p, false);
                place.getActivePlayersList().remove(p);
                LOGGER.mDebug(p.getName() + " sort de la place: " + place.getName() + "[id:" + place.getID() + ", " + place.getWorldName()+ "].");
			} else {
                // Le joueur n'était pas dans la place
                // Si il n'y est toujours pas on skip
                if (!place.contains(location)) continue;

                // Sinon il entre dans la place
                PlayerEntersPlaceEvent evt = new PlayerEntersPlaceEvent(p, place);
                evt.call(this);
                place.trigger(this, p, true);
                place.getActivePlayersList().add(p);
                LOGGER.mDebug(p.getName() + " entre dans la place: " + place.getName() + "[id:" + place.getID() + ", " + place.getWorldName() + "].");
            }
		}
		
	}
}
