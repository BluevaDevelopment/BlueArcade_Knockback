package net.blueva.arcade.modules.knockback.support;

import net.blueva.arcade.api.module.ModuleInfo;
import net.blueva.arcade.api.stats.StatDefinition;
import net.blueva.arcade.api.stats.StatScope;
import net.blueva.arcade.api.stats.StatsAPI;
import net.blueva.arcade.api.config.ModuleConfigAPI;
import org.bukkit.entity.Player;

import java.util.Collection;

public class KnockbackStatsService {

    private final StatsAPI statsAPI;
    private final ModuleInfo moduleInfo;
    private final ModuleConfigAPI moduleConfig;

    public KnockbackStatsService(StatsAPI statsAPI, ModuleInfo moduleInfo, ModuleConfigAPI moduleConfig) {
        this.statsAPI = statsAPI;
        this.moduleInfo = moduleInfo;
        this.moduleConfig = moduleConfig;
    }

    public void registerStats() {
        if (statsAPI == null) {
            return;
        }

        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("wins", moduleConfig.getStringFrom("language.yml", "stats.labels.wins", "Wins"), moduleConfig.getStringFrom("language.yml", "stats.descriptions.wins", "Knockback wins"), StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("games_played", moduleConfig.getStringFrom("language.yml", "stats.labels.games_played", "Games Played"), moduleConfig.getStringFrom("language.yml", "stats.descriptions.games_played", "Knockback games played"), StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("knockback_hits", moduleConfig.getStringFrom("language.yml", "stats.labels.knockback_hits", "Knockback hits"), moduleConfig.getStringFrom("language.yml", "stats.descriptions.knockback_hits", "Hits landed with knockback weapons"), StatScope.MODULE));
        statsAPI.registerModuleStat(moduleInfo.getId(),
                new StatDefinition("knockback_kills", moduleConfig.getStringFrom("language.yml", "stats.labels.knockback_kills", "Knockback eliminations"), moduleConfig.getStringFrom("language.yml", "stats.descriptions.knockback_kills", "Players knocked into the void"), StatScope.MODULE));
    }

    public void recordGamesPlayed(Collection<? extends Player> players) {
        if (statsAPI == null) {
            return;
        }
        for (Player player : players) {
            statsAPI.addModuleStat(player, moduleInfo.getId(), "games_played", 1);
        }
    }

    public void recordWin(Player player) {
        if (statsAPI == null) {
            return;
        }
        statsAPI.addModuleStat(player, moduleInfo.getId(), "wins", 1);
        statsAPI.addGlobalStat(player, "wins", 1);
    }

    public void recordKnockbackHit(Player player) {
        if (statsAPI == null) {
            return;
        }
        statsAPI.addModuleStat(player, moduleInfo.getId(), "knockback_hits", 1);
    }

    public void recordKnockbackKill(Player player) {
        if (statsAPI == null) {
            return;
        }
        statsAPI.addModuleStat(player, moduleInfo.getId(), "knockback_kills", 1);
    }
}
