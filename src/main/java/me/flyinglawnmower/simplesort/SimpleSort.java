package me.flyinglawnmower.simplesort;

/*
 * SimpleSort plugin by:
 * - Shadow1013GL
 * - Pyr0Byt3
 * - pendo324
 */
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

public class SimpleSort extends JavaPlugin implements Listener, TabCompleter {

    private final Permission inventorySortPerm = new Permission("simplesort.inventory");
    private final Permission chestSortPerm = new Permission("simplesort.chest");
    private final Permission wandSortPerm = new Permission("simplesort.chest.wand");
    private final Permission autoSortPerm = new Permission("simplesort.chest.auto");

    private final String autoSortConfigPath = "auto-sorting";
    private final HashMap<String, Boolean> autoSortList = new HashMap<>();
    private Material wand;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        wand = Material.matchMaterial(getConfig().getString("wand", "STICK"));

        if (getConfig().isSet(autoSortConfigPath)) {
            for (String key : getConfig().getConfigurationSection(autoSortConfigPath).getKeys(false)) {
                autoSortList.put(key, getConfig().getBoolean(autoSortConfigPath + "." + key));
            }
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (!autoSortList.isEmpty()) {
            for (Entry<String, Boolean> entry : autoSortList.entrySet()) {
                getConfig().set(autoSortConfigPath + "." + entry.getKey(), entry.getValue());
            }
            saveConfig();
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        Player player = event.getPlayer();

        if (player.hasPermission(autoSortPerm) && !autoSortList.containsKey(player.getName())) {
            autoSortList.put(player.getName(), false);
        }
        event.getPlayer().setMetadata("commandSorting", new FixedMetadataValue(this, false));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            if (command.getName().equalsIgnoreCase("sort")) {
                Player player = (Player) sender;
                Set<Material> nullset = null;
                Block block = player.getTargetBlock(nullset, 4);

                if (args.length == 0) {
                    if (block.getType() == Material.CHEST
                            || block.getType() == Material.TRAPPED_CHEST
                            || block.getType() == Material.ENDER_CHEST) {
                        player.performCommand("sort chest");
                    } else {
                        player.performCommand("sort top");
                    }
                    return true;
                }

                switch (args[0].toLowerCase()) {
                    case "chest":
                        if (player.hasPermission(chestSortPerm)) {
                            if (block.getType() == Material.CHEST
                                    || block.getType() == Material.TRAPPED_CHEST
                                    || block.getType() == Material.ENDER_CHEST) {
                                player.setMetadata("commandSorting", new FixedMetadataValue(this, true));
                                getServer().getPluginManager().callEvent(new PlayerInteractEvent(player, Action.LEFT_CLICK_BLOCK, new ItemStack(wand), block, BlockFace.SELF));
                                return true;
                            } else {
                                player.sendMessage(ChatColor.DARK_RED + "Not currently targeting a chest!");
                                return true;
                            }
                        } else {
                            player.sendMessage(ChatColor.DARK_RED + "You don't have permission to sort chests!");
                        }
                        break;
                    case "top":
                        if (player.hasPermission(inventorySortPerm)) {
                            sortInventory(player.getInventory(), 9, 36);
                            player.sendMessage(ChatColor.DARK_GREEN + "Inventory top sorted!");
                        } else {
                            player.sendMessage(ChatColor.DARK_RED + "You don't have permission to sort your inventory!");
                        }
                        return true;
                    case "all":
                        if (player.hasPermission(inventorySortPerm)) {
                            sortInventory(player.getInventory(), 0, 36);
                            player.sendMessage(ChatColor.DARK_GREEN + "Entire inventory sorted!");
                        } else {
                            player.sendMessage(ChatColor.DARK_RED + "You don't have permission to sort your inventory!");
                        }
                        return true;
                    case "hot":
                        if (player.hasPermission(inventorySortPerm)) {
                            sortInventory(player.getInventory(), 0, 9);
                            player.sendMessage(ChatColor.DARK_GREEN + "Hotbar sorted!");
                        } else {
                            player.sendMessage(ChatColor.DARK_RED + "You don't have permission to sort your inventory!");
                        }
                        return true;
                    case "auto":
                        if (player.hasPermission(autoSortPerm)) {
                            if (args.length == 1) {
                                autoSortList.put(player.getName(), !autoSortList.get(player.getName()));
                            } else if (args.length > 1) {
                                if (args[1].equalsIgnoreCase("on")) {
                                    autoSortList.put(player.getName(), true);
                                } else if (args[1].equalsIgnoreCase("off")) {
                                    autoSortList.put(player.getName(), false);
                                } else {
                                    return false;
                                }
                            }

                            if (autoSortList.get(player.getName())) {
                                player.sendMessage(ChatColor.DARK_GREEN + "Auto-sorting enabled!");
                            } else {
                                player.sendMessage(ChatColor.DARK_RED + "Auto-sorting disabled!");
                            }
                        } else {
                            player.sendMessage(ChatColor.DARK_RED + "You don't have permission to use auto-sorting!");
                        }
                        return true;
                    default:
                        return false;
                }
            }
        } else {
            sender.sendMessage("You need to be a player to sort your inventory!");
            return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("auto")) {
            return Arrays.asList("on", "off");
        }
        if (args.length <= 1) {
            return Arrays.asList("top", "all", "hot", "chest", "auto");
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        boolean commandSorting = player.hasMetadata("commandSorting") && player.getMetadata("commandSorting").size() > 0 && player.getMetadata("commandSorting").get(0).asBoolean();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK
                && event.getMaterial() == wand
                && (player.hasPermission(wandSortPerm) || commandSorting)
                || event.getAction() == Action.RIGHT_CLICK_BLOCK
                && player.hasPermission(autoSortPerm) && autoSortList.get(player.getName())) {
            Block block = event.getClickedBlock();
            Inventory inventory;

            if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                inventory = ((InventoryHolder) block.getState()).getInventory();
            } else if (block.getType() == Material.ENDER_CHEST) {
                inventory = player.getEnderChest();
            } else {
                return;
            }

            player.setMetadata("commandSorting", new FixedMetadataValue(this, false));
            sortInventory(inventory, 0, inventory.getSize());
            player.sendMessage(ChatColor.DARK_GREEN + "Chest sorted!");
        }
    }

    private void sortInventory(Inventory inventory, int startIndex, int endIndex) {
        ItemStack[] items = inventory.getContents();
        boolean stackAll = getConfig().getBoolean("stack-all");

        for (int i = startIndex; i < endIndex; i++) {
            ItemStack item1 = items[i];

            if (item1 == null) {
                continue;
            }

            int maxStackSize = stackAll ? 64 : item1.getMaxStackSize();

            if (item1.getAmount() <= 0 || maxStackSize == 1) {
                continue;
            }

            if (item1.getAmount() < maxStackSize) {
                int needed = maxStackSize - item1.getAmount();

                for (int j = i + 1; j < endIndex; j++) {
                    ItemStack item2 = items[j];

                    if (item2 == null || item2.getAmount() <= 0 || maxStackSize == 1) {
                        continue;
                    }

                    if (item2.getType() == item1.getType()
                            && item1.getDurability() == item2.getDurability()
                            && item1.getEnchantments().equals(item2.getEnchantments())
                            && item1.getItemMeta().equals(item2.getItemMeta())) {
                        if (item2.getAmount() > needed) {
                            item1.setAmount(maxStackSize);
                            item2.setAmount(item2.getAmount() - needed);
                            break;
                        } else {
                            items[j] = null;
                            item1.setAmount(item1.getAmount() + item2.getAmount());
                            needed = maxStackSize - item1.getAmount();
                        }
                    }
                }
            }
        }

        Arrays.sort(items, startIndex, endIndex, new ItemComparator());
        inventory.setContents(items);
    }
}
