package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.Place;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

import static fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.AQLPlaces.CHANNEL_PLACE;

public class TriggerLocalisation extends Trigger {
	
	public String locName;
	public String locDesc;
	public String locType;
	public int locDuration;
	
	public TriggerLocalisation(int id, Place p, String name, boolean state, ModuleLogger log){
		super(id, p, name, state, log);
	}
	
	public TriggerLocalisation(int id, Place p, String name, boolean state, ModuleLogger log, String locName, String locType, int locDuration, String locDesc) {
		super(id, p, name, state, log);
		this.locName = locName;
		this.locDesc = locDesc;
		this.locType = locType;
		this.locDuration = locDuration;
	}
	
	@Override
	public boolean trigger(Player p, boolean goingIn) throws TriggerFailedException {
		if(!goingIn) return false;
		try {
			String payload = locName + ":" + locType + ":" + locDuration + ":" + locDesc;
			AquilonThings.sendPluginMessage(p, CHANNEL_PLACE, payload.getBytes());
		} catch(Exception e){
			throw new TriggerFailedException(this,e);
		}
		return true;
	}

    @Override
    public String getInfos() {
        return super.getInfos()+"\nNom: "+locName+"\nType: "+locType+"\nDescription: "+locDesc;
    }

	@Override
	public Object getField(String field) {
		Field f = Field.valueOf(field);
		switch (f) {
			case LOCAL_NAME:
				return this.locName;
			case LOCAL_TYPE:
				return this.locType;
			case LOCAL_DESCRIPTION:
				return this.locDesc;
			case LOCAL_SHOW_SECONDS:
				return this.locDuration;
			default:
				throw new IllegalArgumentException("Unsupported field");
		}
	}

	@Override
	public void setField(String field, Object value) {
		Field f = Field.valueOf(field);
		switch (f) {
			case LOCAL_NAME:
				this.locName = value.toString();
				break;
			case LOCAL_TYPE:
				this.locType = value.toString();
                break;
			case LOCAL_DESCRIPTION:
				this.locDesc = value.toString();
                break;
			case LOCAL_SHOW_SECONDS:
				this.locDuration = Integer.parseInt(value.toString());
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
		LOCAL_NAME,
		LOCAL_TYPE,
		LOCAL_SHOW_SECONDS,
		LOCAL_DESCRIPTION;

		public static String[] names() {
			List<Field> l = Arrays.asList(values());
			return l.stream().map(Field::name).toArray(String[]::new);
		}
	}
}
