package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers;

import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.Place;
import org.bukkit.entity.Player;

public class TriggerExitCommand extends TriggerCommand {

	public TriggerExitCommand(int id, Place p, String name, boolean state, ModuleLogger log) {
		super(id, p, name, state, log);
	}

	public TriggerExitCommand(int id, Place p, String name, boolean state, ModuleLogger log, String command) {
		super(id, p, name, state, log, command);
	}

	@Override
	public boolean trigger(Player p, boolean goingIn) throws TriggerFailedException {
		if (goingIn) return false;
		runCommand(command, p);
		return true;
	}

}
