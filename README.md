# PluginVersions &nbsp; &nbsp; &nbsp; &nbsp; &nbsp; <a href="https://hangar.papermc.io/SlimeDog/PluginVersions">![download-on-hangar](https://user-images.githubusercontent.com/17748923/187102194-00e910e6-ee8e-42cb-bfe1-d2f9e657ef4b.png)</a> <a href="https://www.spigotmc.org/resources/70927/">![download-on-spigot](https://user-images.githubusercontent.com/17748923/187102011-b72e0f1d-ba74-4cb2-a69e-46f48cb364b5.png)</a>

PluginVersions creates an alphabetically sorted list of loaded plugins and their versions. This 1MB update also keeps a human-readable plugin inventory database, lets you curate plugin URLs over time, and exports Markdown reports for GitHub, Discord, or server documentation.

## Tested Environment

This Paper build targets PaperMC `26.1.2` (`paper-api:26.1.2.build.20-alpha`) and compiles with Java 25. The plugin uses `api-version: 1.21.11` and reports version `2.0.0`.

The plugin data folder is `plugins/1MB-PluginVersions/`.

The only root command is `/pv`. The long `/pluginversions` command is not registered; the namespaced command remains available as `pluginversions:pv`.

All commands can be executed from the console. In-game use requires the matching permission.

## Build

```bash
gradle clean build
```

The compiled jar is written to `builds/libs/1MB-PluginVersions-v2.0.0-003-j25-26.1.2.jar`.

## Commands

| Command | Description |
| --- | --- |
| `/pv help [page\|topic]` | Shows help pages. |
| `/pv list [page]` | Lists loaded plugins and versions. Without a page, it lists every loaded plugin. |
| `/pv reload` | Reloads `config.yml` and the active translation file. |
| `/pv debug [page\|topic]` | Shows status, plugin details, server details, counts, commands, permissions, placeholders, config, set, and URL pages. |
| `/pv debug url add <plugin> <url>` | Stores a manual URL and infers the URL type. |
| `/pv debug url add <plugin> <type> <url>` | Stores a manual URL with a custom type such as `source`, `docs`, or `download`. |
| `/pv debug url del <plugin> [type] <url>` | Removes a stored manual URL. |
| `/pv debug url list <plugin>` | Lists manual URLs stored for a plugin. |
| `/pv debug url audit [page]` | Shows URL coverage for tracked plugins. |
| `/pv config [page]` | Shows active configuration values. |
| `/pv set <path> <value>` | Updates a known `config.yml` setting, saves it, and reloads the plugin settings. Filesystem paths are console-only. |
| `/pv export [markdown\|discord]` | Updates the database and writes timestamped plus stable latest export files. |

## Permissions

All permissions except `pluginversions.help` default to op.

| Permission | Description |
| --- | --- |
| `pluginversions.*` | Grants every PluginVersions command and URL action. |
| `pluginversions.help` | Allows access to help pages. |
| `pluginversions.list` | Allows listing loaded plugins. |
| `pluginversions.reload` | Allows reloading config and translations. |
| `pluginversions.debug` | Allows viewing debug pages. |
| `pluginversions.config` | Allows viewing active config values. |
| `pluginversions.set` | Allows changing known config values. |
| `pluginversions.export` | Allows exporting plugin inventory reports. |
| `pluginversions.url.*` | Grants all manual URL database actions. |
| `pluginversions.url.list` | Allows listing and auditing URL entries. |
| `pluginversions.url.add` | Allows adding manual URL entries. |
| `pluginversions.url.del` | Allows removing manual URL entries. |

## Placeholders

Language phrases live in `translations/Locale_EN.yml` and use MiniMessage. Legacy ampersand color codes are migrated at runtime if older values are found, but new phrases should use MiniMessage tags.

Language tokens include `{name}`, `{display-name}`, `{internal-name}`, `{version}`, `{spacing}`, `{page}`, `{pages}`, `{count}`, `{enabled}`, and `{disabled}`.

If PlaceholderAPI is installed, PluginVersions registers these placeholders:

| Placeholder | Result |
| --- | --- |
| `%pluginversions_total%` | Loaded plugin count. |
| `%pluginversions_enabled%` | Enabled plugin count. |
| `%pluginversions_disabled%` | Disabled plugin count. |
| `%pluginversions_database_tracked%` | Plugins currently tracked in the inventory database. |
| `%pluginversions_version_<plugin>%` | Version for a loaded plugin. |
| `%pluginversions_url_<plugin>_<type>%` | First matching detected or manual URL for a plugin and type. |

## Configuration

`config.yml` contains only operational settings and is fully commented. Player-facing phrases, command text, colors, and placeholders are kept in `translations/Locale_EN.yml`.

Anonymous metrics are disabled by default with `settings.enable-metrics: false`.

Update checks are disabled by default with `check-for-updates: false`.

The YAML database is stored at `plugins/1MB-PluginVersions/plugins-database.yml` by default. It tracks each plugin's display name, internal name, version, enabled state, description, authors, contributors, dependencies, provided plugins, website, detected URLs, manual URLs, and scan history.

`database.file` and `exports.directory` are filesystem paths and can only be changed from the console or by editing `config.yml`.

## URL Database

Detected URLs come from plugin metadata such as website and description fields. Manual URLs are stored under `manual-urls` and are merged with detected URLs in exports.

Known inferred URL types include `github`, `jenkins`, `modrinth`, `hangar`, `spigot`, `dev-bukkit`, `ci`, and `website`. You can also provide your own type:

```text
/pv debug url add LuckPerms source https://github.com/LuckPerms/LuckPerms
/pv debug url add LuckPerms download https://modrinth.com/plugin/luckperms
```

## Exports

Exports are written to `plugins/1MB-PluginVersions/exports/` by default.

| Command | Files |
| --- | --- |
| `/pv export markdown` | `plugins-YYYY-MM-DD-HHMMSS.md` and `plugins-latest.md` |
| `/pv export discord` | `plugins-discord-YYYY-MM-DD-HHMMSS.md` and `plugins-discord-latest.md` |

## Credits

PluginVersions credits mart-r, GabrielHD150, SlimeDog, and mrfloris as authors. Updates in this branch include implementation help from OpenAI.
