package dev.cerus.mapads;

import co.aikar.commands.BukkitCommandCompletionContext;
import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.CommandCompletions;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.locales.LanguageTable;
import co.aikar.locales.MessageKey;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.cerus.mapads.acf.AcfLocaleExtractor;
import dev.cerus.mapads.advert.AdvertController;
import dev.cerus.mapads.advert.storage.AdvertStorage;
import dev.cerus.mapads.advert.storage.MySqlAdvertStorageImpl;
import dev.cerus.mapads.advert.storage.SqliteAdvertStorageImpl;
import dev.cerus.mapads.command.AdvertCommand;
import dev.cerus.mapads.command.DefaultImageCommand;
import dev.cerus.mapads.command.DiagnoseCommand;
import dev.cerus.mapads.command.GroupCommand;
import dev.cerus.mapads.command.HelpCommand;
import dev.cerus.mapads.command.MapAdsCommand;
import dev.cerus.mapads.command.PremiumCommand;
import dev.cerus.mapads.command.PreviewCommand;
import dev.cerus.mapads.command.ReviewCommand;
import dev.cerus.mapads.command.ScreenCommand;
import dev.cerus.mapads.discordbot.diagnostics.Diagnosis;
import dev.cerus.mapads.economy.EconomyWrapper;
import dev.cerus.mapads.economy.EconomyWrappers;
import dev.cerus.mapads.helpbook.HelpBook;
import dev.cerus.mapads.helpbook.HelpBookConfiguration;
import dev.cerus.mapads.hook.DiscordHook;
import dev.cerus.mapads.image.DefaultImageController;
import dev.cerus.mapads.image.ImageConverter;
import dev.cerus.mapads.image.ImageRetriever;
import dev.cerus.mapads.image.storage.ImageStorage;
import dev.cerus.mapads.image.storage.MySqlImageStorageImpl;
import dev.cerus.mapads.image.storage.SqliteImageStorageImpl;
import dev.cerus.mapads.image.transition.TransitionRegistry;
import dev.cerus.mapads.image.transition.recorded.RecordedTransitions;
import dev.cerus.mapads.image.transition.recorded.storage.RecordedTransitionStorage;
import dev.cerus.mapads.image.transition.recorded.storage.SqliteRecordedTransitionStorage;
import dev.cerus.mapads.lang.L10n;
import dev.cerus.mapads.lang.LangUpdater;
import dev.cerus.mapads.listener.PlayerJoinListener;
import dev.cerus.mapads.listener.PlayerQuitListener;
import dev.cerus.mapads.premium.Premium;
import dev.cerus.mapads.screen.AdScreen;
import dev.cerus.mapads.screen.ScreenGroup;
import dev.cerus.mapads.screen.storage.AdScreenStorage;
import dev.cerus.mapads.screen.storage.YamlAdScreenStorageImpl;
import dev.cerus.mapads.task.FrameSendTask;
import dev.cerus.maps.plugin.map.MapScreenRegistry;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import sun.misc.Unsafe;

public class MapAdsPlugin extends JavaPlugin {

    private static boolean enabled = false;

    private final Set<AutoCloseable> closeables = new HashSet<>();
    private ConfigModel configModel;
    private boolean screensLoaded;
    private Supplier<CompletableFuture<Collection<Diagnosis>>> diagnosticsSupplier;

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
        this.getLogger().info("Transition recording is " + (this.configModel.enableTransitionRecording ? "enabled" : "disabled") + ".");
        if (this.configModel.trustedImageUrls.contains("*")) {
            this.getLogger().warning("Warning: Every website is considered trusted. This is a potential security risk.");
            this.getLogger().warning("Remove the \"*\" from the url-whitelist section in the config to disable this.");
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
                        .map(this::translateHexColors)
                        .map(s -> ChatColor.translateAlternateColorCodes('&', s))
                        .collect(Collectors.toList()));
            } else if (o instanceof String) {
                L10n.put(key.replace(",", "."), ChatColor
                        .translateAlternateColorCodes('&', this.translateHexColors((String) o)));
            } else {
                this.getLogger().warning("Invalid lang mapping: " + key + "->" + o.getClass().getName());
            }
        }

        // Init help book
        this.saveResource("helpbook.yml", false);
        final HelpBookConfiguration helpBookConfiguration = new HelpBookConfiguration();
        helpBookConfiguration.load();
        HelpBook.init(helpBookConfiguration);

        // Init economy
        final EconomyWrapper<?> economyWrapper = EconomyWrappers.find();
        this.getLogger().info("Found economy wrapper " + economyWrapper.asString());

        // Init image storage
        final ImageStorage imageStorage = this.loadImageStorage();
        if (imageStorage == null) {
            this.getLogger().severe("Invalid image storage configuration");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        this.closeables.add(imageStorage);

        // Init ad screen storage
        final AdScreenStorage adScreenStorage = this.loadAdScreenStorage();
        this.closeables.add(adScreenStorage);

        // Load recorded transition storage
        final RecordedTransitionStorage recordedTransitionStorage = this.loadRecTransitionStorage();
        this.closeables.add(recordedTransitionStorage);

        // Init advert storage
        final AdvertStorage advertStorage = this.loadAdvertStorage(adScreenStorage, imageStorage, recordedTransitionStorage);
        if (advertStorage == null) {
            this.getLogger().severe("Invalid advert storage configuration");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        this.closeables.add(advertStorage);

        // Init Discord bot
        boolean discordEnabled = false;
        this.diagnosticsSupplier = () -> null;
        if (this.getServer().getPluginManager().isPluginEnabled("map-ads-discord-bot")) {
            final DiscordHook discordHook = new DiscordHook(this, advertStorage, imageStorage, economyWrapper);
            final AutoCloseable closeable = discordHook.load();
            if (closeable != null) {
                this.closeables.add(closeable);
                this.getLogger().info("Discovered Discord extension");
                discordEnabled = true;
                this.diagnosticsSupplier = discordHook::runDiagnostics;
            }
        }
        final boolean finalDiscordEnabled = discordEnabled;

        // Init misc services
        final DefaultImageController defaultImageController = new DefaultImageController(this, imageStorage);
        final AdvertController advertController = new AdvertController(this, advertStorage, imageStorage,
                defaultImageController, adScreenStorage, recordedTransitionStorage);
        final ImageRetriever imageRetriever = new ImageRetriever();
        final ImageConverter imageConverter = new ImageConverter();

        // Register commands & dependencies, init completions and messages
        final BukkitCommandManager commandManager = new BukkitCommandManager(this);
        this.loadAcfMessages(commandManager);
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
        completions.registerCompletion("mapads_groups", context ->
                adScreenStorage.getScreenGroups().stream()
                        .map(ScreenGroup::id)
                        .toList());
        completions.registerCompletion("mapads_group_name", context -> List.of("Group Name"));
        commandManager.getCommandContexts().registerContext(UUID.class, ctx -> {
            final String s = ctx.popFirstArg();
            try {
                return UUID.fromString(s);
            } catch (final IllegalArgumentException ignored) {
                throw new InvalidCommandArgument("Not a UUID");
            }
        });
        commandManager.registerDependency(ImageStorage.class, imageStorage);
        commandManager.registerDependency(AdvertStorage.class, advertStorage);
        commandManager.registerDependency(AdScreenStorage.class, adScreenStorage);
        commandManager.registerDependency(RecordedTransitionStorage.class, recordedTransitionStorage);
        commandManager.registerDependency(ImageRetriever.class, imageRetriever);
        commandManager.registerDependency(ImageConverter.class, imageConverter);
        commandManager.registerDependency(DefaultImageController.class, defaultImageController);
        commandManager.registerDependency(AdvertController.class, advertController);
        commandManager.registerDependency(ConfigModel.class, this.configModel);
        commandManager.registerDependency(EconomyWrapper.class, economyWrapper);
        commandManager.registerCommand(new MapAdsCommand());
        commandManager.registerCommand(new PremiumCommand());
        commandManager.registerCommand(new ScreenCommand());
        commandManager.registerCommand(new DefaultImageCommand());
        commandManager.registerCommand(new ReviewCommand());
        commandManager.registerCommand(new HelpCommand());
        commandManager.registerCommand(new PreviewCommand());
        commandManager.registerCommand(new AdvertCommand());
        commandManager.registerCommand(new GroupCommand());
        commandManager.registerCommand(new DiagnoseCommand());

        // Register listeners
        final PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerJoinListener(this, adScreenStorage, advertStorage), this);
        pluginManager.registerEvents(new PlayerQuitListener(), this);

        // Start tasks
        final BukkitScheduler scheduler = this.getServer().getScheduler();
        scheduler.runTaskTimerAsynchronously(this, new FrameSendTask(this.configModel, adScreenStorage), 4 * 20, FrameSendTask.TICK_PERIOD);
        scheduler.runTaskTimerAsynchronously(this, () -> adScreenStorage.getScreens().forEach(advertController::update), 4 * 20, this.getConfig().getInt("task.update", 60 * 20));
        scheduler.runTaskTimerAsynchronously(this, () -> recordedTransitionStorage.deleteOlderThan(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)), 0, 60 * 20);
        scheduler.runTaskLater(this, () -> this.screensLoaded = true, 4 * 20);

        // Register services
        final ServicesManager servicesManager = this.getServer().getServicesManager();
        servicesManager.register(AdvertStorage.class, advertStorage, this, ServicePriority.Normal);
        servicesManager.register(AdScreenStorage.class, adScreenStorage, this, ServicePriority.Normal);
        servicesManager.register(ImageStorage.class, imageStorage, this, ServicePriority.Normal);
        servicesManager.register(DefaultImageController.class, defaultImageController, this, ServicePriority.Normal);
        servicesManager.register(ImageConverter.class, imageConverter, this, ServicePriority.Normal);
        servicesManager.register(ImageRetriever.class, imageRetriever, this, ServicePriority.Normal);
        servicesManager.register(AdvertController.class, advertController, this, ServicePriority.Normal);

        // Init metrics
        final Metrics metrics = new Metrics(this, 13063);
        metrics.addCustomChart(new SimplePie("default_command_enabled", () -> this.getConfig().getBoolean("disable-default-command") ? "No" : "Yes"));
        metrics.addCustomChart(new SimplePie("premium", () -> Premium.isPremium() ? "Yes" : "No"));
        metrics.addCustomChart(new SimplePie("discord_integration_enabled", () -> finalDiscordEnabled ? "Yes" : "No"));
        metrics.addCustomChart(new SimplePie("transition_recording_enabled", () -> this.configModel.enableTransitionRecording ? "Yes" : "No"));
        metrics.addCustomChart(new AdvancedPie("transition_usage", () -> {
            final Map<String, Integer> map = new HashMap<>();
            for (final String name : TransitionRegistry.names()) {
                if (TransitionRegistry.getTransition(name).getClass().getPackageName().equals("dev.cerus.mapads.image.transition")) {
                    map.put(name, 0);
                }
            }
            for (final AdScreen screen : adScreenStorage.getScreens()) {
                if (map.containsKey(screen.getTransition())) {
                    map.put(screen.getTransition(), map.get(screen.getTransition()) + 1);
                }
            }
            return map;
        }));
    }

    private void fixAnvilGui() {
        try {
            final Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            final Unsafe unsafe = (Unsafe) theUnsafeField.get(null);

            final Class<?> wrapperCls = Class.forName("dev.cerus.mapads.thirdparty.anvilgui.version.Wrapper1_19_R1");
            final Object wrapper = wrapperCls.getDeclaredConstructor().newInstance();
            final Field boolField = wrapperCls.getDeclaredField("IS_ONE_NINETEEN_ONE");
            boolField.setAccessible(true);

            final long off = unsafe.objectFieldOffset(boolField);
            unsafe.putBoolean(wrapper, off, true);

            final Class<?> mainCls = Class.forName("dev.cerus.mapads.thirdparty.anvilgui.AnvilGUI");
            final Field wrapperField = mainCls.getDeclaredField("WRAPPER");
            wrapperField.setAccessible(true);
            wrapperField.set(null, wrapper);

            this.getLogger().info("AnvilGUI fix has been injected");
        } catch (final NoSuchFieldException | ClassNotFoundException | InvocationTargetException | IllegalAccessException | InstantiationException |
                       NoSuchMethodException e) {
            this.getLogger().log(Level.WARNING, "Failed to inject AnvilGUI fix", e);
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
        RecordedTransitions.stop();
    }

    private RecordedTransitionStorage loadRecTransitionStorage() {
        final HikariConfig sqliteHikariConfig = new HikariConfig();
        sqliteHikariConfig.setDriverClassName(org.sqlite.JDBC.class.getName());
        sqliteHikariConfig.setJdbcUrl("jdbc:sqlite:" + this.getDataFolder().getPath() + "/recorded_transitions.db");
        return new SqliteRecordedTransitionStorage(new HikariDataSource(sqliteHikariConfig));
    }

    private AdScreenStorage loadAdScreenStorage() {
        final File screenConfigFile = new File(this.getDataFolder(), "screens.yml");
        final YamlConfiguration screenConfig = YamlConfiguration.loadConfiguration(screenConfigFile);

        final File groupConfigFile = new File(this.getDataFolder(), "groups.yml");
        final YamlConfiguration groupConfig = YamlConfiguration.loadConfiguration(groupConfigFile);

        return new YamlAdScreenStorageImpl(screenConfigFile, screenConfig, groupConfigFile, groupConfig);
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

    private AdvertStorage loadAdvertStorage(final AdScreenStorage adScreenStorage, final ImageStorage imageStorage, final RecordedTransitionStorage recordedTransitionStorage) {
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
                return new MySqlAdvertStorageImpl(new HikariDataSource(mysqlHikariConfig), adScreenStorage, imageStorage, recordedTransitionStorage);
            case "sqlite":
                final String dbName = config.getString("advert-storage.sqlite.db-name");

                final HikariConfig sqliteHikariConfig = new HikariConfig();
                sqliteHikariConfig.setDriverClassName(org.sqlite.JDBC.class.getName());
                sqliteHikariConfig.setJdbcUrl("jdbc:sqlite:" + this.getDataFolder().getPath() + "/" + dbName);
                return new SqliteAdvertStorageImpl(new HikariDataSource(sqliteHikariConfig), adScreenStorage, imageStorage, recordedTransitionStorage);
            default:
                return null;
        }
    }

    private void loadAcfMessages(final BukkitCommandManager commandManager) {
        // I'm too lazy to use ACFs locale system the way it was intended

        try {
            AcfLocaleExtractor.init();
        } catch (final NoSuchFieldException e) {
            this.getLogger().log(Level.SEVERE, "Failed to initialize ACF locale extractor", e);
            return;
        }

        final LanguageTable table = AcfLocaleExtractor.getTable(commandManager, commandManager.getLocales().getDefaultLocale());
        final Map<MessageKey, String> messages = AcfLocaleExtractor.getMessages(table);

        this.checkForLegacyAcfLang();
        final File acfLangFile = new File(this.getDataFolder(), "acf-lang.yml");
        final FileConfiguration acfConf;
        if (acfLangFile.exists()) {
            acfConf = YamlConfiguration.loadConfiguration(acfLangFile);
        } else {
            acfConf = new YamlConfiguration();
            messages.forEach((key, msg) -> acfConf.set(key.getKey().replace('.', '/'), msg));
            try {
                acfConf.save(acfLangFile);
            } catch (final IOException e) {
                this.getLogger().log(Level.SEVERE, "Failed to save ACF lang file", e);
            }
        }

        final Map<MessageKey, String> loadedMessages = new HashMap<>();
        for (final String keyStr : acfConf.getKeys(false)) {
            final MessageKey key = MessageKey.of(keyStr.replace('/', '.'));
            final String msg = acfConf.getString(keyStr);
            loadedMessages.put(key, ChatColor.translateAlternateColorCodes('&', this.translateHexColors(msg)));
        }
        commandManager.getLocales().addMessages(commandManager.getLocales().getDefaultLocale(), loadedMessages);
        this.getLogger().info("%d ACF messages were loaded".formatted(loadedMessages.size()));
    }

    private void checkForLegacyAcfLang() {
        final File legacyAcfLangFile = new File(this.getDataFolder(), "acf-lang.properties");
        if (legacyAcfLangFile.exists()) {
            final Properties acfLangProps = new Properties();
            try (final FileInputStream in = new FileInputStream(legacyAcfLangFile)) {
                acfLangProps.load(in);
            } catch (final IOException e) {
                this.getLogger().log(Level.SEVERE, "Failed to load legacy ACF lang file", e);
                return;
            }

            final File acfLangFile = new File(this.getDataFolder(), "acf-lang.yml");
            final YamlConfiguration acfConf = YamlConfiguration.loadConfiguration(acfLangFile);
            for (final Object keyObj : acfLangProps.keySet()) {
                final String key = keyObj.toString();
                final String msg = acfLangProps.getProperty(keyObj.toString());
                acfConf.set(key.replace('.', '/'), msg);
            }

            try {
                acfConf.save(acfLangFile);
                Files.move(legacyAcfLangFile.toPath(), legacyAcfLangFile.toPath().getParent().resolve("acf-lang.properties.bak"));
            } catch (final IOException e) {
                this.getLogger().log(Level.SEVERE, "Failed to save ACF lang file", e);
            }
        }
    }

    private void update(final YamlConfiguration configuration, final File l10nFile) {
        final LangUpdater langUpdater = new LangUpdater();
        langUpdater.update(l10nFile, configuration);
    }


    public CompletableFuture<Collection<Diagnosis>> runBotDiagnostics() {
        return this.diagnosticsSupplier.get();
    }

    public ConfigModel getConfigModel() {
        return this.configModel;
    }

    public boolean areScreensLoaded() {
        return this.screensLoaded;
    }

    private String translateHexColors(final String s) {
        final Matcher matcher = Pattern.compile("&#[A-Fa-f0-9]{6}").matcher(s);
        return matcher.replaceAll(res -> "§x§" + String.join("§", res.group().substring(2).split("")));
    }

}
