package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.ConfigModel;
import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.gui.AdScreenListGui;
import dev.cerus.mapads.image.ImageConverter;
import dev.cerus.mapads.image.ImageRetriever;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.FormatUtil;
import dev.cerus.mapads.util.ReviewerUtil;
import dev.cerus.mapads.util.UrlVerificationUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.jodah.expiringmap.ExpiringMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@CommandAlias("mapads")
@Subcommand("preview")
@CommandPermission("mapads.command.preview")
public class PreviewCommand extends BaseCommand {

    private final Map<UUID, Long> cooldownMap = ExpiringMap.builder()
            .expiration(5, TimeUnit.MINUTES)
            .build();

    @Dependency
    private ImageRetriever imageRetriever;

    @Dependency
    private ImageConverter imageConverter;

    @Dependency
    private ConfigModel config;

    @Dependency
    private AdScreenStorage adScreenStorage;

    @Default
    @CommandCompletion("none|floyd_steinberg @nothing")
    public void handle(final Player player, final String ditherStr, final String imageUrl) {
        if (this.cooldownMap.containsKey(player.getUniqueId())) {
            final long rem = this.cooldownMap.get(player.getUniqueId()) - System.currentTimeMillis();
            final long m = (rem / 1000) / 60;
            final long s = (rem / 1000) % 60;
            player.sendMessage(L10n.getPrefixed("error.cooldown", m, s));
            return;
        }

        final URI uri;
        try {
            uri = URI.create(imageUrl);
        } catch (final IllegalArgumentException ignored) {
            player.sendMessage(L10n.getPrefixed("error.invalid_url"));
            return;
        }

        final String host = uri.getHost();
        if (host == null) {
            player.sendMessage(L10n.getPrefixed("error.invalid_url"));
            return;
        }

        if (!UrlVerificationUtil.verify(host, this.config)) {
            player.sendMessage(L10n.getPrefixed("error.untrusted_site"));
            return;
        }

        final ImageConverter.Dither dither;
        try {
            dither = ImageConverter.Dither.valueOf(ditherStr.toUpperCase());
        } catch (final Exception ignored) {
            player.sendMessage(L10n.getPrefixed("error.invalid_dither", Arrays.stream(ImageConverter.Dither.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .collect(Collectors.joining(", "))));
            return;
        }

        this.cooldownMap.put(player.getUniqueId(), System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));

        player.sendMessage(L10n.getPrefixed("misc.please_wait"));
        this.imageRetriever.getImageAsync(imageUrl, this.config.maxImageSize).whenComplete((result, throwable) -> {
            if (result.getErr() != null) {
                player.sendMessage(L10n.getPrefixed("error.failed_image", result.getErr().getMessage()));
                return;
            }
            if (!result.isImage()) {
                if (result.getType() == null) {
                    player.sendMessage(L10n.getPrefixed("error.not_image"));
                } else if (result.getType().startsWith("image")) {
                    player.sendMessage(L10n.getPrefixed("error.content_unsupported_image"));
                } else {
                    player.sendMessage(L10n.getPrefixed("error.content_not_image"));
                }
                return;
            }
            final BufferedImage image = result.getImage();
            if (image == null) {
                player.sendMessage(L10n.getPrefixed("error.image_big",
                        FormatUtil.formatSize(result.getLen()),
                        FormatUtil.formatSize(this.config.maxImageSize)));
                return;
            }

            final Predicate<AdScreen> predicate = adScreen -> {
                final MapScreen mapScreen = MapScreenRegistry.getScreen(adScreen.getScreenId());
                return mapScreen != null
                        && mapScreen.getWidth() * 128 == image.getWidth()
                        && mapScreen.getHeight() * 128 == image.getHeight();
            };

            if (this.adScreenStorage.getScreens().stream().noneMatch(predicate)) {
                player.sendMessage(L10n.getPrefixed("error.no_screens"));
                return;
            }

            final AdScreenListGui gui = new AdScreenListGui(player, this.adScreenStorage, adScreen -> {
                player.closeInventory();
                player.sendMessage(L10n.getPrefixed("misc.please_wait"));

                final MapScreen mapScreen = MapScreenRegistry.getScreen(adScreen.getScreenId());
                this.imageConverter.convertAsync(image, dither).whenComplete((mapImage, throwable_) -> {
                    ReviewerUtil.markAsReviewer(player, mapImage, mapScreen);
                    ReviewerUtil.sendImage(player, mapScreen, mapImage);

                    player.sendMessage(L10n.getPrefixed("success.preview", adScreen.getId()));
                });
            }, predicate);
            Bukkit.getScheduler().runTask(JavaPlugin.getPlugin(MapAdsPlugin.class), gui::open);
        });
    }

}
