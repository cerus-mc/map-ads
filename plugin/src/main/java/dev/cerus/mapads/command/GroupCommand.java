package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.ScreenGroup;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@CommandAlias("mapads")
@Subcommand("group")
@CommandPermission("mapads.command.group")
public class GroupCommand extends BaseCommand {

    @Dependency
    private JavaPlugin plugin;

    @Dependency
    private AdScreenStorage adScreenStorage;

    @Subcommand("create")
    @CommandCompletion("group_id @mapads_group_name")
    public void handleCreate(final Player player, final String id, final String name) {
        if (this.adScreenStorage.getScreenGroup(id) != null) {
            player.sendMessage(L10n.getPrefixed("error.group_exists"));
            return;
        }

        this.adScreenStorage.updateScreenGroup(new ScreenGroup(id, name, new ArrayList<>()));
        player.sendMessage(L10n.getPrefixed("success.group_created", name, id));
    }

    @Subcommand("remove")
    @CommandCompletion("@mapads_groups")
    public void handleRemove(final Player player, final String id) {
        if (this.adScreenStorage.getScreenGroup(id) == null) {
            player.sendMessage(L10n.getPrefixed("error.group_not_found"));
            return;
        }

        final ScreenGroup group = this.adScreenStorage.getScreenGroup(id);
        this.adScreenStorage.deleteScreenGroup(group);
        player.sendMessage(L10n.getPrefixed("success.group_deleted", group.groupName(), group.id()));
    }

    @Subcommand("list")
    public void handleList(final Player player) {
        final List<ScreenGroup> groups = this.adScreenStorage.getScreenGroups();
        player.sendMessage(L10n.get("prefix") + "§7" + groups.size() + " groups");
        for (final ScreenGroup group : groups) {
            player.sendMessage(L10n.get("prefix") + "§7" + group.groupName() + " §8[" + group.id()
                    + "]§7: §f" + String.join(", ", group.screenIds()));
        }
    }

    @Subcommand("screen add")
    @CommandCompletion("@mapads_groups @mapads_names")
    public void handleScreenAdd(final Player player, final String groupId, final String screenId) {
        final ScreenGroup group = this.adScreenStorage.getScreenGroup(groupId);
        if (group == null) {
            player.sendMessage(L10n.getPrefixed("error.group_not_found"));
            return;
        }
        if (this.adScreenStorage.getAdScreen(screenId) == null) {
            player.sendMessage(L10n.getPrefixed("error.screen_not_found"));
            return;
        }
        if (group.screenIds().contains(screenId)) {
            player.sendMessage(L10n.getPrefixed("error.group_contains"));
            return;
        }

        group.screenIds().add(screenId);
        this.adScreenStorage.updateScreenGroup(group);
        player.sendMessage(L10n.getPrefixed("success.group_screen_added", screenId, group.groupName()));
    }

    @Subcommand("screen remove")
    @CommandCompletion("@mapads_groups @mapads_names")
    public void handleScreenRemove(final Player player, final String groupId, final String screenId) {
        final ScreenGroup group = this.adScreenStorage.getScreenGroup(groupId);
        if (group == null) {
            player.sendMessage(L10n.getPrefixed("error.group_not_found"));
            return;
        }
        if (!group.screenIds().contains(screenId)) {
            player.sendMessage(L10n.getPrefixed("error.group_not_contains"));
            return;
        }

        group.screenIds().remove(screenId);
        this.adScreenStorage.updateScreenGroup(group);
        player.sendMessage(L10n.getPrefixed("success.group_screen_removed", screenId, group.groupName()));
    }

}
