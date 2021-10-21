package dev.cerus.mapads.helpbook;

import dev.cerus.mapads.MapAdsPlugin;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class HelpBookConfiguration {

    private String title;
    private String author;
    private List<String> pages;

    public void load() {
        final File file = new File(JavaPlugin.getPlugin(MapAdsPlugin.class).getDataFolder(), "helpbook.yml");
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        this.title = configuration.getString("title");
        this.author = configuration.getString("author");
        final ConfigurationSection pagesSection = configuration.getConfigurationSection("pages");
        this.pages = pagesSection.getKeys(false).stream()
                .map(pagesSection::getStringList)
                .map(strings -> String.join("\n", strings))
                .collect(Collectors.toList());
    }

    public List<String> getPages() {
        return this.pages;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getTitle() {
        return this.title;
    }

}
