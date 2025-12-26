package com.straight8.rambeau.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;

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

    public static @NonNull List<String> getNextInteger(@NonNull String argument, int maxPage) {
        List<String> options = new ArrayList<>();
        int curNr;
        if (argument.isEmpty()) {
            curNr = 0;
        } else if (!isInteger(argument)) {
            return options;
        } else {
            curNr = Integer.parseInt(argument);
        }
        if (curNr * 10 > maxPage) {
            return options;
        }
        int example = 10 * curNr;
        while (example <= maxPage && example < 10 * (curNr + 1)) {
            if (example != 0) {
                // don't send 0
                options.add(String.valueOf(example));
            }
            example++;
        }
        return options;
    }

    public static <T> @NonNull List<T> getPage(@NonNull List<T> items, int page, int perPage) {
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

    public static @NonNull String getSpacingFor(String name, int maxLength, boolean isPlayer) {
        if (isPlayer) {
            return " ";
        }
        return " ".repeat(maxLength - name.length());
    }

    public static <T> int getMaxNameLength(Function<T, String> nameGetter, @NonNull List<T> plugins) {
        List<String> names = new ArrayList<>();
        for (T plugin : plugins) {
            names.add(nameGetter.apply(plugin));
        }
        return getMaxNameLength(names);
    }

    public static int getMaxNameLength(@NonNull List<String> pluginNames) {
        int len = 0;
        for (String name : pluginNames) {
            if (name.length() > len) {
                len = name.length();
            }
        }
        return len + 1;
    }
}
