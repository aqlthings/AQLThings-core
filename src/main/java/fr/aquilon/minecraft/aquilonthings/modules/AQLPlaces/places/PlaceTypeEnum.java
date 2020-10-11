package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.places;

import java.util.Arrays;
import java.util.List;

public enum PlaceTypeEnum {
	RADIUS(PlaceRadius.class),
	CUBE(PlaceCube.class);

	private Class<? extends Place> placeClass;

	PlaceTypeEnum(Class<? extends Place> placeClass) {
		this.placeClass = placeClass;
	}

	public String getType() {
		return name();
	}

	public Class<? extends Place> getPlaceClass() {
		return placeClass;
	}

	/**
	 * Retourne le type correspondant à chaque val de l'enum
	 */
	@Override
	public String toString(){
		return name();
	}

	// ----------- Static methods -----------

	public static PlaceTypeEnum fromClass(Class<? extends Place> placeClass) {
		return fromClassName(placeClass.getSimpleName());
	}

	public static PlaceTypeEnum fromClassName(String classN) {
		for (PlaceTypeEnum t : values()) {
			if (t.getPlaceClass().getSimpleName().equals(classN)) return t;
		}
		return null;
	}

	public static String[] names() {
		List<PlaceTypeEnum> l = Arrays.asList(values());
		return l.stream().map(PlaceTypeEnum::name).toArray(String[]::new);
	}
}