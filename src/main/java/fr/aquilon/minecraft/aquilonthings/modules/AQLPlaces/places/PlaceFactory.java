package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places;

import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;

public class PlaceFactory {

	/**
	 * Retourne l'objet place correspondant au type demandé.
	 * @param db
	 * @param id
	 * @param type
	 * @param name
	 * @param log
	 * @return place
	 */
	public static Place getPlace(DatabaseConnector db, int id, String type, String name, String world, ModuleLogger log){
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
