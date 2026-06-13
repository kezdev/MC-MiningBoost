package dev.kezhall.miningboost;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Entry point for the plugin. Paper creates one instance of this class and
 * calls onEnable() when the plugin loads and onDisable() when it unloads.
 *
 * Feature: while a player holds an enabled tool, mining is faster. Built from
 * two vanilla player attributes (1.21+), both configurable in config.yml:
 *   - BLOCK_BREAK_SPEED : multiplies total break speed (base + efficiency)
 *   - MINING_EFFICIENCY : multiplies only the Efficiency enchantment's bonus
 */
public final class MiningBoost extends JavaPlugin implements Listener {

    // Stable keys so we can find, update and remove our own modifiers later.
    private NamespacedKey breakSpeedKey;
    private NamespacedKey efficiencyKey;

    // --- Settings loaded from config.yml ---
    private double breakSpeedBoost;
    private double efficiencyBoost;
    private boolean boostPickaxe;
    private boolean boostAxe;
    private boolean boostShovel;
    private boolean boostHoe;
    private boolean boostShears;

    @Override
    public void onEnable() {
        breakSpeedKey = new NamespacedKey(this, "mining_boost_break_speed");
        efficiencyKey = new NamespacedKey(this, "mining_boost_efficiency");

        saveDefaultConfig(); // writes config.yml on first run if absent
        loadSettings();

        getServer().getPluginManager().registerEvents(this, this);

        // Safety net: re-check every online player's held item twice a second.
        // Covers cases the held-item event misses (inventory swaps, pickups, etc.).
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player player : getServer().getOnlinePlayers()) {
                updatePlayer(player);
            }
        }, 20L, 10L);

        getLogger().info("MiningBoost enabled. Break speed +" + percent(breakSpeedBoost)
                + "%, efficiency +" + percent(efficiencyBoost) + "%.");
    }

    @Override
    public void onDisable() {
        // Clean up our modifiers so reloading the plugin never stacks them.
        for (Player player : getServer().getOnlinePlayers()) {
            setModifier(player, Attribute.BLOCK_BREAK_SPEED, breakSpeedKey, false, 0);
            setModifier(player, Attribute.MINING_EFFICIENCY, efficiencyKey, false, 0);
        }
        getLogger().info("MiningBoost disabled. Goodbye!");
    }

    // ----- Configuration -----------------------------------------------------

    /** Reads values from config.yml into fields. Safe to call again to reload. */
    private void loadSettings() {
        reloadConfig();
        FileConfiguration cfg = getConfig();
        breakSpeedBoost = cfg.getDouble("break-speed-boost", 0.05);
        efficiencyBoost = cfg.getDouble("efficiency-boost", 0.05);
        boostPickaxe = cfg.getBoolean("tools.pickaxe", true);
        boostAxe = cfg.getBoolean("tools.axe", true);
        boostShovel = cfg.getBoolean("tools.shovel", true);
        boostHoe = cfg.getBoolean("tools.hoe", true);
        boostShears = cfg.getBoolean("tools.shears", true);
    }

    private static int percent(double multiplier) {
        return (int) Math.round(multiplier * 100);
    }

    // ----- Mining boost ------------------------------------------------------

    /** Adds or removes the boost based on whether the player's main hand holds an enabled tool. */
    private void updatePlayer(Player player) {
        boolean holdingTool = isBoostedTool(player.getInventory().getItemInMainHand().getType());
        setModifier(player, Attribute.BLOCK_BREAK_SPEED, breakSpeedKey, holdingTool, breakSpeedBoost);
        setModifier(player, Attribute.MINING_EFFICIENCY, efficiencyKey, holdingTool, efficiencyBoost);
    }

    /**
     * Ensures our modifier is present with the right amount (or absent). Idempotent,
     * and re-applies automatically if the configured amount changed after a reload.
     */
    private void setModifier(Player player, Attribute attribute, NamespacedKey key,
                             boolean shouldApply, double amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return; // entity doesn't have this attribute
        }

        AttributeModifier existing = null;
        for (AttributeModifier mod : instance.getModifiers()) {
            if (key.equals(mod.getKey())) {
                existing = mod;
                break;
            }
        }

        if (shouldApply && amount != 0) {
            if (existing == null || existing.getAmount() != amount) {
                if (existing != null) {
                    instance.removeModifier(existing);
                }
                instance.addModifier(new AttributeModifier(key, amount,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1));
            }
        } else if (existing != null) {
            // Copy first to avoid modifying the collection while iterating it.
            for (AttributeModifier mod : new ArrayList<>(instance.getModifiers())) {
                if (key.equals(mod.getKey())) {
                    instance.removeModifier(mod);
                }
            }
        }
    }

    /** Whether this material is a tool type that's enabled in the config. */
    private boolean isBoostedTool(Material material) {
        String name = material.name();
        if (boostPickaxe && name.endsWith("_PICKAXE")) return true;
        if (boostAxe && name.endsWith("_AXE")) return true;
        if (boostShovel && name.endsWith("_SHOVEL")) return true;
        if (boostHoe && name.endsWith("_HOE")) return true;
        if (boostShears && material == Material.SHEARS) return true;
        return false;
    }

    // ----- Events ------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(Component.text("Welcome, " + player.getName() + "!", NamedTextColor.AQUA));
        updatePlayer(player); // apply boost immediately if they're already holding a tool
    }

    // Fires the instant a player scrolls to a new hotbar slot, for snappy response.
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        boolean holdingTool = newItem != null && isBoostedTool(newItem.getType());
        setModifier(player, Attribute.BLOCK_BREAK_SPEED, breakSpeedKey, holdingTool, breakSpeedBoost);
        setModifier(player, Attribute.MINING_EFFICIENCY, efficiencyKey, holdingTool, efficiencyBoost);
    }

    // ----- Commands ----------------------------------------------------------

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("miningboost")) {
            if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("miningboost.reload")) {
                    sender.sendMessage(Component.text("You don't have permission to do that.", NamedTextColor.RED));
                    return true;
                }
                loadSettings();
                for (Player player : getServer().getOnlinePlayers()) {
                    updatePlayer(player); // apply new values live
                }
                sender.sendMessage(Component.text("MiningBoost config reloaded.", NamedTextColor.GREEN));
                return true;
            }

            // No/unknown argument: show current status.
            sender.sendMessage(Component.text("MiningBoost — break speed +" + percent(breakSpeedBoost)
                    + "%, efficiency +" + percent(efficiencyBoost) + "%.", NamedTextColor.AQUA));
            sender.sendMessage(Component.text("Tools: " + enabledToolsSummary(), NamedTextColor.GRAY));
            return true;
        }

        return false;
    }

    private String enabledToolsSummary() {
        ArrayList<String> on = new ArrayList<>();
        if (boostPickaxe) on.add("pickaxe");
        if (boostAxe) on.add("axe");
        if (boostShovel) on.add("shovel");
        if (boostHoe) on.add("hoe");
        if (boostShears) on.add("shears");
        return on.isEmpty() ? "none" : String.join(", ", on);
    }
}
