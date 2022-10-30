package dev.cerus.mapads.acf;

import co.aikar.commands.BukkitCommandManager;
import co.aikar.commands.BukkitLocales;
import co.aikar.commands.Locales;
import co.aikar.locales.LanguageTable;
import co.aikar.locales.LocaleManager;
import co.aikar.locales.MessageKey;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;

public final class AcfLocaleExtractor {

    private static Field localeManagerField;
    private static Field messagesField;

    private AcfLocaleExtractor() {
    }

    public static void init() throws NoSuchFieldException {
        localeManagerField = Locales.class.getDeclaredField("localeManager");
        localeManagerField.setAccessible(true);
        messagesField = LanguageTable.class.getDeclaredField("messages");
        messagesField.setAccessible(true);
    }

    public static Map<MessageKey, String> getMessages(final LanguageTable table) {
        final Map<MessageKey, String> messages;
        try {
            messages = (Map<MessageKey, String>) messagesField.get(table);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return Map.copyOf(messages);
    }

    public static LanguageTable getTable(final BukkitCommandManager manager, final Locale locale) {
        final LocaleManager<?> localeManager = getLocaleManager(manager);
        return localeManager.getTable(locale);
    }

    public static LocaleManager<?> getLocaleManager(final BukkitCommandManager manager) {
        final BukkitLocales locales = manager.getLocales();
        final Object localeManager;
        try {
            localeManager = localeManagerField.get(locales);
        } catch (final IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return (LocaleManager<?>) localeManager;
    }

}
