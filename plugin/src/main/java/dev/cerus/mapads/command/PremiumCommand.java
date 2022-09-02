package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.premium.Premium;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@CommandAlias("mapads")
@Subcommand("premium")
@CommandPermission("mapads.command.premium")
public class PremiumCommand extends BaseCommand {

    @Dependency
    private JavaPlugin plugin;

    @Default
    public void handle(final Player player) {
        player.sendMessage("§6§lM§e§lap§6§lA§e§lds §8v" + this.plugin.getDescription().getVersion());
        if (!Premium.isPremium()) {
            player.sendMessage("§7This is not a premium version");
        } else {
            player.spigot().sendMessage(new ComponentBuilder("§dDownload-ID: §7" + Premium.getNonce())
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Click to copy Download-ID")))
                    .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(Premium.getNonce())))
                    .create());
            player.spigot().sendMessage(new ComponentBuilder("§dID: §7" + Premium.to64BitIdentifier())
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7Click to copy ID")))
                    .event(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(Premium.to64BitIdentifier())))
                    .create());
        }
    }

}
