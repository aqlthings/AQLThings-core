package fr.aquilon.minecraft.aquilonthings.modules.AQLEmotes;

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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Hashtable;
import java.util.List;

@AQLThingsModule(
		name = "AQLEmotes",
		cmds = {
				@Cmd(value = "emote", desc = "Gestion des emotes", aliases = {"em"}),
				@Cmd(value = "emotex", desc = "Forcer une emote", aliases = {"emx"})
		},
		inPackets = @InPacket(AquilonThings.CHANNEL_READY),
		outPackets = @OutPacket(AQLEmotes.CHANNEL_EMOTE)
)
public class AQLEmotes implements IModule {
	public static final ModuleLogger LOGGER = ModuleLogger.get();

	public static final String CHANNEL_EMOTE = "emote";

	public static final String PERM_EMOTE_OTHERS = AquilonThings.PERM_ROOT+".emote.others";

	private Hashtable<String, String> activeEmotes;
	
	public AQLEmotes() {
		activeEmotes = new Hashtable<>();
	}
	
	@Override
	public boolean onStartUp(DatabaseConnector db) {
		return true;
	}

	@Override
	public boolean onStop() {
		return true;
	}

	@Override
	public void onPluginMessageReceived(String channel, Player p, byte[] cmdString) {
		if (!channel.equals(AquilonThings.CHANNEL_READY)) return;
		// Donner les emotes actives des autres joueurs à celui venant de se connecter.
		for (Player joueur : Bukkit.getServer().getOnlinePlayers()) {
			String pUUID = joueur.getUniqueId().toString().replaceAll("-","");
			if (activeEmotes.containsKey(pUUID)){
				AquilonThings.sendPluginMessage(p, CHANNEL_EMOTE, (pUUID+":"+getEmote(joueur)).getBytes());
			}
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String scmd, String[] args) {
		if (cmd.getName().equalsIgnoreCase("em") || cmd.getName().equalsIgnoreCase("emote")) {
			if (args.length>=1 && args[0].equalsIgnoreCase("list")) {
				sendEmoteList(sender);
				return true;
			} else return startEmote(sender, args);
		} else if (cmd.getName().equalsIgnoreCase("emx") || cmd.getName().equalsIgnoreCase("emotex")) {
			return setPlayerEmote(sender, args);
		}
		return false;
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Player p = event.getPlayer();
		String pUUID = p.getUniqueId().toString().replaceAll("-","");
		// On check si le joueur fait une emote...
		if (activeEmotes.containsKey(pUUID)) {
			int fromX = (int) event.getFrom().getX();
			int fromY = (int) event.getFrom().getY();
			int fromZ = (int) event.getFrom().getZ();
			int toX = (int) event.getTo().getX();
			int toY = (int) event.getTo().getY();
			int toZ = (int) event.getTo().getZ();

			// On ne check que si le joueur bouge d'un bloc, pas de mini-mouvement.
			if (fromX != toX || fromZ != toZ || fromY != toY) {
				activeEmotes.remove(pUUID);
				PlayerEmoteEvent evt = new PlayerEmoteEvent(p, null);
				evt.call(this);
				sendUpdatePacket(p);
			}
		}
	}

	/**
	 * Cette fonction vérifie si l'emote existe et envoi le paquet à tout le monde.
	 * @param sender The person who called this command
	 * @param args The parameter: the emote
	 * @return true on success, false on failure
	 */
	public boolean startEmote(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.YELLOW + "Commande non accessible depuis la console. La console n'exprime pas d'emotion de toutes manieres.");
			return true;
		}
		Player player = (Player) sender;
		String pUUID = player.getUniqueId().toString().replaceAll("-","");
		if (args.length < 1) {
			sender.sendMessage(ChatColor.YELLOW + "Utilisation: /emote <emote> OU /em <emote>");
		} else {
			String emote = args[0];
			if (checkEmoteExists(emote)) {
				activeEmotes.put(pUUID, emote);
				PlayerEmoteEvent evt = new PlayerEmoteEvent(player, emote);
				evt.call(this);
				sendUpdatePacket(player);
			} else {
				sender.sendMessage(ChatColor.YELLOW + "Emote inconnue.");
				sendEmoteList(sender);
				return true;
			}
		}
		return true;
	}

	/**
	 * Cette fonction vérifie si l'emote existe et definit l'emote d'un joueur.
	 * @param sender The person who called this command
	 * @param args The parameters (player name and emote)
	 * @return true on success, false on failure
	 */
	public boolean setPlayerEmote(CommandSender sender, String[] args) {
		if (sender instanceof Player && !sender.hasPermission(PERM_EMOTE_OTHERS)) {
			sender.sendMessage(ChatColor.YELLOW + "Occupe toi de ce qui te concerne. Laisse ça aux autres !");
			return true;
		}
		if (args.length < 2) {
			sender.sendMessage(ChatColor.YELLOW + "Utilisation: /emx <joueur> <emote> OU /emotex <joueur> <emote>");
		} else {
			Player target = Bukkit.getPlayer(args[0]);
			if (target == null) {
				sender.sendMessage(ChatColor.YELLOW + "Impossible de trouver le joueur.");
				return true;
			}
			String emote = args[1];
			if (checkEmoteExists(emote)) {
				activeEmotes.put(target.getUniqueId().toString().replaceAll("-",""), emote);
				PlayerEmoteEvent evt = new PlayerEmoteEvent(target, emote);
				evt.call(this);
				sendUpdatePacket(target);
			} else {
				sender.sendMessage(ChatColor.YELLOW + "Emote inconnue.");
				sendEmoteList(sender);
				return true;
			}
		}
		return true;
	}

	/**
	 * Send the list of available emotes to the target
	 * @param target The target to send the list to
	 */
	private void sendEmoteList(CommandSender target) {
		List<String> listEmotes = AquilonThings.instance.getConfig().getStringList("emotes");
		if (listEmotes==null || listEmotes.size()<1) {
			LOGGER.mWarning("Aucune emote trouvé dans le fichier de configuration !");
			target.sendMessage(ChatColor.YELLOW + "Le plugin vient de se tauler, aucune emote détectée.");
			return;
		}
		target.sendMessage(ChatColor.YELLOW + "Liste des emotes:");
		StringBuilder str = new StringBuilder();
		for (String s : listEmotes) {
			str.append(ChatColor.YELLOW + ", " + ChatColor.WHITE).append(s);
		}
		target.sendMessage(ChatColor.YELLOW + "> " + str.substring(str.indexOf(",")+2));
	}

	public void sendUpdatePacket(Player p) {
		String pUUID = p.getUniqueId().toString().replaceAll("-","");
		for (Player player : Bukkit.getOnlinePlayers()) {
			AquilonThings.sendPluginMessage(player, CHANNEL_EMOTE, (pUUID+":"+getEmote(p)).getBytes());
		}
	}
	
	/***
	 * Cette fonction met en forme le résultat de l'interrogation du tableau.
	 * @param player le joueur
	 * @return L'emote séléctioné, ou "null"
	 */
	public String getEmote(Player player) {
		String res = activeEmotes.get(player.getUniqueId().toString().replaceAll("-",""));
		if (res == null)
			return "null";
		else
			return res;
	}

	/**
	 * Cette fonction vérifie si l'emote indiquée existe réellement.
	 * Vérification depuis le fichier de config AQLThings.
	 * @param emote l'emote a vérifier
	 * @return true if it exists, false otherwise
	 */
	private boolean checkEmoteExists(String emote) {
		List<String> listEmotes = AquilonThings.instance.getConfig().getStringList("emotes");
		if (listEmotes==null || listEmotes.size()<1) {
			LOGGER.mWarning("Aucune emote trouvé dans le fichier de configuration !");
			return false;
		}
		for (String s : listEmotes) {
			if (s.equalsIgnoreCase(emote))
				return true;
		}
		return false;
	}
	
}
