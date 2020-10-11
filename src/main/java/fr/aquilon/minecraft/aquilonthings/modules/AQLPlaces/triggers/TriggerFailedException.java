package fr.aquilon.minecraft.aquilonthings.modules.AQLPlaces.triggers;

public class TriggerFailedException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	public Trigger trigger;
	
	public TriggerFailedException(Trigger t, Throwable cause){
		super(cause);
		this.trigger = t;
	}
}
