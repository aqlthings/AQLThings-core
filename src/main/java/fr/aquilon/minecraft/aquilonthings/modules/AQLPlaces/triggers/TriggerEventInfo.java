package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers;

import fr.aquilon.minecraft.aquilonthings.module.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.AQLPlaces;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.Place;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class TriggerEventInfo extends Trigger {

	private static final String STRING_NA = "non précisé";
	
	public String event_name;
	public String event_description;
	public String event_url;
	
	public TriggerEventInfo(int id, Place p, String name, boolean state, ModuleLogger log) {
		super(id, p, name, state, log);
	}

	public TriggerEventInfo(int id, Place p, String name, boolean state, ModuleLogger log, String event_name, String event_description, String event_url) {
		super(id, p, name, state, log);
		this.event_name = event_name;
		this.event_description = event_description;
		this.event_url = event_url;	
	}

	@Override
	public boolean trigger(Player p, boolean goingIn) throws TriggerFailedException {
		// TODO: faire le système de cooldown (hasharray avec nom comme clef + heure au bon format)
		if(!goingIn) return false; 
		try{
			if(p.hasPermission(AQLPlaces.PERM_EVENT_INFO)){
				p.sendMessage(ChatColor.YELLOW + "Zone d'event: " + ChatColor.WHITE + this.event_name);
				if(this.event_description.equals(""))
					p.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + STRING_NA);
				else
					p.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.WHITE + this.event_description);
							
				if(this.event_url.equals(""))
					p.sendMessage(ChatColor.YELLOW + "Documentation: " + ChatColor.WHITE + STRING_NA);
				else
					p.sendMessage(ChatColor.YELLOW + "Documentation: " + ChatColor.WHITE + this.event_url);
				
				return true;
			}
		} catch(Exception e){
			throw new TriggerFailedException(this, e);
		}
		return false;
	}

    @Override
    public String getInfos() {
        return super.getInfos()+"\nNom: "+event_name+"\nURL: "+event_url+"\nDescription: "+event_description;
    }

    @Override
	public Object getField(String field) {
		Field f = Field.valueOf(field);
		switch (f) {
			case EVENT_NAME:
				return this.event_name;
			case EVENT_DESC:
				return this.event_description;
			case EVENT_URL:
				return this.event_url;
			default:
				throw new IllegalArgumentException("Unsupported field");
		}
	}

	@Override
	public void setField(String field, Object value) {
		Field f = Field.valueOf(field);
		switch (f) {
			case EVENT_NAME:
				this.event_name = value.toString();
                break;
			case EVENT_DESC:
				this.event_description = value.toString();
                break;
			case EVENT_URL:
				this.event_url = value.toString();
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
		EVENT_NAME,
		EVENT_DESC,
		EVENT_URL;

		public static String[] names() {
			List<Field> l = Arrays.asList(values());
			return l.stream().map(Field::name).toArray(String[]::new);
		}
	}
}
