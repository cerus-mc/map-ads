package dev.cerus.mapads;

import co.aikar.commands.BukkitCommandManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.cerus.mapads.advert.AdvertController;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.advert.storage.MySqlAdvertStorageImpl;
import dev.cerus.mapads.advert.storage.SqliteAdvertStorageImpl;
import dev.cerus.mapads.api.event.AdvertCreateEvent;
import dev.cerus.mapads.api.event.AdvertReviewEvent;
import dev.cerus.mapads.command.DefaultImageCommand;
import dev.cerus.mapads.command.HelpCommand;
import dev.cerus.mapads.command.MapAdsCommand;
import dev.cerus.mapads.command.PremiumCommand;
import dev.cerus.mapads.command.PreviewCommand;
import dev.cerus.mapads.command.ReviewCommand;
import dev.cerus.mapads.command.ScreenCommand;
import dev.cerus.mapads.helpbook.HelpBook;
import dev.cerus.mapads.helpbook.HelpBookConfiguration;
import dev.cerus.mapads.image.DefaultImageController;
import dev.cerus.mapads.image.ImageConverter;
import dev.cerus.mapads.image.ImageRetriever;
import dev.cerus.mapads.image.MapImage;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.image.storage.MySqlImageStorageImpl;
import dev.cerus.mapads.image.storage.SqliteImageStorageImpl;
import dev.cerus.mapads.image.transition.TransitionRegistry;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.listener.PlayerJoinListener;
import dev.cerus.mapads.premium.Premium;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.screen.storage.YamlAdScreenStorageImpl;
import dev.cerus.mapads.task.FrameSendTask;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class MapAdsPlugin extends JavaPlugin {

    private static boolean enabled = false;

    private final Set<AutoCloseable> closeables = new HashSet<>();
    private ConfigModel configModel;
    private BiFunction<Integer, Integer, MapImage> defaultImageSupplier;
    private boolean screensLoaded;

    @Override
    public void onEnable() {
        if (enabled) {
            this.getLogger().severe("DO NOT RELOAD YOUR SERVER.");
            this.getLogger().severe("This plugin will probably not work correctly now. Disabling.");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        enabled = true;

        Premium.init();
        this.closeables.add(TransitionRegistry::cleanup);

        this.saveDefaultConfig();
        this.configModel = new ConfigModel(this.getConfig());

        this.saveResource("lang.yml", false);
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "lang.yml"));
        for (final String key : configuration.getKeys(false)) {
            final Object o = configuration.get(key);
            if (o instanceof List) {
                L10n.put(key.replace(",", "."), ((List<String>) o).stream()
                        .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                        .collect(Collectors.toList()));
            } else if (o instanceof String) {
                L10n.put(key.replace(",", "."), ChatColor.translateAlternateColorCodes('&', (String) o));
            } else {
                this.getLogger().warning("Invalid lang mapping: " + key + "->" + o.getClass().getName());
            }
        }

        this.saveResource("helpbook.yml", false);
        final HelpBookConfiguration helpBookConfiguration = new HelpBookConfiguration();
        helpBookConfiguration.load();
        HelpBook.init(helpBookConfiguration);

        final RegisteredServiceProvider<Economy> registration = this.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            this.getLogger().severe("Please install an economy plugin!");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        final Economy economy = registration.getProvider();

        final ImageStorage imageStorage = this.loadImageStorage();
        if (imageStorage == null) {
            this.getLogger().severe("Invalid image storage configuration");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        this.closeables.add(imageStorage);

        final AdvertStorage advertStorage = this.loadAdvertStorage(imageStorage);
        if (advertStorage == null) {
            this.getLogger().severe("Invalid advert storage configuration");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        this.closeables.add(advertStorage);

        final AdScreenStorage adScreenStorage = this.loadAdScreenStorage();
        this.closeables.add(adScreenStorage);

        final DefaultImageController defaultImageRetriever = new DefaultImageController(this, imageStorage);
        this.defaultImageSupplier = defaultImageRetriever::getDefaultImage;

        final AdvertController advertController = new AdvertController(this, advertStorage, imageStorage);
        final ImageRetriever imageRetriever = new ImageRetriever();
        final ImageConverter imageConverter = new ImageConverter();
        final BukkitCommandManager commandManager = new BukkitCommandManager(this);
        commandManager.getCommandCompletions().registerCompletion("mapads_names", context ->
                adScreenStorage.getScreens().stream()
                        .map(AdScreen::getId)
                        .collect(Collectors.toList()));
        commandManager.getCommandCompletions().registerCompletion("mapads_transitions", context ->
                TransitionRegistry.names());
        commandManager.getCommandCompletions().registerCompletion("mapads_adverts", context ->
                advertStorage.getPendingAdvertisements().stream()
                        .map(advertisement -> advertisement.getAdvertId().toString())
                        .collect(Collectors.toSet()));
        commandManager.getCommandCompletions().registerCompletion("maps_ids", context ->
                MapScreenRegistry.getScreenIds().stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList()));
        commandManager.getCommandCompletions().registerCompletion("mapads_commondim", context -> {
            final List<String> list = new ArrayList<>();
            for (int x = 1; x <= 20; x++) {
                for (int y = 1; y <= 20; y++) {
                    list.add(x + "x" + y);
                }
            }
            return list;
        });
        commandManager.registerDependency(ImageStorage.class, imageStorage);
        commandManager.registerDependency(AdvertStorage.class, advertStorage);
        commandManager.registerDependency(AdScreenStorage.class, adScreenStorage);
        commandManager.registerDependency(ImageRetriever.class, imageRetriever);
        commandManager.registerDependency(ImageConverter.class, imageConverter);
        commandManager.registerDependency(DefaultImageController.class, defaultImageRetriever);
        commandManager.registerDependency(AdvertController.class, advertController);
        commandManager.registerDependency(ConfigModel.class, this.configModel);
        commandManager.registerDependency(Economy.class, economy);
        commandManager.registerCommand(new MapAdsCommand());
        commandManager.registerCommand(new PremiumCommand());
        commandManager.registerCommand(new ScreenCommand());
        commandManager.registerCommand(new DefaultImageCommand());
        commandManager.registerCommand(new ReviewCommand());
        commandManager.registerCommand(new HelpCommand());
        commandManager.registerCommand(new PreviewCommand());

        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, adScreenStorage, advertStorage), this);

        this.getServer().getScheduler().runTaskTimerAsynchronously(this, new FrameSendTask(adScreenStorage), 4 * 20, 2 * 20);
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> adScreenStorage.getScreens().forEach(advertController::update), 4 * 20, 60 * 20);
        this.getServer().getScheduler().runTaskLater(this, () -> this.screensLoaded = true, 4 * 20);

        this.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void handle(final AdvertCreateEvent event) {
                event.setCancelled(true);
            }

            @EventHandler
            public void handle(final AdvertReviewEvent event) {
                event.setCancelled(true);
            }
        }, this);
    }

    @Override
    public void onDisable() {
        for (final AutoCloseable closeable : this.closeables) {
            try {
                closeable.close();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    private AdScreenStorage loadAdScreenStorage() {
        final File file = new File(this.getDataFolder(), "screens.yml");
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        return new YamlAdScreenStorageImpl(file, configuration);
    }

    private ImageStorage loadImageStorage() {
        final FileConfiguration config = this.getConfig();
        switch (config.getString("image-storage.type", "sqlite").toLowerCase()) {
            case "mysql":
                final String mysqlHost = config.getString("image-storage.mysql.host");
                final int mysqlPort = config.getInt("image-storage.mysql.port");
                final String mysqlDb = config.getString("image-storage.mysql.db");
                final String mysqlUser = config.getString("image-storage.mysql.user");
                final String mysqlPass = config.getString("image-storage.mysql.pass");

                final HikariConfig mysqlHikariConfig = new HikariConfig();
                mysqlHikariConfig.setDriverClassName(org.mariadb.jdbc.Driver.class.getName());
                mysqlHikariConfig.setJdbcUrl("jdbc:mysql://" + mysqlUser + ":" + mysqlPass + "@" + mysqlHost + ":" + mysqlPort + "/" + mysqlDb);
                return new MySqlImageStorageImpl(new HikariDataSource(mysqlHikariConfig));
            case "sqlite":
                final String dbName = config.getString("image-storage.sqlite.db-name");

                final HikariConfig sqliteHikariConfig = new HikariConfig();
                sqliteHikariConfig.setDriverClassName(org.sqlite.JDBC.class.getName());
                sqliteHikariConfig.setJdbcUrl("jdbc:sqlite:" + this.getDataFolder().getPath() + "/" + dbName);
                return new SqliteImageStorageImpl(new HikariDataSource(sqliteHikariConfig));
            default:
                return null;
        }
    }

    private AdvertStorage loadAdvertStorage(final ImageStorage imageStorage) {
        final FileConfiguration config = this.getConfig();
        switch (config.getString("advert-storage.type", "sqlite").toLowerCase()) {
            case "mysql":
                final String mysqlHost = config.getString("advert-storage.mysql.host");
                final int mysqlPort = config.getInt("advert-storage.mysql.port");
                final String mysqlDb = config.getString("advert-storage.mysql.db");
                final String mysqlUser = config.getString("advert-storage.mysql.user");
                final String mysqlPass = config.getString("advert-storage.mysql.pass");

                final HikariConfig mysqlHikariConfig = new HikariConfig();
                mysqlHikariConfig.setDriverClassName(org.mariadb.jdbc.Driver.class.getName());
                mysqlHikariConfig.setJdbcUrl("jdbc:mysql://" + mysqlUser + ":" + mysqlPass + "@" + mysqlHost + ":" + mysqlPort + "/" + mysqlDb);
                return new MySqlAdvertStorageImpl(new HikariDataSource(mysqlHikariConfig), imageStorage);
            case "sqlite":
                final String dbName = config.getString("advert-storage.sqlite.db-name");

                final HikariConfig sqliteHikariConfig = new HikariConfig();
                sqliteHikariConfig.setDriverClassName(org.sqlite.JDBC.class.getName());
                sqliteHikariConfig.setJdbcUrl("jdbc:sqlite:" + this.getDataFolder().getPath() + "/" + dbName);
                return new SqliteAdvertStorageImpl(new HikariDataSource(sqliteHikariConfig), imageStorage);
            default:
                return null;
        }
    }

    public ConfigModel getConfigModel() {
        return this.configModel;
    }

    public BiFunction<Integer, Integer, MapImage> getDefaultImageSupplier() {
        return this.defaultImageSupplier;
    }

    public boolean areScreensLoaded() {
        return this.screensLoaded;
    }

}
