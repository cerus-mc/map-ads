package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.ScreenGroup;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.Mutable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@CommandAlias("mapads")
@Subcommand("group")
@CommandPermission("mapads.command.group")
public class GroupCommand extends BaseCommand {

    @Dependency private JavaPlugin plugin;
    @Dependency private AdScreenStorage adScreenStorage;
    @Dependency private AdvertStorage advertStorage;

    @Subcommand("create")
    @CommandCompletion("group_id @mapads_group_name")
    public void handleCreate(final Player player, final String id, final String name) {
        if (this.adScreenStorage.getScreenGroup(id) != null) {
            player.sendMessage(L10n.getPrefixed("error.group_exists"));
            return;
        }

        this.adScreenStorage.updateScreenGroup(new ScreenGroup(id, name, new ArrayList<>(), Mutable.create(-1), Mutable.create(-1d), Mutable.create(null)));
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

        for (final String screenId : group.screenIds()) {
            final UUID[] adIds = this.advertStorage.getAdvertisements(screenId).stream()
                    .map(Advertisement::getAdvertId)
                    .toArray(UUID[]::new);
            if (adIds.length > 0) {
                this.advertStorage.deleteAdverts(adIds);
            }
        }
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

    @Subcommand("fixedtime set")
    @CommandCompletion("@mapads_groups @range:1-60")
    public void handleFixedTimeSet(final Player player, final String groupId, final int fixedTime) {
        final ScreenGroup group = this.adScreenStorage.getScreenGroup(groupId);
        if (group == null) {
            player.sendMessage(L10n.getPrefixed("error.group_not_found"));
            return;
        }
        if (fixedTime <= 0) {
            player.sendMessage(L10n.getPrefixed("error.value_negative"));
            return;
        }

        group.fixedTime().set(fixedTime);
        this.adScreenStorage.updateScreenGroup(group);
        player.sendMessage(L10n.getPrefixed("success.group_screen_edited", group.groupName()));
    }

    @Subcommand("fixedtime remove")
    @CommandCompletion("@mapads_groups")
    public void handleFixedTimeRemove(final Player player, final String groupId) {
        final ScreenGroup group = this.adScreenStorage.getScreenGroup(groupId);
        if (group == null) {
            player.sendMessage(L10n.getPrefixed("error.group_not_found"));
            return;
        }

        group.fixedTime().set(-1);
        this.adScreenStorage.updateScreenGroup(group);
        player.sendMessage(L10n.getPrefixed("success.group_screen_edited", group.groupName()));
    }

    @Subcommand("fixedprice set")
    @CommandCompletion("@mapads_groups 1000|5000|15000")
    public void handleFixedPriceSet(final Player player, final String groupId, final double fixedPrice) {
        final ScreenGroup group = this.adScreenStorage.getScreenGroup(groupId);
        if (group == null) {
            player.sendMessage(L10n.getPrefixed("error.group_not_found"));
            return;
        }
        if (fixedPrice <= 0) {
            player.sendMessage(L10n.getPrefixed("error.value_negative"));
            return;
        }

        group.fixedPrice().set(fixedPrice);
        this.adScreenStorage.updateScreenGroup(group);
        player.sendMessage(L10n.getPrefixed("success.group_screen_edited", group.groupName()));
    }

    @Subcommand("fixedprice remove")
    @CommandCompletion("@mapads_groups")
    public void handleFixedPriceRemove(final Player player, final String groupId) {
        final ScreenGroup group = this.adScreenStorage.getScreenGroup(groupId);
        if (group == null) {
            player.sendMessage(L10n.getPrefixed("error.group_not_found"));
            return;
        }

        group.fixedPrice().set(-1d);
        this.adScreenStorage.updateScreenGroup(group);
        player.sendMessage(L10n.getPrefixed("success.group_screen_edited", group.groupName()));
    }

    @Subcommand("beneficiary set")
    @CommandCompletion("@mapads_groups @players")
    public void handleBeneficiarySet(final Player player, final String groupId, @Flags("other") final OfflinePlayer beneficiary) {
        final ScreenGroup group = this.adScreenStorage.getScreenGroup(groupId);
        if (group == null) {
            player.sendMessage(L10n.getPrefixed("error.group_not_found"));
            return;
        }
        if (!beneficiary.hasPlayedBefore() && !beneficiary.isOnline()) {
            player.sendMessage(L10n.getPrefixed("error.player_not_found", beneficiary.getName()));
            return;
        }

        group.beneficiary().set(beneficiary.getUniqueId());
        this.adScreenStorage.updateScreenGroup(group);
        player.sendMessage(L10n.getPrefixed("success.group_screen_edited", group.groupName()));
    }

    @Subcommand("beneficiary remove")
    @CommandCompletion("@mapads_groups")
    public void handleBeneficiaryRemove(final Player player, final String groupId) {
        final ScreenGroup group = this.adScreenStorage.getScreenGroup(groupId);
        if (group == null) {
            player.sendMessage(L10n.getPrefixed("error.group_not_found"));
            return;
        }

        group.beneficiary().set(null);
        this.adScreenStorage.updateScreenGroup(group);
        player.sendMessage(L10n.getPrefixed("success.group_screen_edited", group.groupName()));
    }

}
