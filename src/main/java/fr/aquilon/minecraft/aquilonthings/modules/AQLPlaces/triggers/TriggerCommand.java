package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers;

import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.Place;
import fr.aquilon.minecraft.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class TriggerCommand extends Trigger {
	protected String command;
	
	public TriggerCommand(int id, Place p, String name, boolean state, ModuleLogger log) {
		this(id, p, name, state, log, "");
	}

	public TriggerCommand(int id, Place p, String name, boolean state, ModuleLogger log, String command) {
		super(id, p, name, state, log);
		this.command = command;
	}
	
	@Override
	public boolean trigger(Player p, boolean goingIn) throws TriggerFailedException {
		if (!goingIn) return false;
		runCommand(command, p);
		return true;
	}
	
	public void runCommand(String commandExt, Player p) throws TriggerFailedException{
		try{
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), parseReplacePattern(commandExt, p));
		} catch(Exception e){
			throw new TriggerFailedException(this,e);
		}
	}

	private String parseReplacePattern(String command, Player p) {
		// Remplacer le nom du joueur
		getLogger().mDebug("Executing: "+command + " => "+p.getName());
		command = command.replaceAll("\\{Player\\}", p.getName());
		// Remplacer le nom du monde sur lequel se trouve le joueur
		command = command.replaceAll("\\{PlayerWorld\\}", p.getWorld().getName());
		// Remplacer la couleur du joueur
		command = command.replaceAll("\\{PlayerColor\\}", Utils.getPlayerColor(p));
		// Remplacer positions
		command = command.replaceAll("\\{PlayerPosX\\}", Integer.toString(p.getLocation().getBlockX()));
		command = command.replaceAll("\\{PlayerPosY\\}", Integer.toString(p.getLocation().getBlockY()));
		command = command.replaceAll("\\{PlayerPosZ\\}", Integer.toString(p.getLocation().getBlockZ()));
		// Remplacements de la place
		command = getPlace().parseReplacePattern(command, p);
		return command;
	}

	@Override
	public String getInfos() {
		return super.getInfos()+"\nCommande: "+command;
	}

	@Override
	public Object getField(String field) {
		Field f = Field.valueOf(field);
		switch (f) {
			case COMMAND:
				return this.command;
			default:
				throw new IllegalArgumentException("Unsupported field");
		}
	}

	@Override
	public void setField(String field, Object value) {
		Field f = Field.valueOf(field);
		switch (f) {
			case COMMAND:
				this.command = value.toString();
                break;
			default:
				throw new IllegalArgumentException("Unsupported field");
		}
	}

	@Override
	public String[] getFields() {
		return Field.names();
	}

	public enum Field {
		COMMAND;

		public static String[] names() {
			List<Field> l = Arrays.asList(values());
			return l.stream().map(Field::name).toArray(String[]::new);
		}
	}
}
