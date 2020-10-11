package fr.aquilon.minecraft.aquilonthings.modules;

import fr.aquilon.minecraft.aquilonthings.AquilonThings;
import fr.aquilon.minecraft.aquilonthings.DatabaseConnector;
import fr.aquilon.minecraft.aquilonthings.ModuleLogger;
import fr.aquilon.minecraft.aquilonthings.annotation.AQLThingsModule;
import fr.aquilon.minecraft.aquilonthings.annotation.Cmd;
import fr.aquilon.minecraft.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@AQLThingsModule(
        name = "AQLStaff",
        cmds = {
                @Cmd(value = "workbench", desc = "Ouverture d'un établi"),
                @Cmd(value = "anvil", desc = "Ouverture d'une enclume"),
                @Cmd(value = "enchant", desc = "Ouverture d'une table d'enchantement"),
                @Cmd(value = "head", desc = "Obtenir une tête de joueur"),
                @Cmd(value = "armor", desc = "Obtenir une armure en cuir d'une couleur donnée"),
                @Cmd(value = "name", desc = "Renommer un item"),
                @Cmd(value = "lore", desc = "Définir la description d'un item"),
                @Cmd(value = "openinv", desc = "Ouvrir l'inventaire d'un joueur"),
                @Cmd(value = "openender", desc = "Ouvrir l'enderchest d'un joueur")
        }
)
public class AQLStaff implements IModule {
    public static final ModuleLogger LOGGER = ModuleLogger.get();
    public static final String PERM_UTILS = AquilonThings.PERM_ROOT+".utils.";


    @Override
    public boolean onStartUp(DatabaseConnector db) {
        return true;
    }

    @Override
    public boolean onStop() {
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "La console ne peut pas faire ça...");
            return true;
        }
        Player p = (Player) sender;

        if (!sender.hasPermission(PERM_UTILS+cmd.toLowerCase())) {
            sender.sendMessage(ChatColor.RED + "Ça me semblait évident que c'était reservé aux Staff ...");
            return true;
        }

        if (cmd.equalsIgnoreCase("workbench")) {
            Inventory i = Bukkit.createInventory(p, InventoryType.WORKBENCH);
            p.openInventory(i);
            return true;
        } else if (cmd.equalsIgnoreCase("anvil")) {
            Inventory i = Bukkit.createInventory(p, InventoryType.ANVIL);
            p.openInventory(i);
            return true;
        } else if (cmd.equalsIgnoreCase("openinv") || cmd.equalsIgnoreCase("openender")) {
            Player target = p;
            if (args.length > 0) {
                UUID uuid = Utils.getUUID(args[0]);
                if (uuid != null) target = Bukkit.getPlayer(args[0]);
                else target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    p.sendMessage(ChatColor.RED+"Joueur non trouvé");
                    return true;
                }
                // TODO: load offline player inventory from file (NBT)
            }
            Inventory inv;
            if (cmd.equalsIgnoreCase("openinv")) inv = target.getInventory();
            else inv = target.getEnderChest();
            p.openInventory(inv);
            return true;
        } else if (cmd.equalsIgnoreCase("enchant")) {
            Inventory i = Bukkit.createInventory(p, InventoryType.ENCHANTING);
            p.openInventory(i);
            return true;
        } else if (cmd.equalsIgnoreCase("head")) {
            if (args.length == 1) {
                String target = args[0];
                UUID targetUUID = Utils.getUUID(target);
                OfflinePlayer player;
                if (targetUUID != null) player = Bukkit.getPlayer(targetUUID); // Get online player by UUID
                else player = Bukkit.getPlayer(target); // Get online player by name
                if (player == null && targetUUID != null) player = Bukkit.getOfflinePlayer(targetUUID);
                if (player == null) {
                    UUID uuid = Utils.findUsernameUUID(args[0]);
                    if (uuid == null) {
                        p.sendMessage(ChatColor.RED+"Aucun joueur trouvé avec ces paramètres");
                        return true;
                    }
                    player = Bukkit.getOfflinePlayer(uuid);
                }
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta headMeta = (SkullMeta) head.getItemMeta();
                headMeta.setOwningPlayer(player);
                head.setItemMeta(headMeta);
                addItem(p, new ItemStack[]{head});
                p.sendMessage(ChatColor.YELLOW + "Tête "+(player.hasPlayedBefore() ? "de "+player.getName() : "")+" créée");
                return true;
            }
        } else if (cmd.equalsIgnoreCase("armor")) {

            if (args.length == 3) {
                if (Integer.valueOf(args[0]) < 0 || Integer.valueOf(args[0]) > 255 || Integer.valueOf(args[1]) < 0 || Integer.valueOf(args[1]) > 255 || Integer.valueOf(args[2]) < 0 || Integer.valueOf(args[2]) > 255) {
                    sender.sendMessage(ChatColor.RED + "Erreur de couleur");
                    return true;
                }

                Color armorColor = org.bukkit.Color.fromRGB(Integer.valueOf(args[0]), Integer.valueOf(args[1]), Integer.valueOf(args[2]));

                ItemStack helmet = new ItemStack(Material.LEATHER_HELMET, 1);
                LeatherArmorMeta hm = (LeatherArmorMeta) helmet.getItemMeta();
                hm.setColor(armorColor);
                helmet.setItemMeta(hm);

                ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE, 1);
                LeatherArmorMeta cm = (LeatherArmorMeta) chestplate.getItemMeta();
                cm.setColor(armorColor);
                chestplate.setItemMeta(cm);

                ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS, 1);
                LeatherArmorMeta lm = (LeatherArmorMeta) leggings.getItemMeta();
                lm.setColor(armorColor);
                leggings.setItemMeta(lm);

                ItemStack boots = new ItemStack(Material.LEATHER_BOOTS, 1);
                LeatherArmorMeta bm = (LeatherArmorMeta) boots.getItemMeta();
                bm.setColor(armorColor);
                boots.setItemMeta(bm);

                addItem(p, new ItemStack[]{helmet, chestplate, leggings, boots});
                p.sendMessage(ChatColor.YELLOW + "Armure terminée");
                return true;
            }
        } else if (cmd.equalsIgnoreCase("name")) {
            ItemStack item = p.getInventory().getItemInMainHand();
            if (item.getType()==Material.AIR || item.getAmount()==0) {
                sender.sendMessage(ChatColor.YELLOW + "Sans item c'est plus dur !");
                return true;
            }
            ItemMeta meta = item.getItemMeta();

            if (args.length<1) {
                sender.sendMessage(ChatColor.RED + "Il faut peut-être préciser un nom...");
                return true;
            }
            String name = Utils.joinStrings(args, " ");
            String coloredName = ChatColor.translateAlternateColorCodes('&', name);

            meta.setDisplayName(coloredName);
            item.setItemMeta(meta);
            p.getInventory().setItemInMainHand(item);
            p.sendMessage(ChatColor.YELLOW + "Nom défini : "+ChatColor.WHITE+ChatColor.ITALIC+coloredName);
            return true;
        } else if (cmd.equalsIgnoreCase("lore")) {
            if (args.length<2) return false;

            ItemStack item = p.getInventory().getItemInMainHand();
            if (item.getType()==Material.AIR || item.getAmount()==0) {
                sender.sendMessage(ChatColor.YELLOW + "Sans item c'est plus dur !");
                return true;
            }
            ItemMeta meta = item.getItemMeta();

            int line;
            try {
                line = Integer.parseUnsignedInt(args[0]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "La ligne doit correspondre à un nombre entier...");
                return true;
            }

            if (line==0) {
                sender.sendMessage(ChatColor.RED + "Choisir une ligne de 1 à 10");
                return true;
            }

            if (line>10) {
                sender.sendMessage(ChatColor.RED + "Maximum 10 lignes de lores !");
                return true;
            }

            String lore = Utils.joinStrings(args, " ", 1);
            List<String> lores = meta.getLore();
            if (lores == null) lores = new ArrayList<>();

            while (lores.size() <= line-1) {
                lores.add("");
            }

            lore = ChatColor.translateAlternateColorCodes('&', lore);
            lores.set(line-1, lore);

            meta.setLore(lores);
            item.setItemMeta(meta);
            p.getInventory().setItemInMainHand(item);
            p.sendMessage(ChatColor.YELLOW + "Description définie (ligne "+line+"): " +
                    ChatColor.DARK_PURPLE+ChatColor.ITALIC+lore);
            return true;
        }

        return false;
    }

    public static void addItem(Player player, ItemStack[] items) {
        Map<Integer, ItemStack> remainingItems = player.getInventory().addItem(items);
        for (ItemStack currentItemStack : remainingItems.values()) {
            player.getWorld().dropItem(player.getLocation(), currentItemStack);
        }
    }
}
