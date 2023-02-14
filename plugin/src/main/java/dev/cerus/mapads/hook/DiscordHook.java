package dev.cerus.mapads.hook;

import dev.cerus.mapads.advert.Advertisement;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.api.event.AdvertCreateEvent;
import dev.cerus.mapads.api.event.AdvertReviewEvent;
import dev.cerus.mapads.discordbot.AdvertContext;
import dev.cerus.mapads.discordbot.AdvertReviewCallback;
import dev.cerus.mapads.discordbot.DiscordBot;
import dev.cerus.mapads.discordbot.diagnostics.Diagnosis;
import dev.cerus.mapads.economy.EconomyWrapper;
import dev.cerus.mapads.image.storage.ImageStorage;
import java.io.File;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public class DiscordHook {

    private final JavaPlugin plugin;
    private final AdvertStorage advertStorage;
    private final ImageStorage imageStorage;
    private final EconomyWrapper<?> economy;
    private DiscordBot discordBot;

    public DiscordHook(final JavaPlugin plugin,
                       final AdvertStorage advertStorage,
                       final ImageStorage imageStorage,
                       final EconomyWrapper<?> economy) {
        this.plugin = plugin;
        this.advertStorage = advertStorage;
        this.imageStorage = imageStorage;
        this.economy = economy;
    }

    public AutoCloseable load() {
        this.plugin.saveResource("discord.yml", false);
        final File file = new File(this.plugin.getDataFolder(), "discord.yml");
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        if (!configuration.getBoolean("enable")) {
            return null;
        }

        final DiscordBot.Config config = new DiscordBot.Config(
                configuration.getString("token"),
                configuration.getString("activity.online-status"),
                configuration.getString("activity.type"),
                configuration.getString("activity.text"),
                configuration.getStringList("channel-ids").stream()
                        .map(Long::parseLong)
                        .collect(Collectors.toSet()),
                this.plugin.getDataFolder().getPath() + "/" + configuration.getString("storage.sqlite.path")
        );
        config.dateFormat = configuration.getString("time-format", null);
        config.title = configuration.getString("message.title", null);
        config.description = configuration.getString("message.description", null);
        config.color = configuration.getString("message.color", null);
        config.image = configuration.getString("message.image", null);
        config.thumbnail = configuration.getString("message.thumbnail", null);
        config.buttonAccept = configuration.getString("message.button.accept", null);
        config.buttonDeny = configuration.getString("message.button.deny", null);

        this.discordBot = new DiscordBot(config, this.getAdvertCallback());
        this.discordBot.start();

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST)
            public void handleNewAd(final AdvertCreateEvent event) {
                if (!event.isCancelled()) {
                    final Advertisement advertisement = event.getAdvertisement();
                    DiscordHook.this.discordBot.sendAdvertCreateMessage(new AdvertContext(
                            advertisement.getAdvertId(),
                            event.getImageUrl(),
                            event.getPlayer().getUniqueId(),
                            event.getPlayer().getName(),
                            advertisement.getPricePaid(),
                            advertisement.getPurchaseTimestamp(),
                            advertisement.getPurchasedMinutes(),
                            advertisement.getScreenOrGroupId().map(
                                    s -> s,
                                    s -> "(Group) " + s
                            )
                    ));
                }
            }

            @EventHandler(priority = EventPriority.HIGHEST)
            public void handleReview(final AdvertReviewEvent event) {
                if (!event.isCancelled() && !event.isDiscord()) {
                    DiscordHook.this.discordBot.handleReview(event.getAdvertisement().getAdvertId(), event.getResult() == AdvertReviewEvent.Result.ACCEPT);
                }
            }
        }, this.plugin);

        return this.discordBot::stop;
    }

    public CompletableFuture<Collection<Diagnosis>> runDiagnostics() {
        return this.discordBot.runDiagnostics();
    }

    private AdvertReviewCallback getAdvertCallback() {
        return (adId, accepted) -> {
            final CompletableFuture<Boolean> future = new CompletableFuture<>();
            this.advertStorage.getPendingAdvertisements().stream()
                    .filter(advertisement -> advertisement.getAdvertId().equals(adId))
                    .findAny()
                    .ifPresentOrElse(advertisement -> {
                        Bukkit.getScheduler().runTask(this.plugin, () -> {
                            final AdvertReviewEvent event = new AdvertReviewEvent(null, advertisement, accepted
                                    ? AdvertReviewEvent.Result.ACCEPT : AdvertReviewEvent.Result.DENY);
                            Bukkit.getPluginManager().callEvent(event);
                            if (event.isCancelled()) {
                                future.complete(true);
                                return;
                            }

                            if (accepted) {
                                advertisement.setReviewed(true);
                                this.advertStorage.updateAdvert(advertisement);
                            } else {
                                this.imageStorage.deleteMapImages(advertisement.getImageId());
                                this.advertStorage.deleteAdverts(advertisement.getAdvertId());
                                this.economy.deposit(Bukkit.getOfflinePlayer(advertisement.getPlayerUuid()), advertisement.getPricePaid());
                            }
                            future.complete(false);
                        });
                    }, () -> future.complete(false));
            return future;
        };
    }

}
