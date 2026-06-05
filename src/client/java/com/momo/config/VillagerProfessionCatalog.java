package com.momo.config;

import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class VillagerProfessionCatalog {
    public static final String ALL_VALUE = "all";

    private static final List<Entry> ENTRIES = List.of(
            new Entry(ALL_VALUE, null, "text.autotrade-plus.profession.all", "\u5168\u90e8", "All", "all"),
            new Entry("minecraft:farmer", "minecraft:farmer", "text.autotrade-plus.profession.farmer", "\u519c\u6c11", "Farmer", "farmer"),
            new Entry("minecraft:cleric", "minecraft:cleric", "text.autotrade-plus.profession.cleric", "\u7267\u5e08", "Cleric", "cleric"),
            new Entry("minecraft:librarian", "minecraft:librarian", "text.autotrade-plus.profession.librarian", "\u56fe\u4e66\u7ba1\u7406\u5458", "Librarian", "librarian"),
            new Entry("minecraft:fletcher", "minecraft:fletcher", "text.autotrade-plus.profession.fletcher", "\u5236\u7bad\u5e08", "Fletcher", "fletcher"),
            new Entry("minecraft:armorer", "minecraft:armorer", "text.autotrade-plus.profession.armorer", "\u76d4\u7532\u5320", "Armorer", "armorer"),
            new Entry("minecraft:toolsmith", "minecraft:toolsmith", "text.autotrade-plus.profession.toolsmith", "\u5de5\u5177\u5320", "Toolsmith", "toolsmith"),
            new Entry("minecraft:weaponsmith", "minecraft:weaponsmith", "text.autotrade-plus.profession.weaponsmith", "\u6b66\u5668\u5320", "Weaponsmith", "weaponsmith"),
            new Entry("minecraft:fisherman", "minecraft:fisherman", "text.autotrade-plus.profession.fisherman", "\u6e14\u592b", "Fisherman", "fisherman"),
            new Entry("minecraft:leatherworker", "minecraft:leatherworker", "text.autotrade-plus.profession.leatherworker", "\u76ae\u5320", "Leatherworker", "leatherworker"),
            new Entry("minecraft:mason", "minecraft:mason", "text.autotrade-plus.profession.mason", "\u77f3\u5320", "Mason", "mason"),
            new Entry("minecraft:butcher", "minecraft:butcher", "text.autotrade-plus.profession.butcher", "\u5c60\u592b", "Butcher", "butcher"),
            new Entry("minecraft:cartographer", "minecraft:cartographer", "text.autotrade-plus.profession.cartographer", "\u5236\u56fe\u5e08", "Cartographer", "cartographer"),
            new Entry("minecraft:shepherd", "minecraft:shepherd", "text.autotrade-plus.profession.shepherd", "\u7267\u7f8a\u4eba", "Shepherd", "shepherd"),
            new Entry("minecraft:none", "minecraft:none", "text.autotrade-plus.profession.none", "\u65e0\u4e1a", "None", "none"),
            new Entry("minecraft:nitwit", "minecraft:nitwit", "text.autotrade-plus.profession.nitwit", "\u50bb\u5b50", "Nitwit", "nitwit")
    );

    private static final Map<String, Entry> ENTRY_BY_VALUE = new HashMap<>();

    static {
        for (Entry entry : ENTRIES) {
            addAlias(entry.configValue(), entry);
            addAlias(entry.chineseName(), entry);
            addAlias(entry.englishName(), entry);
            addAlias(entry.shortName(), entry);
            if (entry.professionId() != null) {
                addAlias(entry.professionId(), entry);
            }
        }
    }

    private VillagerProfessionCatalog() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static boolean allowsAll(String configValue) {
        if (configValue == null || configValue.isBlank()) {
            return true;
        }
        for (String value : parseValues(configValue)) {
            Entry entry = ENTRY_BY_VALUE.get(canonicalKey(value));
            if (entry != null && ALL_VALUE.equals(entry.configValue())) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> selectedProfessionIds(String configValue) {
        Set<String> ids = new LinkedHashSet<>();
        for (String value : parseValues(configValue)) {
            Entry entry = ENTRY_BY_VALUE.get(canonicalKey(value));
            if (entry != null && entry.professionId() != null) {
                ids.add(entry.professionId());
            } else if (value.startsWith("minecraft:")) {
                ids.add(value);
            }
        }
        return ids;
    }

    public static Set<String> normalizedValues(String configValue) {
        Set<String> values = new LinkedHashSet<>();
        for (String value : parseValues(configValue)) {
            Entry entry = ENTRY_BY_VALUE.get(canonicalKey(value));
            values.add(entry == null ? value : entry.configValue());
        }
        return values;
    }

    public static String normalizeSelection(String configValue) {
        Set<String> values = normalizedValues(configValue);
        if (values.isEmpty() || values.contains(ALL_VALUE)) {
            return ALL_VALUE;
        }
        return String.join(",", values);
    }

    public static Text displayTextForValue(String value) {
        Entry entry = ENTRY_BY_VALUE.get(canonicalKey(value));
        return entry == null ? Text.literal(value) : entry.nameText();
    }

    public static String searchTextForValue(String value) {
        Entry entry = ENTRY_BY_VALUE.get(canonicalKey(value));
        return entry == null ? value.toLowerCase(Locale.ROOT) : entry.searchText();
    }

    private static void addAlias(String value, Entry entry) {
        ENTRY_BY_VALUE.put(canonicalKey(value), entry);
    }

    private static String canonicalKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static Set<String> parseValues(String value) {
        Set<String> values = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return values;
        }
        for (String part : value.split("[,，]")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    public record Entry(
            String configValue,
            String professionId,
            String translationKey,
            String chineseName,
            String englishName,
            String shortName
    ) {
        public Text displayText() {
            return professionId == null
                    ? nameText().copy().append("  ").append(configValue)
                    : nameText().copy().append("  ").append(professionId);
        }

        public Text nameText() {
            return Text.translatable(translationKey);
        }

        public String searchText() {
            String id = professionId == null ? "" : professionId;
            return (configValue + " " + chineseName + " " + englishName + " " + shortName + " " + id).toLowerCase(Locale.ROOT);
        }
    }
}
