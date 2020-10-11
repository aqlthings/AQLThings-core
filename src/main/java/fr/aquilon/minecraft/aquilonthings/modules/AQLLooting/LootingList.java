package fr.aquilon.minecraft.aquilonthings.modules.AQLLooting;

import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class LootingList {

	@SuppressWarnings("deprecation")
	public final static ItemStack GRAISSE_ANIMALE = null; // FIXME !!!
	
	public static List<MobLootInfo> listMobLootInfo = Arrays.asList(
			new MobLootInfo(EntityType.COW, Arrays.asList(new ProbabilityLoot(GRAISSE_ANIMALE, 1/4F))),
			new MobLootInfo(EntityType.CHICKEN, Arrays.asList(new ProbabilityLoot(GRAISSE_ANIMALE, 1/8F))),
			new MobLootInfo(EntityType.DONKEY, Arrays.asList(new ProbabilityLoot(GRAISSE_ANIMALE, 1/4F))),
			new MobLootInfo(EntityType.HORSE, Arrays.asList(new ProbabilityLoot(GRAISSE_ANIMALE, 1/4F))),
			new MobLootInfo(EntityType.LLAMA, Arrays.asList(new ProbabilityLoot(GRAISSE_ANIMALE, 1/4F))),
			new MobLootInfo(EntityType.MULE, Arrays.asList(new ProbabilityLoot(GRAISSE_ANIMALE, 1/4F))),
			new MobLootInfo(EntityType.PIG, Arrays.asList(new ProbabilityLoot(GRAISSE_ANIMALE, 1/3F))),
			new MobLootInfo(EntityType.POLAR_BEAR, Arrays.asList(new ProbabilityLoot(GRAISSE_ANIMALE, 1/3F))),
			new MobLootInfo(EntityType.RABBIT, Arrays.asList(new ProbabilityLoot(GRAISSE_ANIMALE, 1/8F))),
			new MobLootInfo(EntityType.SHEEP, Arrays.asList(new ProbabilityLoot(GRAISSE_ANIMALE, 1/4F)))
		);
	
}
