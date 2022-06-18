package dev.cerus.mapads.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta meta;

    public ItemBuilder(final ItemStack itemStack) {
        this.itemStack = itemStack;
        this.meta = itemStack.getItemMeta();
    }

    public ItemBuilder(final Material type) {
        this(new ItemStack(type));
    }

    public ItemBuilder setSkullOwner(final UUID owner) {
        final SkullMeta skullMeta = (SkullMeta) this.meta;
        skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        return this;
    }

    public ItemBuilder setName(final String name) {
        this.meta.setDisplayName(name);
        return this;
    }

    public ItemBuilder setLore(final Collection<String> lore) {
        this.meta.setLore(lore instanceof List ? (List<String>) lore : List.copyOf(lore));
        return this;
    }

    public ItemBuilder setLore(final String... lore) {
        this.meta.setLore(Arrays.asList(lore));
        return this;
    }

    public ItemBuilder clearLore() {
        this.meta.setLore(null);
        return this;
    }

    public ItemBuilder addLore(final String... strings) {
        List<String> lore = this.meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        lore.addAll(Arrays.asList(strings));
        this.meta.setLore(lore);
        return this;
    }

    public ItemBuilder custom(final Consumer<ItemMeta> func) {
        func.accept(this.meta);
        return this;
    }

    public ItemStack build() {
        this.itemStack.setItemMeta(this.meta);
        return this.itemStack;
    }

}
