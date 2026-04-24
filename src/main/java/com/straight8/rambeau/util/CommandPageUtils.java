package com.straight8.rambeau.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public final class CommandPageUtils {

    private CommandPageUtils() {
    }

    public static boolean isInteger(String str) {
        try {
            Integer.valueOf(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static List<String> getNextInteger(String argument, int maxPage) {
        List<String> options = new ArrayList<>();
        for (int page = 1; page <= maxPage; page++) {
            String candidate = String.valueOf(page);
            if (candidate.startsWith(argument)) {
                options.add(candidate);
            }
        }
        return options;
    }

    public static <T> List<T> getPage(List<T> items, int page, int perPage) {
        page = page - 1; // 0-starting page
        List<T> list = new ArrayList<>();
        int startItem = page * perPage;
        int firstExcludedItem = (page + 1) * perPage;
        if (items.size() < startItem) {
            return list;
        }
        for (int index = startItem; index < firstExcludedItem && index < items.size(); index++) {
            list.add(items.get(index));
        }
        return list;
    }

    public static String getSpacingFor(String name, int maxLength, boolean isPlayer) {
        if (isPlayer) {
            return " ";
        }
        return " ".repeat(maxLength - name.length());
    }

    public static <T> int getMaxNameLength(Function<T, String> nameGetter, List<T> plugins) {
        List<String> names = new ArrayList<>();
        for (T plugin : plugins) {
            names.add(nameGetter.apply(plugin));
        }
        return getMaxNameLength(names);
    }

    public static int getMaxNameLength(List<String> pluginNames) {
        int len = 0;
        for (String name : pluginNames) {
            if (name.length() > len) {
                len = name.length();
            }
        }
        return len + 1;
    }

    public static List<String> filterByPrefix(String argument, Collection<String> options) {
        String lowerArgument = argument.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lowerArgument)) {
                matches.add(option);
            }
        }
        return matches;
    }

    public static int getTotalPages(int itemCount, int perPage) {
        if (itemCount == 0) {
            return 1;
        }
        return (itemCount + perPage - 1) / perPage;
    }
}
