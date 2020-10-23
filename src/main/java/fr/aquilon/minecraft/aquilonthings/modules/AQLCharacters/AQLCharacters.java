package fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.annotation.InPacket;
import fr.aquilon.minecraft.aquilonthings.annotation.OutPacket;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.AbstractCharacter;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharRace;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.Character;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterPlayer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterSkill;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterSkin;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CommonSkin;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.PlayerContext;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.TempCharacter;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Module de gestion des personnages.
 *
 * TODO: CharacterChangeEvent
 * TODO: Only two packets (One for character, one for skin)
 */
@AQLThingsModule(
		name = "AQLCharacters",
		cmds = @Cmd(value = AQLCharacters.COMMAND, desc = "Gestion des personnages"),
		inPackets = @InPacket(AquilonThings.CHANNEL_READY),
		outPackets = {
				@OutPacket(AQLCharacters.CHANNEL_NAME), @OutPacket(AQLCharacters.CHANNEL_SKIN),
				@OutPacket(AQLCharacters.CHANNEL_RACE), @OutPacket(AQLCharacters.CHANNEL_SCALE),
				@OutPacket(AQLCharacters.CHANNEL_SKILLS)
		}
)
public class AQLCharacters implements IModule {
	public static final ModuleLogger LOGGER = ModuleLogger.get();

	public static final String COMMAND = "perso";

	public static final String CHANNEL_RACE = "race";
	public static final String CHANNEL_SCALE = "scale";
	public static final String CHANNEL_NAME = "name";
	public static final String CHANNEL_SKIN = "skin";
	public static final String CHANNEL_SKILLS = "skills";

	public static final String PERM_CREATE_TEMP_CHAR = AquilonThings.PERM_ROOT+".characters.create.chars.tempo";
	public static final String PERM_SELECT_CHARACTER = AquilonThings.PERM_ROOT+".characters.category.{{CATEGORY}}.chars.select";
    public static final String PERM_SELECT_ANY_CHARACTER = AquilonThings.PERM_ROOT+".characters.any.chars.select";
	public static final String PERM_SELECT_SKIN = AquilonThings.PERM_ROOT+".characters.category.{{CATEGORY}}.skins.select";
	public static final String PERM_INFO = AquilonThings.PERM_ROOT+".characters.info";
	public static final String PERM_SCALE = AquilonThings.PERM_ROOT+".characters.scale";

	private CharacterDatabase charDB;
	private Map<String, Float> playerScales;

	public AQLCharacters() {
		charDB = new CharacterDatabase(this);
		playerScales = new HashMap<>();
	}
	
	@Override
	public boolean onStartUp(DatabaseConnector db) {
		for (Player p : Bukkit.getOnlinePlayers()) {
			sendUpdatePackets(p);
		}
		return true;
	}

	@Override
	public boolean onStop() {
		return true;
	}

	@Override
	public void onPluginMessageReceived(String channel, Player p, byte[] bytes) {
		if (channel.equals(AquilonThings.CHANNEL_READY)) { // Player joined server
            charDB.updatePlayer(p);
		    // Step one: send info about new player to everyone
			// Step two: send info about connected players to new player
            PlayerContext newPlayer = PlayerContext.getFromPlayer(p, charDB);
			if (newPlayer==null) return;
			p.setDisplayName(newPlayer.getName());
			for (Player target : Bukkit.getOnlinePlayers()) {
				sendPlayerPackets(target, newPlayer);

				if (target!=p) sendPlayerPackets(p, PlayerContext.getFromPlayer(target, charDB));
			}
			sendServerPacket(newPlayer);
			// Send scales
			for (String pUUID : playerScales.keySet()) {
				sendScale(p, pUUID, playerScales.get(pUUID));
			}
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if (cmd.equalsIgnoreCase("perso")) {
			if (args.length>=1 && args[0].equalsIgnoreCase("info")) {
				// Info sur un joueur
				if (!sender.hasPermission(PERM_INFO)) {
					sender.sendMessage(ChatColor.YELLOW+"Oust ! Touche pas à ça !");
					return true;
				}
				if (args.length<2) {
					sender.sendMessage(ChatColor.YELLOW+"Usage: /perso info (<joueur> | <nomRP>)");
					return true;
				}
				Player target = Bukkit.getPlayer(args[1]);
				if (target==null) {
                    List<CharacterPlayer> players = charDB.findPlayersFromCharacterName(
                            Utils.joinStrings(args, " ", 1))
							.stream().filter((p) -> Bukkit.getPlayer(p.getUUIDObject())!=null)
							.collect(Collectors.toList());
                    if (players.size()==1) {
                        target = Bukkit.getPlayer(players.get(0).getUUIDObject());
                    } else if (players.size()==0) {
						sender.sendMessage(ChatColor.YELLOW+"Aucun personnage connecté avec ce nom.");
						return true;
					} else {
						String pList = ChatColor.WHITE+players.stream()
								.map(CharacterPlayer::getUsername)
								.collect(Collectors.joining(ChatColor.YELLOW+", "+ChatColor.WHITE));
						sender.sendMessage(ChatColor.YELLOW+"Plus d'un joueur trouvé. Précisez la recherche\n" +
								"Liste : "+pList);
						return true;
					}
				}
				if (target==null) {
                    sender.sendMessage(ChatColor.YELLOW+"Aucun joueur trouvé avec ces paramètres.");
                    return true;
                }
				PlayerContext pCtx = PlayerContext.getFromPlayer(target, charDB);
				String data = ChatColor.GRAY+"=== "+Utils.decoratePlayerName(target)+
						ChatColor.GRAY+" ===\n"+ChatColor.YELLOW;
				if (pCtx.character == null) {
					data += "Aucun personnage séléctionné\n";
				} else {
					data += "Personnage ";
					if (pCtx.character instanceof TempCharacter) data += "temporaire";
					else data += "sélectionné";
					data += " :";
					if (pCtx.character instanceof Character)
						data += ChatColor.GRAY+" #"+((Character) pCtx.character).getID();
					data += " "+ChatColor.WHITE+pCtx.character.getName()+ChatColor.YELLOW +
							" ("+ChatColor.GRAY+pCtx.character.getRace()+" "+
							(pCtx.character.getSex()=='M' ? "male" : "femelle")+ChatColor.YELLOW+")";
					if (pCtx.playerData.getName() != null)
						data += ", nom : "+ChatColor.WHITE+pCtx.playerData.getName();
				}
				data += ChatColor.YELLOW+"\nSkin séléctionné : ";
				if (pCtx.getCurrentSkin()!=null) {
					data += pCtx.getCurrentSkin().getLabel();
				} else data += ChatColor.GRAY+"aucun";
				sender.sendMessage(data);
				return true;
			}
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED+"Commande uniquement disponible pour les joueurs !");
				return true;
			}
			Player p = (Player) sender;
			String uuid = p.getUniqueId().toString().replaceAll("-","");
			if (args.length<1) {
				sender.sendMessage(ChatColor.YELLOW+"Interface web : "+ChatColor.WHITE+"https://aquilon-mc.fr/pjmanager/");
				String noSelectedChar = ChatColor.YELLOW+"Aucun personnage sélectionné\n" +
						"Pour en séléctionner un : "+ChatColor.WHITE+"/perso select <nom>";
				// Infos sur l'état actuel : Personnage + skin
				PlayerContext pCtx = PlayerContext.getFromPlayer(p, charDB);
				if (pCtx.character == null) {
					sender.sendMessage(noSelectedChar);
				} else {
                    sender.sendMessage(ChatColor.YELLOW+"Personnage "+
                            (pCtx.character instanceof TempCharacter ? "temporaire" : "sélectionné")+" : "+
                            ChatColor.WHITE+pCtx.character.getName()+ChatColor.YELLOW +
                            " ("+ChatColor.GRAY+pCtx.character.getRace()+ChatColor.YELLOW+")" +
                            (pCtx.playerData.getName() != null ? " avec le nom "+ChatColor.WHITE+pCtx.playerData.getName() : "")
                    );
				}
				String skin;
				if (pCtx.getCurrentSkin()!=null) {
					skin = pCtx.getCurrentSkin().getLabel();
                } else skin = ChatColor.GRAY+"aucun";
                sender.sendMessage(ChatColor.YELLOW+"Skin sélectionné : " + skin);
			} else if (args[0].equalsIgnoreCase("select")) {
				// Séléctionne un personnage
				if (args.length<2) {
					sender.sendMessage(ChatColor.YELLOW+"Usage : "+ChatColor.WHITE+
                            "/perso select <nom_perso> (/ <nom_custom>)");
					return true;
				}
				Character c = null;
				String customName = null;
                if (args[1].equals("shared") || args[1].equals("partagé")) { // Perso partagé
                	if (args.length<4) {
						sender.sendMessage(ChatColor.YELLOW+"Usage : "+ChatColor.WHITE+
								"/perso select shared <catégorie> <nom_perso> (/ <nom_custom>)");
						return true;
					}
                    String category = args[2];
                    if (!p.hasPermission(PERM_SELECT_CHARACTER.replace("{{CATEGORY}}", category))) {
                        sender.sendMessage(ChatColor.YELLOW+"Vous n'avez pas accès à cette catégorie !");
                        return true;
                    }
                    String[] names = Utils.joinStrings(args, " ", 3).split("/",2);
					if (names[0].length()<2) {
						sender.sendMessage(ChatColor.YELLOW+"Nom trop court, minimum 2 lettres !");
						return true;
					}
					String nameQuery = names[0].trim().replaceAll("%","[%]");
                    List<Character> sharedChars = charDB.findSharedCharactersWithName(category, nameQuery);
                    if (sharedChars.size()>1) {
                        sender.sendMessage(ChatColor.YELLOW+"Plusieurs personnages trouvés avec ces paramètres.");
                        return true;
                    } else if (sharedChars.size()==1) {
                        c = sharedChars.get(0);
                    }
                    if (names.length>1) customName = names[1].trim();
                } else try { // Par ID
                    int charID = Integer.parseUnsignedInt(args[1]);
                    if (charID == 0) {
                        sender.sendMessage(ChatColor.YELLOW+"Euh, non ! C'est pas zéro, c'est Zorro !");
                        return true;
                    }
                    c = charDB.findCharacterByID(charID);
                    if (args.length>2) customName = Utils.joinStrings(args, " ", 2);
                } catch (NumberFormatException ignored) { // Perso joueur, par nom
                    String[] names = Utils.joinStrings(args, " ", 1).split("/",2);
                    if (names[0].length()<2) {
                        sender.sendMessage(ChatColor.YELLOW+"Nom trop court, minimum 2 lettres !");
                        return true;
                    }
                    String nameQuery = names[0].trim().replaceAll("%","[%]");
                    c = charDB.findPlayerCharacterFromName(uuid, nameQuery);
                    if (names.length>1) customName = names[1].trim();
                }
				if (c==null) {
					sender.sendMessage(ChatColor.YELLOW+"Aucun personnage trouvé avec ces paramétres !");
					return true;
				}
				if (c.getPlayerUUID()==null) {
					if (!p.hasPermission(PERM_SELECT_CHARACTER.replace("{{CATEGORY}}", c.getCategory()))) {
						sender.sendMessage(ChatColor.YELLOW+"Vous n'avez pas la permission d'utiliser ce personnage !");
						return true;
					}
				} else {
					if (!(
							p.hasPermission(PERM_SELECT_ANY_CHARACTER) ||
							c.getPlayerUUIDObject().equals(p.getUniqueId())
						)) {
						sender.sendMessage(ChatColor.YELLOW+"Vous n'avez pas la permission d'utiliser ce personnage !");
						return true;
					}
				}
				if (c.getStatus() != Character.Status.ACTIVATED && !p.hasPermission(PERM_SELECT_ANY_CHARACTER)) {
                    sender.sendMessage(ChatColor.YELLOW+"Ce personnage n'a pas été validé, " +
                            "vous ne pouvez pas encore le jouer.");
                    return true;
                }
				List<CharacterSkin> skins = charDB.findCharacterSkins(c.getID());
				CharacterSkin selectSkin = null;
				if (skins.size()>0) selectSkin = skins.get(0);
				if (selectCharacter(p, c, customName, selectSkin, null)) {
					sender.sendMessage(ChatColor.YELLOW+"Sélection du personnage " +
							ChatColor.WHITE+c.getName()+ChatColor.YELLOW +
							" ("+ChatColor.GRAY+c.getRace()+ChatColor.YELLOW+")" +
							(customName!=null ? " avec le nom "+ChatColor.WHITE+customName : "")
					);
				} else {
					sender.sendMessage(ChatColor.YELLOW+"Erreur lors de la sélection du personnage " +
							ChatColor.WHITE+c.getName()+ChatColor.YELLOW+" !");
				}
			} else if (args[0].equalsIgnoreCase("skin")) {
				// Séléctionne un skin
				if (args.length<2) {
					sender.sendMessage(ChatColor.YELLOW+"Usage : "+ChatColor.WHITE+"/perso skin [list | common | <nom_skin>]");
					return true;
				}
                if (args[1].equals("common") || args[1].equals("commun") || args[1].equals("shared") || args[1].equals("partagé")) {
				    if (args.length<4) {
                        sender.sendMessage(ChatColor.YELLOW+"Usage : "+ChatColor.WHITE+"/perso skin common <categorie> <nom_skin>");
                        return true;
                    }
				    String cat = args[2];
                    if (!sender.hasPermission(PERM_SELECT_SKIN.replace("{{CATEGORY}}",cat))){
                        sender.sendMessage(ChatColor.YELLOW+"Vous n'avez pas accès à cette catégorie");
                        return true;
                    }
                    String skinName = Utils.joinStrings(args, " ", 3);
                    CommonSkin skin = charDB.findCommonSkinWithNames(cat, skinName);
                    if (skin==null) {
                        sender.sendMessage(ChatColor.YELLOW+"Aucun skin trouvé avec ce nom dans cette catégorie");
                        return true;
                    }
                    if (selectCommonSkin(p, skin)) {
                        sender.sendMessage(ChatColor.YELLOW+"Skin commun défini : "+ChatColor.WHITE+skin.getName());
                    } else {
                        sender.sendMessage(ChatColor.YELLOW+"Erreur lors de la sélection du skin commun "+ChatColor.WHITE+skin.getName()+" !");
                    }
                    return true;
                }
				CharacterPlayer player = charDB.findPlayer(uuid);
				if (player==null || player.getSelectedCharacter()==0) {
					sender.sendMessage(ChatColor.YELLOW+"Sélectionnez un personnage avant de choisir un skin.");
					return true;
				} else {
                    if (args[1].equals("list")) {
                        List<CharacterSkin> skins = charDB.findCharacterSkins(player.getSelectedCharacter());
                        if (skins.size()<1) {
                            sender.sendMessage(ChatColor.YELLOW+"Aucun skin disponible pour ce personnage.");
                            return true;
                        }
                        String list = ChatColor.WHITE+skins.stream()
                                .map(CharacterSkin::getName)
                                .collect(Collectors.joining(ChatColor.YELLOW+", "+ChatColor.WHITE));
                        sender.sendMessage(ChatColor.YELLOW+"Skin disponibles pour ce personnage :\n"+list);
                        return true;
                    }
                    String skinName = Utils.joinStrings(args, " ", 1);
					CharacterSkin skin = charDB.findCharacterSkin(player.getSelectedCharacter(), skinName);
					if (skin==null) {
						sender.sendMessage(ChatColor.YELLOW+"Aucun skin avec ce nom pour ce personnage !");
						return true;
					}
					if (selectSkin(p, skin)) {
						sender.sendMessage(ChatColor.YELLOW+"Sélection du skin "+ChatColor.WHITE+skin.getName());
					} else {
						sender.sendMessage(ChatColor.YELLOW+"Erreur lors de la sélection du skin "+ChatColor.WHITE+skin.getName()+" !");
					}
				}
			} else if (args[0].equalsIgnoreCase("temp") || args[0].equalsIgnoreCase("tempo")) {
				// Créé et séléctionne un personnage temporaire
                String usage = "/perso tempo <RACE> <SEXE> <NOM>";
                if (args.length<4) {
                    sender.sendMessage(ChatColor.YELLOW+"Usage : "+ChatColor.WHITE+usage);
                    return true;
                }
                if (!sender.hasPermission(PERM_CREATE_TEMP_CHAR)) {
					sender.sendMessage(ChatColor.YELLOW+"Vous n'avez pas le droit de créer de personnage temporaire.");
					return true;
				}
                String raceName = args[1].equals("h") ? "humain" :
						args[1].equals("n") ? "nain" :
						args[1].equals("g") ? "gobelin" :
						args[1].equals("o") ? "orc" :
						args[1].equals("e") ? "elfe" : args[1];
                CharRace race = charDB.findRaceFromName(raceName);
                if (race==null || !race.userCanUse(sender)) {
					Map<String,List<CharRace>> raceList = charDB.findRaces();
					List<String> catRaces  = new ArrayList<>();
					for (String cat : raceList.keySet()) {
						if (!CharRace.userCanUse(sender, cat)) continue;
						catRaces.add(raceList.get(cat).stream()
								.map(CharRace::getName)
								.collect(Collectors.joining(ChatColor.YELLOW+", "+ChatColor.WHITE)));
					}
                    sender.sendMessage(ChatColor.YELLOW+"Race invalide, options :\n"+ChatColor.WHITE+catRaces.stream()
							.collect(Collectors.joining(ChatColor.YELLOW+", "+ChatColor.WHITE)));
                    return true;
                }
                String sex = args[2].toUpperCase();
                if (!sex.equals("M") && !sex.equals("F")) {
                    sender.sendMessage(ChatColor.YELLOW+"Le sexe doit être defini comme " +
                            ChatColor.WHITE+"M"+ChatColor.YELLOW+" ou "+ChatColor.WHITE+"F");
                    return true;
                }
                String name = Utils.joinStrings(args, " ", 3);
                AbstractCharacter c = new TempCharacter(uuid)
                        .setName(name)
                        .setRace(race.getName())
                        .setSex(sex.charAt(0))
						.setHeight(race.getMeanHeight());
                if (!charDB.putTempCharacter((TempCharacter) c)) {
                    sender.sendMessage(ChatColor.YELLOW+"Erreur lors de la création du personnage temporaire !");
                    return true;
                }
                if (selectTempCharacter(p, (TempCharacter) c, null, null)) {
                    sender.sendMessage(ChatColor.YELLOW+"Sélection du personnage " +
                            ChatColor.WHITE+c.getName()+ChatColor.YELLOW +
                            " ("+ChatColor.GRAY+c.getRace()+ChatColor.YELLOW+")"
                    );
                } else {
                    sender.sendMessage(ChatColor.YELLOW+"Erreur lors de la sélection du personnage !");
                }
			} else if (args[0].equals("name")) {
                if (args.length<2) {
                    sender.sendMessage(ChatColor.YELLOW+"Usage : "+ChatColor.WHITE+"/perso name [reset | <nom>]");
                    return true;
                }
                String name = Utils.joinStrings(args, " ", 1);
                if (name.equals("reset")) name = null;
                if (!charDB.setPlayerCustomName(p, name)) {
                    sender.sendMessage(ChatColor.YELLOW+"Erreur lors de la définition du nom !");
                    return true;
                }
                sendUpdatePackets(p);
                sender.sendMessage(ChatColor.YELLOW+"Nom défini : "+ChatColor.WHITE+name);
            } else if (args[0].equals("scale")) {
                if (!sender.hasPermission(PERM_SCALE)) {
                    sender.sendMessage(ChatColor.YELLOW+"Oust ! Touche pas à ça !");
                    return true;
                }
                if (args.length<2) {
                    sender.sendMessage(ChatColor.YELLOW+"Usage : "+ChatColor.WHITE+"/perso scale <taille> [<joueur>]");
                    return true;
                }
				float scale;
                if (args[1].equals("reset")) {
                	scale = 0f;
				} else {
					try {
						scale = Float.parseFloat(args[1]);
						if (scale < 0f) throw new NumberFormatException("Invalid number");
					} catch (NumberFormatException e) {
						sender.sendMessage(ChatColor.YELLOW+"Un nombre positif, c'est pas compliqué quand même !");
						return true;
					}
				}
				Player target = p;
                if (args.length>2) {
                	target = Bukkit.getPlayer(args[2]);
                	if (target==null) {
						sender.sendMessage(ChatColor.YELLOW+"Aucun joueur avec ce nom !");
						return true;
					}
				}
				String targetUUID = target.getUniqueId().toString().replaceAll("-","");
				if (scale==0) {
                	scale = 1f;
                	playerScales.remove(targetUUID);
				} else {
					playerScales.put(targetUUID, scale);
				}
                for (Player player : Bukkit.getOnlinePlayers()) {
                    sendScale(player, targetUUID, scale);
                }
                sender.sendMessage(ChatColor.YELLOW+"Scale défini : "+ChatColor.WHITE+args[1]);
            } else if (args[0].equals("reset")) {
                if (!charDB.unsetPlayerCharacter(p)) {
                    sender.sendMessage(ChatColor.YELLOW+"Erreur lors de la déselection du personnage !");
                    return true;
                }
                sendUpdatePackets(p, null, null, null, null);
                sender.sendMessage(ChatColor.YELLOW+"Personnage déselectionné.");
            } else return false;
			return true;
		}
		return false;
	}

    public boolean selectCharacter(Player player, Character character, String customName, CharacterSkin skin, CommonSkin commonSkin) {
        if (skin != null && skin.getCharacterID() != character.getID())
            throw new IllegalArgumentException("Invalid skin !");
        if (!charDB.setPlayerCharacter(player, character, customName, skin, commonSkin))
            return false;
        String uuid = player.getUniqueId().toString().replaceAll("-","");
        if (!charDB.deleteTempCharacter(uuid))
        	LOGGER.mWarning("Couldn't delete temp character for player "+uuid);
        String selectedSkin = null;
        if (skin!=null) selectedSkin = skin.getFileName();
        else if (commonSkin!=null) selectedSkin = commonSkin.getFileName();
        List<CharacterSkill> skills = charDB.findCharacterSkills(character.getID());
        sendUpdatePackets(player, customName, character, selectedSkin, skills);
        return true;
    }

    public boolean selectTempCharacter(Player player, TempCharacter character, String customName, CommonSkin skin) {
        if (!charDB.setPlayerCharacterTemp(player, customName, skin))
            return false;
        sendUpdatePackets(player, customName, character, skin!=null ? skin.getFileName() : null, null);
        return true;
    }

    public boolean selectSkin(Player player, CharacterSkin skin) {
        if (!charDB.setPlayerSkin(player, skin)) return false;
        String uuid = player.getUniqueId().toString().replaceAll("-","");
        byte[] skinData = (uuid+":"+skin.getFileName()).getBytes();
        for (Player p : Bukkit.getOnlinePlayers()) {
			AquilonThings.sendPluginMessage(p, CHANNEL_SKIN, skinData);
        }
        AquilonThings.sendServerMessage(CHANNEL_SKIN, skinData);
        return true;
    }

    public boolean selectCommonSkin(Player player, CommonSkin skin) {
        if (!charDB.setPlayerCommonSkin(player, skin, true)) return false;
        PlayerContext ctx = PlayerContext.getFromPlayer(player, charDB);
        for (Player target : Bukkit.getOnlinePlayers())
            sendPlayerPackets(target, ctx);
        sendServerPacket(ctx);
        return true;
    }

    public void sendScale(Player target, String pUUID, float scale) {
		AquilonThings.sendPluginMessage(target, CHANNEL_SCALE,
				(pUUID+":"+scale).getBytes());
	}

    public void sendUpdatePackets(Player p) {
	    PlayerContext ctx = PlayerContext.getFromPlayer(p, charDB);
	    sendUpdatePackets(p, ctx.getCustomName(), ctx.character, ctx.getSkinFile(), ctx.skills);
    }

	public void sendUpdatePackets(Player player, String customName, AbstractCharacter character, String skin, List<CharacterSkill> skills) {
	    String name = customName!=null ? customName : (character!=null ? character.getName() : player.getName());
        String race = character!=null ? character.getRace() : "humain"; // TODO: load default from config
        String uuid = player.getUniqueId().toString().replaceAll("-","");
        player.setDisplayName(name);
        byte[] nameData = (uuid+":"+name).getBytes();
        byte[] raceData = (uuid+":"+race).getBytes();
        byte[] skinData = (uuid+":"+(skin!=null ? skin : "skin_mojang")).getBytes();
        byte[] skillsData = (uuid+";"+(skills!=null ? PlayerContext.getSkillsString(skills) : "")).getBytes();
		for (Player p : Bukkit.getOnlinePlayers()) {
			AquilonThings.sendPluginMessage(p, CHANNEL_NAME, nameData);
			AquilonThings.sendPluginMessage(p, CHANNEL_RACE, raceData);
			AquilonThings.sendPluginMessage(p, CHANNEL_SKIN, skinData);
			AquilonThings.sendPluginMessage(p, CHANNEL_SKILLS, skillsData);
        }
		AquilonThings.sendServerMessage(CHANNEL_NAME, nameData);
		AquilonThings.sendServerMessage(CHANNEL_RACE, raceData);
		AquilonThings.sendServerMessage(CHANNEL_SKIN, skinData);
		AquilonThings.sendServerMessage(CHANNEL_SKILLS, skillsData);
    }

	public void sendPlayerPackets(Player target, PlayerContext context) {
		AquilonThings.sendPluginMessage(target, CHANNEL_NAME, context.getNameData());

		byte[] raceData = context.getRaceData("humain"); // TODO: load default from config
		AquilonThings.sendPluginMessage(target, CHANNEL_RACE, raceData);

		byte[] skinData = context.getSkinData("skin_mojang");
		AquilonThings.sendPluginMessage(target, CHANNEL_SKIN, skinData);

		byte[] skillsData = context.getSkillsData();
		AquilonThings.sendPluginMessage(target, CHANNEL_SKILLS, skillsData);
	}

	public void sendServerPacket(PlayerContext context) {
		AquilonThings.sendServerMessage(CHANNEL_NAME, context.getNameData());

		byte[] raceData = context.getRaceData("humain"); // TODO: load default from config
		AquilonThings.sendServerMessage(CHANNEL_RACE, raceData);

		byte[] skinData = context.getSkinData("skin_mojang");
		AquilonThings.sendServerMessage(CHANNEL_SKIN, skinData);

		byte[] skillsData = context.getSkillsData();
		AquilonThings.sendServerMessage(CHANNEL_SKILLS, skillsData);
	}

	public CharacterDatabase getCharacterDB() {
		return charDB;
	}
}
