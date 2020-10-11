package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers;

import java.util.Arrays;
import java.util.List;

public enum TriggerTypeEnum {
	LOCALISATION(TriggerLocalisation.class),
	COMMAND(TriggerCommand.class),
	EXIT_COMMAND(TriggerExitCommand.class),
	COMMAND_CYCLIC(TriggerCommandCyclic.class),
	EVENT_INFO(TriggerEventInfo.class);

	private Class<? extends Trigger> triggerClass;

	TriggerTypeEnum(Class<? extends Trigger> triggerClass) {
		this.triggerClass = triggerClass;
	}

	public Class<? extends Trigger> getTriggerClass() {
		return triggerClass;
	}

	/**
	 * Retourne le type correspondant à chaque val de l'enum
	 */
	@Override
	public String toString(){
		return name();
	}

	public static TriggerTypeEnum fromClass(Class<? extends Trigger> triggerClass) {
		return fromClassName(triggerClass.getSimpleName());
	}

	public static TriggerTypeEnum fromClassName(String classN) {
		for (TriggerTypeEnum t : values()) {
			if (t.getTriggerClass().getSimpleName().equals(classN)) return t;
		}
		return null;
	}

    public static String[] names() {
        List<TriggerTypeEnum> l = Arrays.asList(values());
        return l.stream().map(TriggerTypeEnum::name).toArray(String[]::new);
    }
	
}