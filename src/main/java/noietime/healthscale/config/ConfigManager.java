package noietime.healthscale.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages loading and parsing of the plugin's configuration file.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private final Logger logger;

    public ConfigManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * Reads config.yml and returns an immutable {@link PluginConfig}.
     * Returns {@code null} if configuration parsing encounters unrecoverable errors.
     */
    @Nullable
    public PluginConfig loadConfig() {
        plugin.reloadConfig();
        var c = plugin.getConfig();

        boolean enabled = c.getBoolean("enabled", true);
        double minScale = c.getDouble("min-scale", 2.0);
        double maxScale = c.getDouble("max-scale", 2048.0);

        if (minScale <= 0 || maxScale <= 0 || minScale >= maxScale) {
            logger.warning("Invalid min-scale or max-scale configuration. Resetting to defaults (2.0 / 2048.0).");
            minScale = 2.0;
            maxScale = 2048.0;
        }

        double globalScale = c.getDouble("health-scale", 20.0);
        if (!isValidScale(globalScale, minScale, maxScale)) {
            logger.warning("Configured health-scale (" + globalScale + ") is outside valid bounds ["
                    + minScale + ", " + maxScale + "]. Resetting to 20.0.");
            globalScale = 20.0;
        }

        // Parse world overrides
        Map<String, Double> overrides = new HashMap<>();
        ConfigurationSection worldSection = c.getConfigurationSection("world-overrides");
        if (worldSection != null) {
            for (String worldName : worldSection.getKeys(false)) {
                ConfigurationSection ws = worldSection.getConfigurationSection(worldName);
                if (ws == null) continue;
                double wScale = ws.getDouble("health-scale", globalScale);
                if (!isValidScale(wScale, minScale, maxScale)) {
                    logger.warning("Invalid health-scale for world '" + worldName + "'. Ignoring override.");
                    continue;
                }
                overrides.put(worldName, wScale);
            }
        }

        // Parse messages
        Map<String, String> msgs = new HashMap<>();
        ConfigurationSection msgSection = c.getConfigurationSection("messages");
        if (msgSection != null) {
            for (String key : msgSection.getKeys(false)) {
                msgs.put(key, msgSection.getString(key, ""));
            }
        }

        return new PluginConfig(enabled, globalScale, minScale, maxScale,
                Map.copyOf(overrides), Map.copyOf(msgs));
    }

    public boolean isValidScale(double value, double min, double max) {
        return Double.isFinite(value) && value >= min && value <= max;
    }
}
