package net.blueva.arcade.modules.knockback.support;

import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class KnockbackMessagingService {

    private final ModuleConfigAPI moduleConfig;

    public KnockbackMessagingService(ModuleConfigAPI moduleConfig) {
        this.moduleConfig = moduleConfig;
    }

    public void sendDescription(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                String winMode) {
        String descriptionKey = "description." + winMode;
        List<String> description = moduleConfig.getStringListFrom("language.yml", descriptionKey);
        if (description == null || description.isEmpty()) {
            description = moduleConfig.getStringListFrom("language.yml", "description");
        }

        for (Player player : context.getPlayers()) {
            for (String line : description) {
                context.getMessagesAPI().sendRaw(player, line);
            }
        }
    }

    public void broadcastDeathMessage(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                      Player victim,
                                      Player killer) {
        // Don't broadcast death messages for spectators
        if (context.getSpectators().contains(victim)) {
            return;
        }

        String path = killer != null ? "messages.deaths.killed_by_player" : "messages.deaths.generic";
        String message = getRandomMessage(path);

        if (message == null) {
            return;
        }

        message = message
                .replace("{victim}", victim.getName())
                .replace("{killer}", killer != null ? killer.getName() : "");

        for (Player player : context.getPlayers()) {
            context.getMessagesAPI().sendRaw(player, message);
        }
    }

    public String getModeLabel(String mode) {
        if ("most_kills".equals(mode)) {
            return moduleConfig.getStringFrom("language.yml", "scoreboard.mode_labels.most_kills");
        }
        return moduleConfig.getStringFrom("language.yml", "scoreboard.mode_labels.last_standing");
    }

    public String getScoreboardPath(String winMode) {
        return "scoreboard." + winMode;
    }

    private String getRandomMessage(String path) {
        List<String> messages = moduleConfig.getStringListFrom("language.yml", path);
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        int index = ThreadLocalRandom.current().nextInt(messages.size());
        return messages.get(index);
    }
}
