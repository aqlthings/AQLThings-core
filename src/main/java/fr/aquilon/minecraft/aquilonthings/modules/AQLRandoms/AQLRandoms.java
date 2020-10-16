package fr.aquilon.minecraft.aquilonthings.modules.AQLRandoms;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.AQLCharacters;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.CharacterDatabase;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterPlayer;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.CharacterSkill;
import fr.aquilon.minecraft.aquilonthings.modules.AQLCharacters.model.Skill;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * An AQLThings module providing a command to roll dices with bonuses/maluses
 * Note: This module depends on AQLCharacters
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
@AQLThingsModule(
		name = "AQLRandoms",
		cmds = {
				@Cmd(value = AQLRandoms.CMD_RANDOM, desc = "Tire un nombre au hasard")
		}
)
public class AQLRandoms implements IModule {
	public static final ModuleLogger LOGGER = ModuleLogger.get();

	public static final String CMD_RANDOM = "random";

	public static final String PERM_RANDOM_SEE_SECRET = AquilonThings.PERM_ROOT+".random.secret";

	@Override
	public boolean onStartUp(DatabaseConnector db) {
		return true;
	}

	@Override
	public boolean onStop() {
		return true;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String scmd, String[] rawArgs) {
		if(!scmd.equalsIgnoreCase(CMD_RANDOM)) return false;
		if (!(sender instanceof Player)) {
			sender.sendMessage("Commande joueur uniqument.");
			return true;
		}
		String params = Utils.joinStrings(rawArgs, " ");
		Player p = ((Player) sender);
		String uuid = p.getUniqueId().toString().replaceAll("-","");
		int max = 20;
		int customBonus = 0;
		boolean priv = false;
		CharacterSkill charSkill = null;
		String usage = ChatColor.YELLOW+"Syntaxe invalide !\nExemple: "+
				ChatColor.WHITE+"/random 50 + 6 - 4 + 2 secret"+ChatColor.YELLOW+
				" ou "+ChatColor.WHITE+"/random MED +1 privé";
		String args = params.toLowerCase().trim();
		if (args.length()>0 && (args.contains("privé") || args.contains("secret"))) {
			priv = true;
			args = args.replaceAll("privé","").replaceAll("secret","").trim();
		}
		if (args.length()>0 && !args.startsWith("+") && !args.startsWith("-")) {
			char firstChar = args.charAt(0);
			if (Character.isLetter(firstChar)) {
				AQLCharacters characters = AquilonThings.instance.getModuleData(AQLCharacters.class);
				if (characters == null) {
					sender.sendMessage(ChatColor.YELLOW+"Valeur invalide !\n"+ChatColor.WHITE+usage);
					return true;
				}
				CharacterDatabase charDB = characters.getCharacterDB();
				Skill skill =  charDB.findSkillFromShorthand(args.substring(0,3));
				args = args.substring(3).trim();
				if (skill==null) {
					sender.sendMessage(ChatColor.YELLOW+"Compétence inconnue !");
					return true;
				}
				CharacterPlayer charP = charDB.findPlayer(uuid);
				int charid = charP == null ? 0 : charP.getSelectedCharacter();
				if (charid == 0) {
					sender.sendMessage(ChatColor.YELLOW+"Aucun personnage séléctionné !");
					return true;
				}
				charSkill = charDB.findCharacterSkillFromShorthand(charid, skill.getShorthand());
				if (charSkill == null)
					charSkill = new CharacterSkill(charid, skill.getCategory(), skill.getName())
							.setLevel(0).setCategoryUnlocked(false);
				if (charDB.findCharacterSkillCategory(charid, skill.getCategory()) != null)
					charSkill.setCategoryUnlocked(true);
			}
		}
		if (args.length()>0) {
			String[] arg_arr = args.replaceAll(" ","").split("(?=[\\+-])");
			for (String arg : arg_arr) {
				if (arg.startsWith("+") || arg.startsWith("-")) {
					try {
						customBonus += Integer.parseInt(arg);
					} catch (NumberFormatException e) {
						sender.sendMessage(usage);
						return true;
					}
				} else {
					try {
						max = Integer.parseInt(arg);
					} catch (NumberFormatException e) {
						sender.sendMessage(usage);
						return true;
					}
				}
			}
			if (max<1) {
				sender.sendMessage(ChatColor.YELLOW+"Valeur invalide !\n"+ChatColor.WHITE+usage);
				return true;
			}
		}

		AQLRandomEvent evt = new AQLRandomEvent(p, max, priv, customBonus, charSkill);
		evt.call(this);
		return true;
	}
}
