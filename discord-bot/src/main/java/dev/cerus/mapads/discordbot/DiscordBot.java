package dev.cerus.mapads.discordbot;

import dev.cerus.mapads.discordbot.diagnostics.Diagnosis;
import dev.cerus.mapads.discordbot.listener.ButtonInteractListener;
import dev.cerus.mapads.discordbot.listener.MessageDeleteListener;
import dev.cerus.mapads.discordbot.storage.AdvertMessageStorage;
import dev.cerus.mapads.discordbot.storage.SQLiteAdvertMessageStorage;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBot {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Config config;
    private final AdvertReviewCallback callback;
    private final SimpleDateFormat dateFormat;
    private final AdvertMessageStorage storage;
    private JDA jda;

    public DiscordBot(final Config config, final AdvertReviewCallback callback) {
        this.config = config;
        this.callback = callback;
        this.dateFormat = new SimpleDateFormat(config.dateFormat == null ? "dd.MM.yyyy HH:mm:ss" : config.dateFormat);
        this.storage = new SQLiteAdvertMessageStorage(config.dbPath);
        this.storage.loadAll();
    }

    public CompletableFuture<Void> start() {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        this.executorService.execute(() -> {
            try {
                this.jda = JDABuilder.createDefault(this.config.token, Arrays.asList(
                                GatewayIntent.GUILD_MEMBERS,
                                GatewayIntent.GUILD_MESSAGES
                        ))
                        .addEventListeners(
                                new MessageDeleteListener(this.storage),
                                new ButtonInteractListener(this.storage, this.config, this.callback)
                        )
                        .build().awaitReady();

                Activity.ActivityType type;
                try {
                    type = Activity.ActivityType.valueOf(this.config.activityType);
                } catch (final IllegalArgumentException ignored) {
                    type = Activity.ActivityType.LISTENING;
                }
                OnlineStatus status;
                try {
                    status = OnlineStatus.valueOf(this.config.onlineStatus);
                } catch (final IllegalArgumentException ignored) {
                    status = OnlineStatus.ONLINE;
                }
                final Activity activity = Activity.of(type, this.config.activityString);
                this.jda.getPresence().setPresence(status, activity);

                future.complete(null);
            } catch (final InterruptedException | LoginException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public void stop() {
        this.jda.shutdown();
        this.executorService.shutdown();
    }

    public void handleReview(final UUID adId, final boolean accepted) {
        final Long messageId = this.storage.get(adId);
        if (messageId != null) {
            this.jda.getTextChannelById(this.storage.getChannel(messageId)).deleteMessageById(messageId).queue();
            this.storage.delete(adId);
        }
    }

    public void sendAdvertCreateMessage(final AdvertContext context) {
        final MessageEmbed messageEmbed = this.createEmbed(context);
        final Message message = new MessageBuilder()
                .setEmbed(messageEmbed)
                .setActionRows(ActionRow.of(
                        Button.of(ButtonStyle.SUCCESS, "mapads-btn-accept", this.config.buttonAccept == null
                                ? "Accept" : this.replace(context, this.config.buttonAccept)),
                        Button.of(ButtonStyle.DANGER, "mapads-btn-deny", this.config.buttonDeny == null
                                ? "Deny" : this.replace(context, this.config.buttonDeny))
                ))
                .build();

        this.config.channelIds.forEach(aLong -> {
            final TextChannel channel = this.jda.getTextChannelById(aLong);
            if (channel != null && channel.getGuild().getMember(this.jda.getSelfUser()).hasPermission(channel,
                    Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)) {
                channel.sendMessage(message).queue(msg ->
                        this.storage.update(msg.getIdLong(), msg.getChannel().getIdLong(), context.getAdId()));
            }
        });
    }

    private MessageEmbed createEmbed(final AdvertContext context) {
        final EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(this.config.title == null ? "Advertisement" : this.replace(context, this.config.title));
        embedBuilder.setDescription(this.config.description == null ? "Advertisement" : this.replace(context, this.config.description));
        embedBuilder.setColor(this.config.color == null || !this.config.color.matches("#[A-Fa-f0-9]{6}")
                ? Color.GRAY : new Color(Integer.parseInt(this.config.color.substring(1), 16)));
        if (this.config.image != null) {
            embedBuilder.setImage(this.replace(context, this.config.image));
        }
        if (this.config.thumbnail != null) {
            embedBuilder.setThumbnail(this.replace(context, this.config.thumbnail));
        }
        embedBuilder.setTimestamp(Instant.now());
        embedBuilder.setFooter("Map-Ads Bot");
        return embedBuilder.build();
    }

    public CompletableFuture<Collection<Diagnosis>> runDiagnostics() {
        final CompletableFuture<Collection<Diagnosis>> future = new CompletableFuture<>();

        final Collection<CompletableFuture<?>> futures = new HashSet<>();
        final Collection<Diagnosis> diagnoses = new HashSet<>();
        for (final long channelId : this.config.channelIds) {
            final TextChannel channel = this.jda.getTextChannelById(channelId);
            if (channel == null) {
                diagnoses.add(new Diagnosis(false, "Channel '%d' was not found".formatted(channelId), null));
                continue;
            }
            final Member selfMember = channel.getGuild().getSelfMember();
            if (!selfMember.hasPermission(channel, Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ)) {
                diagnoses.add(new Diagnosis(false, "Missing permissions in channel '%d'".formatted(channelId), null));
                continue;
            }

            final CompletableFuture<?> msgFuture = new CompletableFuture<>();
            channel.sendMessage(new MessageBuilder()
                            .setContent("**This is a debug message, please ignore!**\n" +
                                    "If you don't see an embed you need to check the bot permissions.")
                            .setEmbed(this.createEmbed(new AdvertContext(
                                    new UUID(0, 0),
                                    "https://mchr.cerus.dev/v1/render/Cerus_?skin=minotar&size=256&renderer=isometric",
                                    UUID.fromString("06f8c3cc-a3c5-4b48-bc6d-d3ee8963f2af"), // Cerus_
                                    "Cerus_",
                                    0,
                                    System.currentTimeMillis(),
                                    0,
                                    "Testing"
                            )))
                            .build())
                    .queue(msg -> {
                        if (msg.getEmbeds().isEmpty() || msg.isSuppressedEmbeds()) {
                            diagnoses.add(new Diagnosis(false, "No permissions to send embeds in channel '%d'".formatted(channelId), null));
                        }
                        msgFuture.complete(null);
                    }, err -> {
                        diagnoses.add(new Diagnosis(false, "Unable to send message in channel '%d', see console for details".formatted(channelId), err));
                        msgFuture.completeExceptionally(err);
                    });
            futures.add(msgFuture);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).whenComplete(($, t) -> {
            if (diagnoses.isEmpty()) {
                diagnoses.add(new Diagnosis(true, "No issues have been detected", null));
            }
            future.complete(diagnoses);
        });

        return future;
    }

    private String replace(final AdvertContext context, final String s) {
        return s.replace("{{PLAYER_NAME}}", context.getPlayerName())
                .replace("{{PLAYER_ID}}", context.getPlayerId().toString())
                .replace("{{ADVERT_ID}}", context.getAdId().toString())
                .replace("{{IMAGE_URL}}", context.getImageUrl())
                .replace("{{TIME}}", this.dateFormat.format(new Date(context.getCreatedAt())))
                .replace("{{TIME_RAW}}", String.valueOf(context.getCreatedAt() / 1000))
                .replace("{{PRICE}}", String.valueOf(context.getPrice()))
                .replace("{{MINUTES}}", String.valueOf(context.getMinutes()))
                .replace("{{SCREEN}}", context.getScreen());
    }

    public Map<String, Map<Long, String>> getTextChannels() {
        return this.jda.getGuilds().stream()
                .map(guild -> {
                    final Map<Long, String> map = guild.getTextChannels().stream()
                            .filter(channel -> guild.getMember(this.jda.getSelfUser()).hasPermission(channel,
                                    Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                            .map(channel -> Map.entry(channel.getIdLong(), channel.getName()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    return Map.entry(guild.getName(), map);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static class Config {

        public String token;
        public String onlineStatus;
        public String activityType;
        public String activityString;
        public Set<Long> channelIds;
        public String dbPath;

        public String title;
        public String description;
        public String color;
        public String thumbnail;
        public String image;
        public String dateFormat;
        public String buttonAccept;
        public String buttonDeny;

        public Config(final String token,
                      final String onlineStatus,
                      final String activityType,
                      final String activityString,
                      final Set<Long> channelIds,
                      final String dbPath) {
            this.token = token;
            this.onlineStatus = onlineStatus;
            this.activityType = activityType;
            this.activityString = activityString;
            this.channelIds = channelIds;
            this.dbPath = dbPath;
        }

    }

}
