package dev.cerus.mapads.discordbot.listener;

import dev.cerus.mapads.discordbot.storage.AdvertMessageStorage;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MessageDeleteListener extends ListenerAdapter {

    private final AdvertMessageStorage storage;

    public MessageDeleteListener(final AdvertMessageStorage storage) {
        this.storage = storage;
    }

    @Override
    public void onGuildMessageDelete(@NotNull final GuildMessageDeleteEvent event) {
        if (this.storage.get(event.getMessageIdLong()) != null) {
            this.storage.delete(event.getMessageIdLong());
        }
    }

}
