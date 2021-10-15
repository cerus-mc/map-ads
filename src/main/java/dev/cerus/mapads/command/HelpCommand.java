package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.helpbook.HelpBook;
import org.bukkit.entity.Player;

@CommandAlias("mapads")
@Subcommand("help")
public class HelpCommand extends BaseCommand {

    @Default
    public void handle(final Player player) {
        HelpBook.open(player);
    }

}
