package fr.aquilon.minecraft.aquilonthings.modules.AQLLooting;

import java.util.List;

import org.bukkit.entity.EntityType;

public class MobLootInfo {
	
	private EntityType type;
	private List<ProbabilityLoot> probabilityLootList;
	
	public MobLootInfo (EntityType type, List<ProbabilityLoot> probabilityLootList) {
		this.type = type;
		this.probabilityLootList = probabilityLootList;
	}
	
	//--------------------------------------------------------------

	public EntityType getType() {
		return type;
	}

	public void setType(EntityType type) {
		this.type = type;
	}

	public List<ProbabilityLoot> getProbabilityLootList() {
		return probabilityLootList;
	}

	public void setProbabilityLootList(List<ProbabilityLoot> probabilityLootList) {
		this.probabilityLootList = probabilityLootList;
	}
	
}
