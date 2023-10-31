package dev.cerus.mapads.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Subcommand;
import dev.cerus.mapads.MapAdsPlugin;
import dev.cerus.mapads.image.DefaultImageController;
import dev.cerus.mapads.image.ImageConverter;
import dev.cerus.mapads.image.ImageRetriever;
import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.util.FormatUtil;
import dev.cerus.mapads.util.UrlVerificationUtil;
import dev.cerus.maps.api.MapScreen;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@CommandAlias("mapads")
@Subcommand("defaultimage")
@CommandPermission("mapads.command.defaultimage")
public class DefaultImageCommand extends BaseCommand {

    private static final int W_MAX = 20;
    private static final int H_MAX = 20;

    @Dependency private ImageStorage imageStorage;
    @Dependency private ImageRetriever imageRetriever;
    @Dependency private ImageConverter imageConverter;
    @Dependency private DefaultImageController defaultImageController;
    @Dependency private AdScreenStorage adScreenStorage;
    @Dependency private MapAdsPlugin plugin;

    @Subcommand("remove")
    @CommandCompletion("@mapads_commondim|@mapads_names")
    public void handleDefaultImageRemove(final Player player, final String targetArg) {
        if (!targetArg.matches("\\d+x\\d+") && this.adScreenStorage.getAdScreen(targetArg) == null) {
            player.sendMessage(L10n.get("error.expected_screen_or_size"));
            return;
        }

        final String key;
        if (this.adScreenStorage.getAdScreen(targetArg) == null) {
            final String[] strings = targetArg.split("x");
            final int width = Integer.parseInt(strings[0]);
            final int height = Integer.parseInt(strings[1]);
            if (width >= W_MAX || height >= H_MAX) {
                player.sendMessage(L10n.getPrefixed("error.dimension_big"));
                return;
            }
            key = (width * 128) + "x" + (height * 128);
        } else {
            final AdScreen adScreen = this.adScreenStorage.getAdScreen(targetArg);
            key = adScreen.getId();
        }

        this.defaultImageController.removeDefaultImage(key);
        player.sendMessage(L10n.getPrefixed("success.def_img_removed", key));
    }

    @Subcommand("set")
    @CommandCompletion("@mapads_commondim|@mapads_names none|floyd_steinberg @nothing")
    public void handleDefaultImageSet(final Player player, final String targetArg, final String ditherStr, final String imageUrl) {
        if (!targetArg.matches("\\d+x\\d+") && this.adScreenStorage.getAdScreen(targetArg) == null) {
            player.sendMessage(L10n.get("error.expected_screen_or_size"));
            return;
        }

        final int requiredWidth;
        final int requiredHeight;
        final String key;
        if (this.adScreenStorage.getAdScreen(targetArg) == null) {
            final String[] strings = targetArg.split("x");
            final int width = Integer.parseInt(strings[0]);
            final int height = Integer.parseInt(strings[1]);
            if (width >= W_MAX || height >= H_MAX) {
                player.sendMessage(L10n.getPrefixed("error.dimension_big"));
                return;
            }
            requiredWidth = width;
            requiredHeight = height;
            key = (width * 128) + "x" + (height * 128);
        } else {
            final AdScreen adScreen = this.adScreenStorage.getAdScreen(targetArg);
            final MapScreen mapScreen = MapScreenRegistry.getScreen(adScreen.getScreenId());
            requiredWidth = mapScreen.getWidth();
            requiredHeight = mapScreen.getHeight();
            key = adScreen.getId();
        }

        final URI uri = URI.create(imageUrl);
        if (uri.getHost() == null) {
            player.sendMessage(L10n.get("error.invalid_url"));
            return;
        }
        if (!UrlVerificationUtil.verify(uri.getHost(), this.plugin.getConfigModel())) {
            player.sendMessage(L10n.get("error.untrusted_site"));
            return;
        }
        final ImageConverter.Dither dither;
        try {
            dither = ImageConverter.Dither.valueOf(ditherStr.toUpperCase());
        } catch (final Exception ignored) {
            player.sendMessage(L10n.get("error.invalid_dither", Arrays.stream(ImageConverter.Dither.values())
                    .map(Enum::name)
                    .map(String::toLowerCase)
                    .collect(Collectors.joining(", "))));
            return;
        }
        player.sendMessage(L10n.get("misc.please_wait"));
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            final ImageRetriever.Result result = this.imageRetriever.getImage(imageUrl, this.plugin.getConfigModel().maxImageSize);
            if (result.getErr() != null) {
                player.sendMessage(L10n.getPrefixed("error.failed_image", result.getErr().getMessage()));
                return;
            }
            final BufferedImage image = result.getImage();
            if (image == null) {
                player.sendMessage(L10n.getPrefixed("error.image_big",
                        FormatUtil.formatSize(result.getLen()),
                        FormatUtil.formatSize(this.plugin.getConfigModel().maxImageSize)));
                return;
            }
            if (image.getWidth() % 128 != 0 || image.getHeight() % 128 != 0
                || image.getWidth() / 128 != requiredWidth || image.getHeight() / 128 != requiredHeight) {
                player.sendMessage(L10n.getPrefixed("error.image_dimensions", requiredWidth * 128,
                        requiredHeight * 128, image.getWidth(), image.getHeight()));
                return;
            }
            final MapImage converted = this.imageConverter.convert(image, dither);
            this.imageStorage.updateMapImage(converted);
            this.defaultImageController.setDefaultImage(key, converted);
            player.sendMessage(L10n.getPrefixed("success.def_img_changed_new", key));
        });
    }

}
