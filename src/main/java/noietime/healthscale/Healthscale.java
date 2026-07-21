package noietime.healthscale;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import noietime.healthscale.command.HealthScaleCommand;
import noietime.healthscale.config.ConfigManager;
import noietime.healthscale.config.PluginConfig;
import noietime.healthscale.listener.PlayerListener;
import noietime.healthscale.service.HealthScaleService;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Main plugin class for HealthScale.
 */
@SuppressWarnings("UnstableApiUsage")
public class Healthscale extends JavaPlugin {

    private ConfigManager configManager;
    private HealthScaleService healthScaleService;
    private volatile PluginConfig cfg;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.healthScaleService = new HealthScaleService(this);

        PluginConfig loaded = configManager.loadConfig();
        if (loaded == null) {
            getLogger().severe("Failed to parse config.yml. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.cfg = loaded;

        // Register event listener
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Register command via Paper Brigadier lifecycle events
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final var commands = event.registrar();
            commands.register(
                    "healthscale",
                    "Manage HealthScale plugin configuration",
                    List.of("hs", "hscale"),
                    new HealthScaleCommand(this)
            );
        });

        getLogger().info("HealthScale v" + getPluginMeta().getVersion()
                + " enabled | Global scale: " + cfg.globalScale()
                + " | World overrides: " + cfg.worldOverrides().size()
                + " | Folia multithreading support enabled");
    }

    @Override
    public void onDisable() {
        if (healthScaleService != null) {
            healthScaleService.resetAllPlayersOnDisable();
        }
        getLogger().info("HealthScale disabled. Reset player health scales.");
    }

    public @NotNull ConfigManager getConfigManager() {
        return configManager;
    }

    public @NotNull HealthScaleService getHealthScaleService() {
        return healthScaleService;
    }

    public @NotNull PluginConfig getPluginConfig() {
        return cfg;
    }

    public void setPluginConfig(@NotNull PluginConfig cfg) {
        this.cfg = cfg;
    }
}
