package fr.aquilon.minecraft.aquilonthings.modules.AQLLooting;

import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

/**
 * @author Dniektr
 */
@AQLThingsModule(
		name = "AQLLooting"
)
public class AQLLooting implements IModule {
	
	private Player playerKiller;

	/**
	 * Appel lors du meurtre (ou crime) d'une entité par un joueur
	 * @param event
	 */
	@EventHandler
	public void onEntityDeathEvent(EntityDeathEvent event) {
		if (event.getEntity().getKiller() instanceof Player) {
			playerKiller = event.getEntity().getKiller();
			dropCustomLoots(event.getEntity());
		}
	}

	/**
	 * Méthode qui récupère la liste des loots de l'animal tué et
	 * qui fait spawn de façon aléatoire les loots associés
	 * @param entity
	 */
	private void dropCustomLoots(LivingEntity entity) {
		EntityType entityType = entity.getType();
		MobLootInfo loots = null;
		for (MobLootInfo lootInfo : getListMobLootInfo()) {
			if (lootInfo != null && lootInfo.getType().equals(entityType)) {
				loots = lootInfo;
				break;
			}
		}
		
		if (loots != null && !loots.getProbabilityLootList().isEmpty()) {
			for (ProbabilityLoot prob : loots.getProbabilityLootList()) {
				Random rand = new Random();
				if (rand.nextFloat() < prob.getRatio()) {
					playerKiller.getWorld().dropItemNaturally(entity.getLocation(), new ItemStack(prob.getItemStack()));
				}
			}
		}
	}
	
	//-------------------------------------

	public List<MobLootInfo> getListMobLootInfo() {
		return LootingList.listMobLootInfo;
	}
	
	@Override
	public boolean onStartUp(DatabaseConnector db) {return true;}

	@Override
	public boolean onStop() {return true;}

}
