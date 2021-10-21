package dev.cerus.mapads.discordbot.listener;

import dev.cerus.mapads.discordbot.AdvertReviewCallback;
import dev.cerus.mapads.discordbot.DiscordBot;
import dev.cerus.mapads.discordbot.storage.AdvertMessageStorage;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonInteraction;
import org.jetbrains.annotations.NotNull;

public class ButtonInteractListener extends ListenerAdapter {

    private final AdvertMessageStorage storage;
    private final DiscordBot.Config config;
    private final AdvertReviewCallback callback;

    public ButtonInteractListener(final AdvertMessageStorage storage, final DiscordBot.Config config, final AdvertReviewCallback callback) {
        this.storage = storage;
        this.config = config;
        this.callback = callback;
    }

    @Override
    public void onButtonClick(@NotNull final ButtonClickEvent event) {
        final ButtonInteraction interaction = event.getInteraction();
        final Button button = event.getButton();
        if (!button.getId().startsWith("mapads")) {
            return;
        }
        if (!this.config.channelIds.contains(interaction.getChannel().getIdLong())) {
            return;
        }
        if (!interaction.isFromGuild()) {
            return;
        }

        final Member member = interaction.getMember();
        final TextChannel channel = interaction.getTextChannel();
        if (!member.hasPermission(channel, Permission.MESSAGE_MANAGE)) {
            return;
        }

        final UUID adId = this.storage.get(interaction.getMessageIdLong());
        if (adId == null) {
            return;
        }

        final CompletableFuture<Boolean> future;
        if (button.getId().equals("mapads-btn-accept")) {
            future = this.callback.apply(adId, true);
        } else if (button.getId().equals("mapads-btn-deny")) {
            future = this.callback.apply(adId, false);
        } else {
            return;
        }

        interaction.deferReply(true).queue();
        future.whenComplete((aBoolean, throwable) -> {
            if (aBoolean) {
                interaction.getHook().editOriginal("Cancelled").queue();
                return;
            }
            interaction.getHook().editOriginal("Ok").queue();
            this.storage.delete(interaction.getMessageIdLong());
            interaction.getMessage().delete().queue();
        });
    }

}
