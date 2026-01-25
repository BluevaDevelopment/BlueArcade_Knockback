package net.blueva.arcade.modules.knockback.game;

import net.blueva.arcade.api.config.CoreConfigAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import net.blueva.arcade.api.game.GameContext;
import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.ModuleAPI;
import net.blueva.arcade.api.visuals.VisualEffectsAPI;
import net.blueva.arcade.modules.knockback.state.KnockbackArenaState;
import net.blueva.arcade.modules.knockback.state.KnockbackStateRegistry;
import net.blueva.arcade.modules.knockback.support.KnockbackLoadoutService;
import net.blueva.arcade.modules.knockback.support.KnockbackMessagingService;
import net.blueva.arcade.modules.knockback.support.KnockbackStatsService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KnockbackGameManager {

    private final ModuleConfigAPI moduleConfig;
    private final CoreConfigAPI coreConfig;
    private final ModuleInfo moduleInfo;
    private final KnockbackStatsService statsService;
    private final KnockbackLoadoutService loadoutService;
    private final KnockbackMessagingService messagingService;
    private final KnockbackStateRegistry stateRegistry;

    public KnockbackGameManager(ModuleConfigAPI moduleConfig,
                                CoreConfigAPI coreConfig,
                                ModuleInfo moduleInfo,
                                KnockbackStatsService statsService) {
        this.moduleConfig = moduleConfig;
        this.coreConfig = coreConfig;
        this.moduleInfo = moduleInfo;
        this.statsService = statsService;
        this.loadoutService = new KnockbackLoadoutService(moduleConfig);
        this.messagingService = new KnockbackMessagingService(moduleConfig);
        this.stateRegistry = new KnockbackStateRegistry();
    }

    public void handleStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        context.getSchedulerAPI().cancelArenaTasks(context.getArenaId());

        KnockbackArenaState arenaState = stateRegistry.startArena(context);
        for (Player player : context.getPlayers()) {
            arenaState.getPlayerKills().put(player.getUniqueId(), 0);
        }

        messagingService.sendDescription(context, getWinMode(context));
    }

    public void handleCountdownTick(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                    int secondsLeft) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) continue;

            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.starting_game.countdown"));

            String title = coreConfig.getLanguage("titles.starting_game.title")
                    .replace("{game_display_name}", moduleInfo.getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            String subtitle = coreConfig.getLanguage("titles.starting_game.subtitle")
                    .replace("{game_display_name}", moduleInfo.getName())
                    .replace("{time}", String.valueOf(secondsLeft));

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 5);
        }
    }

    public void handleCountdownFinish(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) continue;

            String title = coreConfig.getLanguage("titles.game_started.title")
                    .replace("{game_display_name}", moduleInfo.getName());

            String subtitle = coreConfig.getLanguage("titles.game_started.subtitle")
                    .replace("{game_display_name}", moduleInfo.getName());

            context.getTitlesAPI().sendRaw(player, title, subtitle, 0, 20, 20);
            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.starting_game.start"));
        }
    }

    public boolean freezePlayersOnCountdown() {
        return false;
    }

    public void handleGameStart(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        startGameTimer(context);

        for (Player player : context.getPlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            loadoutService.giveKnockbackStick(player);
            loadoutService.giveStartingItems(player);
            loadoutService.applyStartingEffects(player);
            loadoutService.applyRespawnEffects(player);
            context.getScoreboardAPI().showScoreboard(player, messagingService.getScoreboardPath(getWinMode(context)));
        }
    }

    private void startGameTimer(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        int arenaId = context.getArenaId();

        Integer gameTime = context.getDataAccess().getGameData("basic.time", Integer.class);
        if (gameTime == null || gameTime == 0) {
            gameTime = 180;
        }

        final int[] timeLeft = {gameTime};
        String taskId = "arena_" + arenaId + "_knockback_timer";
        KnockbackArenaState arenaState = stateRegistry.getArenaState(context);

        context.getSchedulerAPI().runTimer(taskId, () -> {
            if (arenaState == null || arenaState.isEnded()) {
                context.getSchedulerAPI().cancelTask(taskId);
                return;
            }

            timeLeft[0]--;

            List<Player> alivePlayers = context.getAlivePlayers();
            List<Player> allPlayers = context.getPlayers();

            if (alivePlayers.size() <= 1 || timeLeft[0] <= 0) {
                endGameOnce(context);
                return;
            }

            String actionBarTemplate = coreConfig.getLanguage("action_bar.in_game.global");
            for (Player player : allPlayers) {
                if (!player.isOnline()) continue;

                Map<String, String> customPlaceholders = getCustomPlaceholders(player);
                customPlaceholders.put("time", String.valueOf(timeLeft[0]));
                customPlaceholders.put("alive", String.valueOf(alivePlayers.size()));
                customPlaceholders.put("spectators", String.valueOf(context.getSpectators().size()));

                if (actionBarTemplate != null) {
                    String actionBarMessage = actionBarTemplate
                            .replace("{time}", String.valueOf(timeLeft[0]))
                            .replace("{round}", String.valueOf(context.getCurrentRound()))
                            .replace("{round_max}", String.valueOf(context.getMaxRounds()));
                    context.getMessagesAPI().sendActionBar(player, actionBarMessage);
                }

                context.getScoreboardAPI().update(player, messagingService.getScoreboardPath(getWinMode(context)), customPlaceholders);
            }
        }, 0L, 20L);
    }

    private void endGameOnce(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        KnockbackArenaState arenaState = stateRegistry.getArenaState(context);
        if (arenaState == null || arenaState.isEnded()) {
            return;
        }

        arenaState.markEnded();
        context.getSchedulerAPI().cancelArenaTasks(context.getArenaId());

        List<Player> alivePlayers = new ArrayList<>(context.getAlivePlayers());
        String winMode = getWinMode(context);
        if (alivePlayers.size() == 1 && "last_standing".equals(winMode)) {
            Player winner = alivePlayers.getFirst();
            context.setWinner(winner);
            handleWin(winner);
        }

        if ("most_kills".equals(winMode)) {
            handleMostKillsOutcome(context);
        }

        context.endGame();
    }

    public void handleEnd(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        context.getSchedulerAPI().cancelArenaTasks(context.getArenaId());

        statsService.recordGamesPlayed(context.getPlayers());
        stateRegistry.removeArena(context);
    }

    public void onDisable() {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> anyContext = stateRegistry.anyContext();
        if (anyContext != null) {
            anyContext.getSchedulerAPI().cancelModuleTasks("knockback");
        }
        stateRegistry.clear();
    }

    public void handleWin(Player player) {
        KnockbackArenaState arenaState = stateRegistry.getArenaState(player);
        if (arenaState == null) {
            return;
        }

        if (arenaState.getWinner() == null) {
            arenaState.setWinner(player.getUniqueId());
            statsService.recordWin(player);
        }
    }

    public Map<String, String> getCustomPlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();

        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getGameContext(player);
        KnockbackArenaState arenaState = stateRegistry.getArenaState(player);

        if (context != null && arenaState != null) {
            placeholders.put("alive", String.valueOf(context.getAlivePlayers().size()));
            placeholders.put("spectators", String.valueOf(context.getSpectators().size()));
            placeholders.put("kills", String.valueOf(getPlayerKills(arenaState, player)));
            placeholders.put("mode", messagingService.getModeLabel(getWinMode(context)));

            if ("most_kills".equals(getWinMode(context))) {
                List<Player> topPlayers = getTopPlayersByKills(context, arenaState);
                for (int i = 0; i < 5; i++) {
                    String placeKey = "place_" + (i + 1);
                    String killsKey = "kills_" + (i + 1);
                    if (topPlayers.size() > i) {
                        Player topPlayer = topPlayers.get(i);
                        placeholders.put(placeKey, topPlayer.getName());
                        placeholders.put(killsKey, String.valueOf(getPlayerKills(arenaState, topPlayer)));
                    } else {
                        placeholders.put(placeKey, "-");
                        placeholders.put(killsKey, "0");
                    }
                }
            }
        }

        return placeholders;
    }

    public void handlePlayerFall(Player player) {
        GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context = getGameContext(player);
        KnockbackArenaState arenaState = stateRegistry.getArenaState(player);
        if (context == null || arenaState == null || arenaState.isEnded()) {
            return;
        }

        Location deathLocation = player.getLocation();

        UUID playerId = player.getUniqueId();
        if (!arenaState.getFallingPlayers().add(playerId)) {
            return;
        }

        Player killer = getRecentKiller(context, arenaState, player);
        if (killer != null) {
            handleKillCredit(context, killer);
        }

        playVisualEffects(player, killer, deathLocation);

        String winMode = getWinMode(context);
        boolean knockedByPlayer = killer != null;

        messagingService.broadcastDeathMessage(context, player, killer);

        if ("most_kills".equals(winMode)) {
            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();

            int respawnDelayTicks = Math.max(0, moduleConfig.getInt("respawn.most_kills_delay_ticks", 60));
            context.getSchedulerAPI().runLater(
                    "knockback_respawn_" + context.getArenaId() + "_" + player.getUniqueId(),
                    () -> {
                        arenaState.getFallingPlayers().remove(playerId);
                        if (arenaState.isEnded() || !context.isPlayerPlaying(player)) {
                            return;
                        }
                        context.respawnPlayer(player);
                        player.setGameMode(GameMode.SURVIVAL);
                        loadoutService.giveKnockbackStick(player);
                        loadoutService.giveStartingItems(player);
                        loadoutService.applyStartingEffects(player);
                        loadoutService.applyRespawnEffects(player);
                        context.getSoundsAPI().play(player, coreConfig.getSound("sounds.in_game.respawn"));
                    },
                    respawnDelayTicks
            );
            return;
        }

        context.eliminatePlayer(player, moduleConfig.getStringFrom("language.yml", "messages.eliminated"));
        player.getInventory().clear();
        player.setGameMode(GameMode.SPECTATOR);
        if (knockedByPlayer) {
            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.in_game.dead"));
            context.getTitlesAPI().sendRaw(player,
                    moduleConfig.getStringFrom("language.yml", "titles.you_died.title"),
                    moduleConfig.getStringFrom("language.yml", "titles.you_died.subtitle"),
                    0, 80, 20);
        } else {
            context.getSoundsAPI().play(player, coreConfig.getSound("sounds.in_game.classified"));
            context.getTitlesAPI().sendRaw(player,
                    moduleConfig.getStringFrom("language.yml", "titles.classified.title"),
                    moduleConfig.getStringFrom("language.yml", "titles.classified.subtitle"),
                    0, 80, 20);
        }
    }

    private void playVisualEffects(Player target, Player killer, Location deathLocation) {
        VisualEffectsAPI visualEffectsAPI = ModuleAPI.getVisualEffectsAPI();
        if (visualEffectsAPI == null) {
            return;
        }
        if (deathLocation != null) {
            visualEffectsAPI.playDeathEffect(target, deathLocation);
        } else {
            visualEffectsAPI.playDeathEffect(target);
        }
        if (killer != null) {
            visualEffectsAPI.playKillEffect(killer);
        }
    }

    public void handleRespawnEffects(Player player) {
        loadoutService.applyRespawnEffects(player);
    }

    public void handleKnockbackHit(Player attacker, Player victim) {
        statsService.recordKnockbackHit(attacker);

        KnockbackArenaState arenaState = stateRegistry.getArenaState(victim);
        if (arenaState == null) {
            return;
        }

        arenaState.getLastHitBy().put(victim.getUniqueId(), attacker.getUniqueId());
        arenaState.getLastHitTime().put(victim.getUniqueId(), System.currentTimeMillis());
    }

    public void handleKillCredit(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                 Player killer) {
        KnockbackArenaState arenaState = stateRegistry.getArenaState(context);
        if (arenaState == null || killer == null) {
            return;
        }

        statsService.recordKnockbackKill(killer);
        arenaState.getPlayerKills().merge(killer.getUniqueId(), 1, Integer::sum);
    }

    public GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> getGameContext(Player player) {
        return stateRegistry.getContext(player);
    }

    public void handlePlayerLeave(Player player) {
        KnockbackArenaState arenaState = stateRegistry.getArenaState(player);
        if (arenaState != null) {
            arenaState.getFallingPlayers().remove(player.getUniqueId());
            arenaState.getLastHitBy().remove(player.getUniqueId());
            arenaState.getLastHitTime().remove(player.getUniqueId());
            arenaState.getPlayerKills().remove(player.getUniqueId());
        }
    }

    public String getWinMode(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        String mode = context.getDataAccess().getGameData("basic.win_mode", String.class);
        if (mode == null) {
            return "last_standing";
        }
        mode = mode.toLowerCase();
        if (!mode.equals("last_standing") && !mode.equals("most_kills")) {
            return "last_standing";
        }
        return mode;
    }

    public Player getTopKiller(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        KnockbackArenaState arenaState = stateRegistry.getArenaState(context);
        List<Player> topPlayers = getTopPlayersByKills(context, arenaState);
        if (topPlayers.isEmpty()) {
            return null;
        }

        return topPlayers.getFirst();
    }

    private void handleMostKillsOutcome(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context) {
        KnockbackArenaState arenaState = stateRegistry.getArenaState(context);
        if (arenaState == null) {
            return;
        }

        List<Player> topPlayers = getTopPlayersByKills(context, arenaState);
        if (topPlayers.isEmpty()) {
            return;
        }

        Player winner = topPlayers.getFirst();
        context.setWinner(winner);
        handleWin(winner);

        for (int i = 1; i < topPlayers.size(); i++) {
            Player player = topPlayers.get(i);
            if (!context.isPlayerPlaying(player)) {
                continue;
            }
            context.finishPlayer(player);
        }
    }

    private List<Player> getTopPlayersByKills(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                              KnockbackArenaState arenaState) {
        Map<Player, Integer> killCounts = new HashMap<>();
        if (arenaState == null) {
            return List.of();
        }

        for (Player player : context.getPlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            killCounts.put(player, getPlayerKills(arenaState, player));
        }

        List<Map.Entry<Player, Integer>> sorted = new ArrayList<>(killCounts.entrySet());
        sorted.sort((a, b) -> {
            int compare = Integer.compare(b.getValue(), a.getValue());
            if (compare != 0) {
                return compare;
            }
            return a.getKey().getName().compareToIgnoreCase(b.getKey().getName());
        });

        List<Player> topPlayers = new ArrayList<>();
        for (Map.Entry<Player, Integer> entry : sorted) {
            topPlayers.add(entry.getKey());
            if (topPlayers.size() >= 5) {
                break;
            }
        }

        return topPlayers;
    }

    private int getPlayerKills(KnockbackArenaState arenaState, Player player) {
        return arenaState.getPlayerKills().getOrDefault(player.getUniqueId(), 0);
    }

    private Player getRecentKiller(GameContext<Player, Location, World, Material, ItemStack, Sound, Block, Entity> context,
                                   KnockbackArenaState arenaState,
                                   Player victim) {
        UUID killerId = arenaState.getLastHitBy().get(victim.getUniqueId());
        Long hitTime = arenaState.getLastHitTime().get(victim.getUniqueId());
        if (killerId == null || hitTime == null) {
            return null;
        }

        long windowTicks = moduleConfig.getInt("kills.credit_window_ticks", 200);
        long windowMillis = windowTicks * 50L;
        if (System.currentTimeMillis() - hitTime > windowMillis) {
            return null;
        }

        for (Player player : context.getPlayers()) {
            if (player.getUniqueId().equals(killerId)) {
                return player;
            }
        }

        return null;
    }
}
