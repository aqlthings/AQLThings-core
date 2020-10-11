package fr.aquilon.minecraft.aquilonthings;

public enum GenericStateEnum {
	FAILED, OK, UNKOWN;
	
	public boolean toBool(){
		return this != GenericStateEnum.FAILED;
	}
}


