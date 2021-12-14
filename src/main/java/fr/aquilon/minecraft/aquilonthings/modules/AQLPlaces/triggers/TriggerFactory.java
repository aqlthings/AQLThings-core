package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers;

import fr.aquilon.minecraft.aquilonthings.module.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places.Place;
import fr.aquilon.minecraft.aquilonthings.utils.DatabaseConnector;

public class TriggerFactory {

	/**
	 * Retourne l'objet trigger correspondant au type demandé.
	 * @param db The database connector
	 * @param id The id of the trigger
	 * @param type The kind of trigger
	 * @param p The place this trigger is attached to
	 * @param name The name of the trigger
	 * @param state Whether this trigger is enabled or not
	 * @param log A logger to send logs to
	 * @return A trigger with the requested type
	 */
	public Trigger getTrigger(DatabaseConnector db, int id, String type, Place p, String name, boolean state, ModuleLogger log) {
		try {
			Trigger trg;
			switch(TriggerTypeEnum.valueOf(type)){
				case LOCALISATION:
					trg = new TriggerLocalisation(id, p, name, state, log);
					break;
				case COMMAND:
					trg = new TriggerCommand(id, p, name, state, log);
					break;
				case EXIT_COMMAND:
					trg = new TriggerExitCommand(id, p, name, state, log);
					break;
				case COMMAND_CYCLIC:
					trg = new TriggerCommandCyclic(id, p, name, state, log);
					break;
				case EVENT_INFO:
					trg = new TriggerEventInfo(id, p, name, state, log);
					break;
				default:
					log.mWarning("Unknown trigger type '"+type+"' for trigger "+id+" !");
					return null;
			}
			trg.retrieveParamsFromDB(db);
			return trg;
		} catch (Exception e) {
			log.mWarning("Error while retrieving trigger "+id+" > "+e.getClass().getName()+": "+e.getMessage());
		}
		return null;
	}
	
}
