package fr.aquilon.minecraft.aquilonthings.modules.AQLLooting;

import org.bukkit.inventory.ItemStack;

public class ProbabilityLoot {
	
	private ItemStack itemStack;
	private float ratio;
	
	public ProbabilityLoot (ItemStack itemStack, float ratio) {
		this.itemStack = itemStack;
		this.ratio = ratio;
	}
	
	//--------------------------------------------------------------

	public ItemStack getItemStack() {
		return itemStack;
	}

	public void setItemStack(ItemStack itemStack) {
		this.itemStack = itemStack;
	}

	public float getRatio() {
		return ratio;
	}

	public void setRatio(float ratio) {
		this.ratio = ratio;
	}
	
}
