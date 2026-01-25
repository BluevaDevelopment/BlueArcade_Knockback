package net.blueva.arcade.modules.knockback.listener;

import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.game.GamePhase;
import net.blueva.arcade.modules.knockback.game.KnockbackGameManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class KnockbackListener implements Listener {

    private final KnockbackGameManager gameManager;

    public KnockbackListener(KnockbackGameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = gameManager.getGameContext(player);

        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        if (event.getTo() == null) {
            return;
        }

        if (context.getPhase() != GamePhase.PLAYING) {
            if (!context.isInsideBounds(event.getTo())) {
                context.respawnPlayer(player);
                gameManager.handleRespawnEffects(player);
            }
            return;
        }

        if (!context.isInsideBounds(event.getTo())) {
            gameManager.handlePlayerFall(player);
            return;
        }

        Material deathBlock = getDeathBlock(context);
        Material blockBelowType = event.getTo().clone().subtract(0, 1, 0).getBlock().getType();
        if (blockBelowType == deathBlock) {
            gameManager.handlePlayerFall(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = gameManager.getGameContext(player);

        if (context == null || !context.isPlayerPlaying(player)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = gameManager.getGameContext(victim);
        if (context == null || context.getPhase() != GamePhase.PLAYING) {
            return;
        }

        if (!context.isPlayerPlaying(victim) || !context.isPlayerPlaying(attacker) || attacker.equals(victim)) {
            event.setCancelled(true);
            return;
        }

        event.setDamage(0);
        gameManager.handleKnockbackHit(attacker, victim);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVoidDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = gameManager.getGameContext(player);
        if (context == null || context.getPhase() != GamePhase.PLAYING) {
            return;
        }

        if (!context.isPlayerPlaying(player)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            gameManager.handlePlayerFall(player);
        }
    }

    private Material getDeathBlock(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        try {
            String deathBlockName = context.getDataAccess().getGameData("basic.death_block", String.class);
            if (deathBlockName != null) {
                return Material.valueOf(deathBlockName.toUpperCase());
            }
        } catch (Exception ignored) {
            // fallback
        }
        return Material.BARRIER;
    }
}
