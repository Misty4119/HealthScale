package noietime.healthscale.service;

import noietime.healthscale.config.PluginConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Core business service for managing player health display scaling.
 * Compatible with Folia multithreading specifications.
 */
public class HealthScaleService {

    private final JavaPlugin plugin;

    public HealthScaleService(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Schedules a health-scale update for {@code player} on their entity scheduler thread.
     * Safe to call from any thread when plugin is enabled.
     */
    public void applyHealthScale(@NotNull Player player, @NotNull PluginConfig config) {
        if (!config.enabled()) return;
        if (!plugin.isEnabled()) {
            player.setHealthScaled(true);
            player.setHealthScale(config.scaleFor(player.getWorld()));
            return;
        }

        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline() || !player.isValid()) return;
            double scale = config.scaleFor(player.getWorld());
            player.setHealthScaled(true);
            player.setHealthScale(scale);
        }, null);
    }

    /**
     * Removes health scaling for {@code player}.
     */
    public void removeHealthScale(@NotNull Player player) {
        if (!plugin.isEnabled()) {
            player.setHealthScaled(false);
            return;
        }

        player.getScheduler().run(plugin, task -> {
            if (!player.isOnline() || !player.isValid()) return;
            player.setHealthScaled(false);
        }, null);
    }

    /**
     * Resets health scaling for all online players.
     * Called when the plugin configuration is reloaded or updated during runtime.
     */
    public void updateAllPlayers(@NotNull PluginConfig config) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (config.enabled()) {
                applyHealthScale(player, config);
            } else {
                removeHealthScale(player);
            }
        }
    }

    /**
     * Safely resets all online players' health scale during plugin disable/shutdown.
     * Directly calls {@link Player#setHealthScaled(boolean)} without registering tasks on a disabled plugin scheduler.
     */
    public void resetAllPlayersOnDisable() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            try {
                if (player.isOnline() && player.isValid()) {
                    player.setHealthScaled(false);
                }
            } catch (Throwable ignored) {
                // Ignore any connection/scheduler exceptions during shutdown
            }
        }
    }
}
