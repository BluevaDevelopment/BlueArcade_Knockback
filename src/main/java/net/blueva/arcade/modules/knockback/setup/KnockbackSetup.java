package net.blueva.arcade.modules.knockback.setup;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.blueva.arcade.api.setup.GameSetupHandler;
import net.blueva.arcade.api.setup.SetupContext;
import net.blueva.arcade.api.setup.SetupDataAPI;
import net.blueva.arcade.api.setup.TabCompleteContext;
import net.blueva.arcade.api.setup.TabCompleteResult;
import net.blueva.arcade.modules.knockback.KnockbackModule;

import java.util.Arrays;
import java.util.List;

public class KnockbackSetup implements GameSetupHandler {

    private final KnockbackModule module;

    public KnockbackSetup(KnockbackModule module) {
        this.module = module;
    }

    @Override
    public boolean handle(SetupContext context) {
        return handleInternal(castSetupContext(context));
    }

    private boolean handleInternal(SetupContext<Player, CommandSender, Location> context) {
        if (!context.hasHandlerArgs(1)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_setmode"));
            return true;
        }

        String subcommand = context.getArg(context.getStartIndex() - 1).toLowerCase();

        if ("setmode".equals(subcommand)) {
            return handleSetMode(context);
        }

        context.getMessagesAPI().sendRaw(context.getPlayer(),
                module.getCoreConfig().getLanguage("admin_commands.errors.unknown_subcommand"));
        return true;
    }

    @Override
    public TabCompleteResult tabComplete(TabCompleteContext context) {
        return tabCompleteInternal(castTabContext(context));
    }

    private TabCompleteResult tabCompleteInternal(TabCompleteContext<Player, CommandSender> context) {
        int relIndex = context.getRelativeArgIndex();

        if (relIndex == 0 && "setmode".equals(context.getArg(context.getStartIndex() - 1))) {
            return TabCompleteResult.of("last_standing", "most_kills");
        }

        return TabCompleteResult.empty();
    }

    @Override
    public List<String> getSubcommands() {
        return Arrays.asList("setmode");
    }

    @Override
    public boolean validateConfig(SetupContext context) {
        return validateConfigInternal(castSetupContext(context));
    }

    private boolean validateConfigInternal(SetupContext<Player, CommandSender, Location> context) {
        SetupDataAPI data = context.getData();
        if (!data.has("basic.win_mode")) {
            data.setString("basic.win_mode", "last_standing");
            data.save();
        }

        return true;
    }

    private boolean handleSetMode(SetupContext<Player, CommandSender, Location> context) {
        if (!context.hasHandlerArgs(1)) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_setmode"));
            return true;
        }

        String mode = context.getHandlerArg(0).toLowerCase();
        if (!mode.equals("last_standing") && !mode.equals("most_kills")) {
            context.getMessagesAPI().sendRaw(context.getPlayer(),
                    module.getModuleConfig().getStringFrom("language.yml", "setup_messages.usage_setmode"));
            return true;
        }

        context.getData().setString("basic.win_mode", mode);
        context.getData().save();

        context.getMessagesAPI().sendRaw(context.getPlayer(),
                module.getModuleConfig().getStringFrom("language.yml", "setup_messages.mode_set")
                        .replace("{mode}", mode));
        return true;
    }


    @SuppressWarnings("unchecked")
    private SetupContext<Player, CommandSender, Location> castSetupContext(SetupContext context) {
        return (SetupContext<Player, CommandSender, Location>) context;
    }

    @SuppressWarnings("unchecked")
    private TabCompleteContext<Player, CommandSender> castTabContext(TabCompleteContext context) {
        return (TabCompleteContext<Player, CommandSender>) context;
    }
}
