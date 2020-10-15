package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers;

import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.Place;
import fr.aquilon.minecraft.aquilonthings.utils.Utils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TriggerCommandCyclic extends TriggerCommand {
	public int counter = 0;
    protected ArrayList<Player> activePlayerlist = new ArrayList<>();

    protected boolean discriminate; // One random player (true) or every players (false)
	public int interval; // seconds
	public int interval_variation; // seconds
		
	public TriggerCommandCyclic(int id, Place p, String name, boolean state, ModuleLogger log) {
		super(id, p, name, state, log);
		TriggerCommandTickControl.getInstance().registerTriggerCommand(this);
	}
	
	public TriggerCommandCyclic(int id, Place p, String name, boolean state, ModuleLogger log, boolean discriminate, String command, int interval, int interval_variation) {
		super(id, p, name, state, log, command);
        this.discriminate = discriminate;
		this.interval = interval;
		this.interval_variation = interval_variation;
		TriggerCommandTickControl.getInstance().registerTriggerCommand(this);
	}

	@Override
	public boolean trigger(Player p, boolean goingIn) {
        if(goingIn){
            if(!activePlayerlist.contains(p))
                activePlayerlist.add(p);
        } else {
            activePlayerlist.remove(p);
        }
		return true;
	}
	
	public void resetCounter(){
		if(counter > 0)
			return;
		int variation_random = ThreadLocalRandom.current().nextInt(-interval_variation, interval_variation + 1);
		counter = (interval + variation_random);
		getLogger().mInfo("Reset de "+getName()+" avec la valeur de: " + counter + "("+interval+" + "+variation_random+").");
	}

	@Override
	public String getInfos() {
		return super.getInfos()+"\nDiscriminate: "+discriminate+
                "\nIntervale: "+interval+" (+/- "+interval_variation+")";
	}

	@Override
	public Object getField(String field) {
		try {
			return super.getField(field);
		} catch (IllegalArgumentException e) {
			Field f = Field.valueOf(field);
			switch (f) {
				case DISCRIMINATE:
					return this.discriminate;
				case INTERVAL:
					return this.interval;
				case INTERVAL_VAR:
					return this.interval_variation;
				default:
					throw new IllegalArgumentException("Unsupported field");
			}
		}
	}

	@Override
	public void setField(String field, Object value) {
		try {
			super.setField(field, value);
		} catch (IllegalArgumentException e) {
			Field f = Field.valueOf(field);
			switch (f) {
				case DISCRIMINATE:
					this.discriminate = Boolean.valueOf(value.toString());
					break;
				case INTERVAL:
					this.interval = Integer.valueOf(value.toString());
					break;
				case INTERVAL_VAR:
					this.interval_variation = Integer.valueOf(value.toString());
					break;
				default:
					throw new IllegalArgumentException("Unsupported field");
			}
		}
	}

	@Override
	public String[] getFields() {
		return Utils.mergeArrays(super.getFields(), Field.names());
	}

	public enum Field {
        DISCRIMINATE,
		INTERVAL,
		INTERVAL_VAR;

		public static String[] names() {
			List<Field> l = Arrays.asList(values());
			return l.stream().map(Field::name).toArray(String[]::new);
		}
	}

}
