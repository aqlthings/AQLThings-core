package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places;

import fr.aquilon.minecraft.aquilonthings.module.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.utils.DatabaseConnector;

public class PlaceFactory {

	/**
	 * @param db The database connector
	 * @param id The place id
	 * @param type The kind of place
	 * @param name The name of the place
	 * @param world The world this place is in
	 * @param log A logger to send logs to
	 * @return A place with the requested type.
	 */
	public static Place getPlace(DatabaseConnector db, int id, String type, String name, String world, ModuleLogger log) {
	    try {
			Place p;
			switch (PlaceTypeEnum.valueOf(type)){
				case RADIUS:
					p = new PlaceRadius(id, name, world);
					break;
				case CUBE:
					p = new PlaceCube(id, name, world);
					break;
				default:
					log.mWarning("Unknown place type '"+type+"' for place "+id+" !");
					return null;
			}
			p.retrieveParamsFromDB(db, log);
			return p;
		} catch (Exception e) {
			log.mWarning("Error while retrieving place "+id+" > "+e.getClass().getName()+": "+e.getMessage());
		}
		return null;
	}

}
