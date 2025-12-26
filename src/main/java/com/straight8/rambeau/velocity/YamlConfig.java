package com.straight8.rambeau.velocity;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.Yaml;

public class YamlConfig {
    private static final String DELIMITER = ".";
    private final String currentPath;
    private final String name;
    private final Map<String, Object> map;

    public YamlConfig(File file) throws FileNotFoundException {
        this(new Yaml().load(new FileInputStream(file)), "");
    }

    private YamlConfig(Map<String, Object> map, String currentPath) {
        this.map = map;
        this.currentPath = currentPath;
        String[] split = currentPath.split(Pattern.quote(DELIMITER));
        this.name = split[split.length - 1]; // last part
    }

    public static void createFiles(String file) {
        if (!PluginVersionsVelocity.getInstance().getDataFolder().exists()) {
            PluginVersionsVelocity.getInstance().getDataFolder().mkdir();
        }

        File fileconfig = new File(PluginVersionsVelocity.getInstance().getDataFolder(), file + ".yml");
        if (!fileconfig.exists()) {
            try (InputStream in = PluginVersionsVelocity.getInstance().getResourceAsStream(file + ".yml")) {
                Files.copy(in, fileconfig.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Collection<String> getKeys(boolean deep) {
        if (deep) {
            throw new IllegalStateException("Deep keys are not supported at this time");
        }
        return map.keySet();
    }

    public Map<String, Object> getValues(boolean deep) {
        if (!deep) {
            throw new IllegalStateException("Shallow values are not supported at this time");
        }
        return map;
    }

    public boolean contains(String path) {
        Map<String, Object> curMap = map;
        Object result;
        String[] split = path.split(Pattern.quote(DELIMITER));
        int counter = 0;
        for (String curPath : split) {
            counter++;
            result = curMap.get(curPath);
            if (!(result instanceof Map)) {
                return counter >= split.length; // not found
// contains
            }
            curMap = (Map<String, Object>) result; // unchecked
        }
        return true; // result is a map
    }

    public boolean contains(String path, boolean ignoreDefault) {
        return contains(path); // TODO - better
    }

    public boolean isSet(String path) {
        return contains(path); // TODO - better
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public String getName() {
        return name;
    }

    public Object get(String path) {
        Map<String, Object> curMap = map;
        Object result = null;
        String[] split = path.split(Pattern.quote(DELIMITER));
        int counter = 0;
        for (String curPath : split) {
            counter++;
            result = curMap.get(curPath);
            if (!(result instanceof Map)) {
                if (counter < split.length) {
                    return null; // not found
                }
                return result;
            }
            curMap = (Map<String, Object>) result; // unchecked
        }
        return result;
    }

    public Object get(String path, Object def) {
        Object o = get(path);
        return o == null ? def : o;
    }

    public String getString(String path) {
        Object o = get(path);
        return o == null ? null : String.valueOf(o);
    }

    public String getString(String path, String def) {
        String str = getString(path);
        return str == null ? def : str;
    }

    public int getInt(String path) {
        return getInt(path, 0);
    }

    public int getInt(String path, int def) {
        if (!contains(path) && !isSet(path)) {
            return def;
        }
        try {
            return Integer.parseInt(getString(path));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public boolean getBoolean(String path) {
        return getBoolean(path, false);
    }

    public boolean getBoolean(String path, boolean def) {
        if (!contains(path) && !isSet(path)) {
            return def;
        }
        try {
            return Boolean.parseBoolean(getString(path));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public double getDouble(String path) {
        return getDouble(path, 0.0D);
    }

    public double getDouble(String path, double def) {
        if (!contains(path) && !isSet(path)) {
            return def;
        }
        try {
            return Double.parseDouble(getString(path));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public long getLong(String path) {
        return getLong(path, 0L);
    }

    public long getLong(String path, long def) {
        if (!contains(path) && !isSet(path)) {
            return def;
        }
        try {
            return Long.parseLong(getString(path));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public List<?> getList(String path) {
        if (!contains(path)) {
            return null;
        }
        Object o = get(path);
        if (o instanceof List) {
            return (List<?>) o;
        }
        return null;
    }

    public List<?> getList(String path, List<?> def) {
        List<?> list = getList(path);
        return list == null ? def : list;
    }

    public List<String> getStringList(String path) {
        List<?> list = getList(path);
        if (list == null || list.isEmpty()) {
            return new ArrayList<>(); // empty list
        }
        if (list.get(0) instanceof String) {
            return list; // unchecked
        }
        return new ArrayList<>();
    }

    public YamlConfig getConfigurationSection(String path) {
        Object res = get(path);
        if (res instanceof Map) {
            return new YamlConfig((Map<String, Object>) res, currentPath + DELIMITER + path); // unchecked
        }
        return null;
    }
}