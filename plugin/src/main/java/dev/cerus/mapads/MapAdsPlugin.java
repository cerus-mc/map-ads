package dev.cerus.mapads;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.CommandCompletions;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.cerus.mapads.advert.AdvertController;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.advert.storage.MySqlAdvertStorageImpl;
import dev.cerus.mapads.advert.storage.SqliteAdvertStorageImpl;
import dev.cerus.mapads.command.DefaultImageCommand;
import dev.cerus.mapads.command.HelpCommand;
import dev.cerus.mapads.command.MapAdsCommand;
import dev.cerus.mapads.command.PremiumCommand;
import dev.cerus.mapads.command.PreviewCommand;
import dev.cerus.mapads.command.ReviewCommand;
import dev.cerus.mapads.command.ScreenCommand;
import dev.cerus.mapads.helpbook.HelpBook;
import dev.cerus.mapads.helpbook.HelpBookConfiguration;
import dev.cerus.mapads.image.ColorCache;
import dev.cerus.mapads.image.DefaultImageController;
import dev.cerus.mapads.image.ImageConverter;
import dev.cerus.mapads.image.ImageRetriever;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

public class MapAdsPlugin extends JavaPlugin {

    private static boolean enabled = false;

    private final Set<AutoCloseable> closeables = new HashSet<>();
    private ConfigModel configModel;
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

        // Misc init
        Premium.init();
        this.closeables.add(TransitionRegistry::cleanup);

        // Init config
        this.saveDefaultConfig();
        this.configModel = new ConfigModel(this.getConfig());

        // Init color cache
        ColorCache colorCache = null;
        if (this.configModel.useColorCache && Premium.isPremium()) {
            final File file = new File(this.getDataFolder(), "colors.cache");
            if (!file.exists()) {
                this.getLogger().info("Color cache does not exist. Generating new cache... This will take a while");
                colorCache = ColorCache.generate();
                try (final OutputStream outputStream = new FileOutputStream(file)) {
                    colorCache.write(outputStream);
                    outputStream.flush();
                } catch (final IOException e) {
                    e.printStackTrace();
                    this.getLogger().severe("Failed to save color cache");
                }
            } else {
                try (final InputStream inputStream = new FileInputStream(file)) {
                    colorCache = ColorCache.fromInputStream(inputStream);
                } catch (final IOException e) {
                    e.printStackTrace();
                    this.getLogger().severe("Failed to load color cache");
                }
            }
        }

        // Init L10n
        this.saveResource("lang.yml", false);
        final File l10nFile = new File(this.getDataFolder(), "lang.yml");
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(l10nFile);
        this.update(configuration, l10nFile);
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

        // Init help book
        this.saveResource("helpbook.yml", false);
        final HelpBookConfiguration helpBookConfiguration = new HelpBookConfiguration();
        helpBookConfiguration.load();
        HelpBook.init(helpBookConfiguration);

        // Init Vault
        final RegisteredServiceProvider<Economy> registration = this.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration == null) {
            this.getLogger().severe("Please install an economy plugin!");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        final Economy economy = registration.getProvider();

        // Init image storage
        final ImageStorage imageStorage = this.loadImageStorage();
        if (imageStorage == null) {
            this.getLogger().severe("Invalid image storage configuration");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        this.closeables.add(imageStorage);

        // Init advert storage
        final AdvertStorage advertStorage = this.loadAdvertStorage(imageStorage);
        if (advertStorage == null) {
            this.getLogger().severe("Invalid advert storage configuration");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        this.closeables.add(advertStorage);

        // Init misc services
        final AdScreenStorage adScreenStorage = this.loadAdScreenStorage();
        this.closeables.add(adScreenStorage);
        final DefaultImageController defaultImageController = new DefaultImageController(this, imageStorage);
        final AdvertController advertController = new AdvertController(this, advertStorage, imageStorage, defaultImageController);
        final ImageRetriever imageRetriever = new ImageRetriever();
        final ImageConverter imageConverter = new ImageConverter(colorCache);

        // Register commands & dependencies, init completions
        final BukkitCommandManager commandManager = new BukkitCommandManager(this);
        final CommandCompletions<BukkitCommandCompletionContext> completions = commandManager.getCommandCompletions();
        completions.registerCompletion("mapads_names", context ->
                adScreenStorage.getScreens().stream()
                        .map(AdScreen::getId)
                        .collect(Collectors.toList()));
        completions.registerCompletion("mapads_transitions", context ->
                TransitionRegistry.names());
        completions.registerCompletion("mapads_adverts", context ->
                advertStorage.getPendingAdvertisements().stream()
                        .map(advertisement -> advertisement.getAdvertId().toString())
                        .collect(Collectors.toSet()));
        completions.registerCompletion("maps_ids", context ->
                MapScreenRegistry.getScreenIds().stream()
                        .map(String::valueOf)
                        .collect(Collectors.toList()));
        completions.registerCompletion("mapads_commondim", context -> {
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
        commandManager.registerDependency(DefaultImageController.class, defaultImageController);
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

        // Register listeners
        final PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerJoinListener(this, adScreenStorage, advertStorage), this);

        // Start tasks
        final BukkitScheduler scheduler = this.getServer().getScheduler();
        scheduler.runTaskTimerAsynchronously(this, new FrameSendTask(adScreenStorage), 4 * 20, 2 * 20);
        scheduler.runTaskTimerAsynchronously(this, () -> adScreenStorage.getScreens().forEach(advertController::update), 4 * 20, 60 * 20);
        scheduler.runTaskLater(this, () -> this.screensLoaded = true, 4 * 20);

        // Register services
        final ServicesManager servicesManager = this.getServer().getServicesManager();
        servicesManager.register(AdvertStorage.class, advertStorage, this, ServicePriority.Normal);
        servicesManager.register(AdScreenStorage.class, adScreenStorage, this, ServicePriority.Normal);
        servicesManager.register(ImageStorage.class, imageStorage, this, ServicePriority.Normal);
        servicesManager.register(DefaultImageController.class, defaultImageController, this, ServicePriority.Normal);
        servicesManager.register(ImageConverter.class, imageConverter, this, ServicePriority.Normal);
        servicesManager.register(ImageRetriever.class, imageRetriever, this, ServicePriority.Normal);

        // Init metrics
        final Metrics metrics = new Metrics(this, 13063);
        metrics.addCustomChart(new SimplePie("premium", () -> Premium.isPremium() ? "Yes" : "No"));
        metrics.addCustomChart(new AdvancedPie("transition_usage", () -> {
            final Map<String, Integer> map = new HashMap<>();
            for (final String name : TransitionRegistry.names()) {
                map.put(name, 0);
            }
            for (final AdScreen screen : adScreenStorage.getScreens()) {
                if (map.containsKey(screen.getTransition())) {
                    map.put(screen.getTransition(), map.get(screen.getTransition()) + 1);
                }
            }
            return map;
        }));
    }

    private void update(final YamlConfiguration configuration, final File l10nFile) {
        boolean changed = false;
        if (!configuration.contains("misc,update,0")) {
            configuration.set("misc,update,0", "&aA new Map-Ads update is available!");
            changed = true;
        }
        if (!configuration.contains("misc,update,1")) {
            configuration.set("misc,update,1", "&e%s");
            changed = true;
        }

        if (changed) {
            try {
                configuration.save(l10nFile);
                this.getLogger().info("lang.yml was updated");
            } catch (final IOException e) {
                e.printStackTrace();
                this.getLogger().severe("Failed to update lang.yml file");
            }
        }
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
                mysqlHikariConfig.setJdbcUrl("jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/"
                        + mysqlDb + "?user=" + mysqlUser + "&password=" + mysqlPass);
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
                mysqlHikariConfig.setJdbcUrl("jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/"
                        + mysqlDb + "?user=" + mysqlUser + "&password=" + mysqlPass);
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

    public boolean areScreensLoaded() {
        return this.screensLoaded;
    }

}
