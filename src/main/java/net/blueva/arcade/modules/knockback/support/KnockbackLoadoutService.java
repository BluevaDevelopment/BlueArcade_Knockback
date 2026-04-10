package net.blueva.arcade.modules.knockback.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.ui.ItemAPI;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class KnockbackLoadoutService {

    private final ModuleConfigAPI moduleConfig;

    public KnockbackLoadoutService(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public void giveStartingItems(Player player) {
        List<String> startingItems = moduleConfig.getStringList("items.starting_items");

        if (startingItems == null || startingItems.isEmpty()) {
            return;
        }

        for (String itemString : startingItems) {
            try {
                String[] parts = itemString.split(":");
                if (parts.length >= 2) {
                    Material material = Material.valueOf(parts[0].toUpperCase());
                    int amount = Integer.parseInt(parts[1]);
                    int slot = parts.length >= 3 ? Integer.parseInt(parts[2]) : -1;

                    ItemStack item = new ItemStack(material, amount);

                    if (slot >= 0 && slot < 36) {
                        player.getInventory().setItem(slot, item);
                    } else {
                        player.getInventory().addItem(item);
                    }
                }
            } catch (Exception ignored) {
                // Ignore malformed entries
            }
        }
    }

    public void giveKnockbackStick(Player player, ItemAPI<Player, ItemStack, Material> itemAPI) {
        String materialName = moduleConfig.getString("items.knockback_stick.material");
        int amount = moduleConfig.getInt("items.knockback_stick.amount", 1);
        int slot = moduleConfig.getInt("items.knockback_stick.slot", 0);
        int knockbackLevel = moduleConfig.getInt("items.knockback_stick.knockback_level", 3);
        boolean unbreakable = moduleConfig.getBoolean("items.knockback_stick.unbreakable", true);

        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (Exception e) {
            material = Material.STICK;
        }

        ItemStack stick = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.KNOCKBACK, Math.max(1, knockbackLevel), true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setUnbreakable(unbreakable);
            stick.setItemMeta(meta);
        }

        String name = moduleConfig.getString("items.knockback_stick.name");
        List<String> lore = moduleConfig.getStringList("items.knockback_stick.lore");

        if (itemAPI != null) {
            stick = itemAPI.decorate(stick, name, lore);
        } else {
            // Fallback if ItemAPI is unavailable
            ItemMeta fallbackMeta = stick.getItemMeta();
            if (fallbackMeta != null) {
                if (name != null && !name.isEmpty()) {
                    fallbackMeta.setDisplayName(name);
                }
                if (lore != null && !lore.isEmpty()) {
                    fallbackMeta.setLore(lore);
                }
                stick.setItemMeta(fallbackMeta);
            }
        }

        if (slot >= 0 && slot < 36) {
            player.getInventory().setItem(slot, stick);
        } else {
            player.getInventory().addItem(stick);
        }
    }

    public void applyStartingEffects(Player player) {
        List<String> startingEffects = moduleConfig.getStringList("effects.starting_effects");

        if (startingEffects == null || startingEffects.isEmpty()) {
            return;
        }

        for (String effectString : startingEffects) {
            try {
                String[] parts = effectString.split(":");
                if (parts.length >= 3) {
                    PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                    int duration = Integer.parseInt(parts[1]);
                    int amplifier = Integer.parseInt(parts[2]);

                    if (effectType != null) {
                        player.addPotionEffect(new PotionEffect(
                                effectType, duration, amplifier, false, false
                        ));
                    }
                }
            } catch (Exception ignored) {
                // Ignore malformed entries
            }
        }
    }

    public void applyRespawnEffects(Player player) {
        List<String> respawnEffects = moduleConfig.getStringList("effects.respawn_effects");

        if (respawnEffects == null || respawnEffects.isEmpty()) {
            return;
        }

        for (String effectString : respawnEffects) {
            try {
                String[] parts = effectString.split(":");
                if (parts.length >= 3) {
                    PotionEffectType effectType = PotionEffectType.getByName(parts[0].toUpperCase());
                    int duration = Integer.parseInt(parts[1]);
                    int amplifier = Integer.parseInt(parts[2]);

                    if (effectType != null) {
                        player.addPotionEffect(new PotionEffect(
                                effectType, duration, amplifier, false, false
                        ));
                    }
                }
            } catch (Exception ignored) {
                // Ignore malformed entries
            }
        }
    }
}
