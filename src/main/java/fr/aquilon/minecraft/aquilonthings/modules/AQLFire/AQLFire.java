package fr.aquilon.minecraft.aquilonthings.modules.AQLFire;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.aquilonthings.annotation.InPacket;
import fr.aquilon.minecraft.aquilonthings.modules.IModule;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@AQLThingsModule(
		name = "AQLFire",
		cmds = {
				@Cmd(value = "tpfire", desc = "Teleportation au dernier incendie"),
				@Cmd(value = "ff", desc = "Fire-fighter: Eteindre un incendie")
		},
		inPackets = @InPacket(AquilonThings.CHANNEL_READY)
)
public class AQLFire implements IModule {
	public static final ModuleLogger LOGGER = ModuleLogger.get();

	public static final String PERM_ALLOWED = AquilonThings.PERM_ROOT+".fire.allowed";
	public static final String PERM_FIGHT = AquilonThings.PERM_ROOT+".fire.fight";

	public static final int OPT_RADIUS_CHECK_DEFAULT = 4;
	public static final int OPT_MIN_BLOCKS_ALERT_STAFF_DEFAULT = 10;
	public static final int OPT_MIN_BLOCKS_ALERT_PLAYERS_DEFAULT = 12;
	public static final int OPT_RADIUS_ALERT_DEFAULT = 200;
	public static final int OPT_RADIUS_ALERT_FAR_DEFAULT = 600;
	public static final int OPT_DELAY_DEFAULT = 300;
	public static final int OPT_RADIUS_SNOWBALL_DEFAULT = 1;
	public static final int OPT_MAX_FIGHT_RADIUS_DEFAULT = 30;

	private int opt_radiusCheck;
	private int opt_minBlocksAlertPlayers;
	private int opt_minBlocksAlertStaff;
	private int opt_radiusAlert;
	private int opt_radiusAlertFar;
	private int opt_delay;
	private int opt_radiusSnowball;
	private int opt_maxFightRadius;

	private Map<Location, Long> alerts;
	private Location lastAlert;

	public AQLFire() {
		this.alerts = new HashMap<>();
	}
	
	@Override
	public boolean onStartUp(DatabaseConnector db) {
		this.opt_radiusCheck = AquilonThings.instance.getConfig().getInt("fire.alert.radiusCheck", OPT_RADIUS_CHECK_DEFAULT);
		this.opt_minBlocksAlertPlayers = AquilonThings.instance.getConfig().getInt("fire.alert.minBlocksPlayers", OPT_MIN_BLOCKS_ALERT_PLAYERS_DEFAULT);
		this.opt_minBlocksAlertStaff = AquilonThings.instance.getConfig().getInt("fire.alert.minBlocksStaff", OPT_MIN_BLOCKS_ALERT_STAFF_DEFAULT);
		this.opt_radiusAlert = AquilonThings.instance.getConfig().getInt("fire.alert.radiusMessage", OPT_RADIUS_ALERT_DEFAULT);
		this.opt_radiusAlertFar = AquilonThings.instance.getConfig().getInt("fire.alert.radiusMessageFar", OPT_RADIUS_ALERT_FAR_DEFAULT);
		this.opt_delay = AquilonThings.instance.getConfig().getInt("fire.alert.delay", OPT_DELAY_DEFAULT);
		this.opt_radiusSnowball = AquilonThings.instance.getConfig().getInt("fire.snowballRadius", OPT_RADIUS_SNOWBALL_DEFAULT);
		this.opt_maxFightRadius = AquilonThings.instance.getConfig().getInt("fire.fightRadius", OPT_MAX_FIGHT_RADIUS_DEFAULT);
		return true;
	}

	@Override
	public boolean onStop() {
		return true;
	}

	@EventHandler
	public void onBlockBurn(BlockBurnEvent event) {
		event.setCancelled(true);
	}

	@EventHandler
	public void onBlockIgnite(BlockIgniteEvent event) {
		if (event.isCancelled()) return;
		AQLFireEvent fireEvent = new AQLFireEvent(event);
		fireEvent.call(this);
	}

	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		Projectile projectile = event.getEntity();
		// On vérifie que le projectile est bien une boule de neige
		if (projectile.getType().equals(EntityType.SNOWBALL)) {
			// On essaie d'eteindre le feu
			if (extinguish(projectile.getLocation(), opt_radiusSnowball) > 0) {
				// Envoi de quelques effets au client
				projectile.playEffect(EntityEffect.WOLF_SHAKE);
			}
		}
	}

	public int getRadiusCheck() {
		return opt_radiusCheck;
	}

	public int getMinBlocksAlertPlayers() {
		return opt_minBlocksAlertPlayers;
	}

	public int getMinBlocksAlertStaff() {
		return opt_minBlocksAlertStaff;
	}

	public int getRadiusAlert() {
		return opt_radiusAlert;
	}

	public int getRadiusAlertFar() {
		return opt_radiusAlertFar;
	}

	public int getDelay() {
		return opt_delay;
	}

	// TODO: make this asynchronous (?)
	public int extinguish(Location center, int radius) {
		World world = Objects.requireNonNull(center.getWorld(), "Location world cannot be null");
		int count = 0;
		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					// Suppression du bloc si il s'agit de feu
					if (world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z).getType().equals(Material.FIRE)) {
						count++;
						world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z).setType(Material.AIR);
						Location fireLoc = new Location(world, center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
						world.playEffect(fireLoc, Effect.EXTINGUISH, 5);
					}
				}
			}
		}

		// Extinction du feu des entités environantes
		Collection<Entity> nearbyEntities = world.getNearbyEntities(center, radius, radius, radius);
		for (Entity entity : nearbyEntities) {
			entity.setFireTicks(0);
		}

		return count;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String scmd, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.YELLOW + "Nan mais t'as cru que t'allais où la ? T'es sur la console.");
			return true;
		}
		Player p = (Player) sender;
		if (cmd.getName().equalsIgnoreCase("tpfire")) {
			if (sender.hasPermission(PERM_ALLOWED)) {
				if (lastAlert==null) {
					sender.sendMessage(ChatColor.YELLOW + "Tu le fais exprès ? Il y a pas d'incendie.");
					return true;
				}
				if (!p.teleport(lastAlert)) {
					LOGGER.mInfo("Impossible de TP "+sender.getName()+" à l'incendie "+lastAlert.toString());
				}
			} else {
				sender.sendMessage(ChatColor.RED + "Nope ! T'es pas encore staff :)");
				return true;
			}
		} else if (cmd.getName().equalsIgnoreCase("ff")) {
			if (!sender.hasPermission(PERM_FIGHT)) {
				sender.sendMessage(ChatColor.RED + "Nope ! T'es pas encore staff :)");
				return true;
			}
			int radius = 5;
			if (args.length > 0) {
				try {
					radius = Integer.parseUnsignedInt(args[0]);
				} catch (NumberFormatException e) {
					sender.sendMessage(ChatColor.RED + "Le rayon c'est un nombre !");
					return true;
				}
			}
			if (radius > opt_maxFightRadius) {
				sender.sendMessage(ChatColor.RED + "Rayon trop large, maximum: "+opt_maxFightRadius);
				return true;
			}
			int count = extinguish(p.getLocation(), radius);
			sender.sendMessage(ChatColor.WHITE.toString()+count+ChatColor.YELLOW+" blocks eteins");
			return true;
		}
		return true;
	}

	private Location roundLoc(Location pos) {
		return new Location(
				pos.getWorld(),
				Math.round(pos.getBlockX()/100f)*100f,
				64f,
				Math.round(pos.getBlockZ()/100f)*100f
		);
	}

	public boolean checkAlert(Location position) {
		Location roundLoc = roundLoc(position);
		Long currentTimestamp = System.currentTimeMillis()/1000;
		boolean res = !alerts.containsKey(roundLoc);
		if (!res && (currentTimestamp - alerts.get(roundLoc)) > this.opt_delay) {
			res = true;
			alerts.remove(roundLoc);
		}
		return res;
	}

	public void registerAlert(Location position) {
		registerAlert(position, false);
	}
	public void registerAlert(Location position, boolean staff) {
		Location roundLoc = roundLoc(position);
		Long currentTimestamp = System.currentTimeMillis()/1000;
		if (checkAlert(roundLoc)) {
			alerts.put(roundLoc, currentTimestamp);
			if (staff) lastAlert = position;
		}
	}
}
