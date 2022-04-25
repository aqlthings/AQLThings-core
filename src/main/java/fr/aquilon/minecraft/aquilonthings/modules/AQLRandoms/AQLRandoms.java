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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An AQLThings module providing a command to roll dices with bonuses/maluses
 * Note: This module depends on AQLCharacters
 * @author BilliAlpha (billi.pamege.300@gmail.com)
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
		AQLCharacters characters = AquilonThings.instance.getModuleData(AQLCharacters.class);
		boolean withSkills = characters != null;
		int max = 20;
		String bonusString;
		boolean priv = false;
		CharacterSkill charSkill = null;
		String usage = ChatColor.YELLOW+"Exemple: "+
				ChatColor.WHITE+"/random 50 + 6 - 4 + 2 secret";
		if (withSkills)
				usage += ChatColor.YELLOW+" ou "+ChatColor.WHITE+"/random MED +1 privé";
		String args = params.toLowerCase().trim();
		if (args.length()>0 && (args.contains("privé") || args.contains("secret"))) {
			priv = true;
			args = args.replaceAll("privé","").replaceAll("secret","").trim();
		}

		List<ParamToken> tokens;
		try {
			tokens = splitTokens(args, withSkills);
		} catch (ParamParsingError e) {
			sender.sendMessage(ChatColor.YELLOW + "Syntaxe invalide: " +
					ChatColor.WHITE + e.params.substring(0, e.pos) +
					ChatColor.RED + e.params.charAt(e.pos) +
					ChatColor.WHITE + e.params.substring(e.pos + 1) +
					"\n>  " + ChatColor.YELLOW + e.msg);
			return true;
		}

		Optional<ParamToken> pMax = tokens.stream().filter(t -> t.type == ParamTokenType.MAX_NUM).findFirst();
		if (pMax.isPresent()) {
			try {
				max = Integer.parseUnsignedInt(pMax.get().value);
			} catch (NumberFormatException e) {
				sender.sendMessage(ChatColor.YELLOW + "Maximum non numérique !\n" + usage);
				return true;
			}
		}
		if (max < 1) {
			sender.sendMessage(ChatColor.YELLOW + "Maximum invalide !\n" + usage);
			return true;
		}

		Optional<ParamToken> pSkill = tokens.stream().filter(t -> t.type == ParamTokenType.SKILL).findFirst();
		if (withSkills && pSkill.isPresent()) {
			try {
				charSkill = findCharSkill(characters, uuid, pSkill.get().value);
			} catch (IllegalArgumentException e) {
				sender.sendMessage(ChatColor.YELLOW + e.getMessage() + "\n" + usage);
				return true;
			}
		}

		bonusString = tokens.stream()
				.filter(t -> t.type == ParamTokenType.MODIFIER)
				.map(ParamToken::getValue)
				.collect(Collectors.joining());

		AQLRandomEvent evt;
		try {
			evt = new AQLRandomEvent(p, max, priv, bonusString, charSkill);
		} catch (IllegalArgumentException ex) {
			sender.sendMessage(ChatColor.YELLOW + "Syntaxe invalide : " + ChatColor.WHITE + args + "\n" + usage);
			return true;
		}
		evt.call(this);
		return true;
	}

	public CharacterSkill findCharSkill(AQLCharacters characters, String uuid, String skillKey)
			throws IllegalArgumentException
	{
		CharacterDatabase charDB = characters.getCharacterDB();
		Skill skill =  charDB.findSkillFromShorthand(skillKey);
		if (skill==null) {
			throw new IllegalArgumentException("Unknown skill: "+skillKey);
		}
		CharacterPlayer charP = charDB.findPlayer(uuid);
		int charid = charP == null ? 0 : charP.getSelectedCharacter();
		if (charid == 0) {
			throw new IllegalArgumentException("No character selected");
		}
		CharacterSkill charSkill = charDB.findCharacterSkillFromShorthand(charid, skill.getShorthand());
		if (charSkill == null)
			charSkill = new CharacterSkill(charid, skill.getCategory(), skill.getName())
					.setLevel(0).setCategoryUnlocked(false);
		if (charDB.findCharacterSkillCategory(charid, skill.getCategory()) != null)
			charSkill.setCategoryUnlocked(true);
		return charSkill;
	}

	private static List<ParamToken> splitTokens(String params, boolean withSkills) throws ParamParsingError {
		List<ParamToken> res = new ArrayList<>();
		StringBuilder str = new StringBuilder();
		ParamTokenType prevType;
		ParamTokenType currType = ParamTokenType.NONE;
		boolean forceNext;
		boolean hasMax = false;
		boolean hasSkill = false;
		for (int i = 0; i < params.length(); i++) {
			forceNext = false;
			prevType = currType;
			char c = params.charAt(i);
			if (c == ' ') {
				continue; // Ignore spaces
				// FIXME: we should only ignore spaces around '+' and '-'
			} else if (c == '+' || c == '-') {
				currType = ParamTokenType.MODIFIER;
				forceNext = true; // New modifier
			} else if (prevType == ParamTokenType.MODIFIER && Character.isDigit(c)) {
				currType = ParamTokenType.MODIFIER; // Continue existing modifier
			} else if (prevType == ParamTokenType.NONE && Character.isDigit(c)) {
				if (hasMax)
					throw new ParamParsingError(i, params, "Multiple max nums");
				currType = ParamTokenType.MAX_NUM; // New max num
				hasMax = true;
			} else if (prevType == ParamTokenType.MAX_NUM && Character.isDigit(c)) {
				currType = ParamTokenType.MAX_NUM; // Continue existing max num
			} else if (withSkills) {
				if (prevType != ParamTokenType.SKILL && hasSkill)
					throw new ParamParsingError(i, params, "Multiple skill names");
				currType = ParamTokenType.SKILL; // letter => skill
				hasSkill = true;
			} else {
				throw new ParamParsingError(i, params, "Unexpected symbol");
			}
			if (forceNext || prevType != currType) { // End of token
				res.add(new ParamToken(str.toString(), prevType));
				str = new StringBuilder();
			}
			str.append(c);
		}
		res.add(new ParamToken(str.toString(), currType));
		res = res.stream()
				.filter(p -> p.type != ParamTokenType.NONE)
				.collect(Collectors.toList());

		return res;
	}

	private enum ParamTokenType {
		NONE,
		MODIFIER,
		SKILL,
		MAX_NUM
	}

	private static class ParamToken {
		public final String value;
		public final ParamTokenType type;

		private ParamToken(String value, ParamTokenType type) {
			this.value = value;
			this.type = type;
		}

		public String getValue() {
			return value;
		}

		public ParamTokenType getType() {
			return type;
		}
	}

	private static class ParamParsingError extends Exception {
		public final int pos;
		public final String params;
		private final String msg;

		private ParamParsingError(int pos, String params, String msg) {
			this.pos = pos;
			this.params = params;
			this.msg = msg;
		}

		@Override
		public String getMessage() {
			return "Invalid parameters: "+msg+" at position "+pos+": "+params;
		}
	}
}
