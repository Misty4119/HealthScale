package noietime.healthscale.service;

import noietime.healthscale.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Core business service for managing player health display scaling.
 * Compatible with Folia / Canvas multithreading specifications.
 *
 * <p>Reliability guarantees:
 * <ul>
 *   <li><b>applyHealthScale</b> — immediate entity-scheduler run; safe to call from any thread.</li>
 *   <li><b>applyHealthScaleDelayed(player, config)</b> — GlobalRegionScheduler → EntityScheduler
 *       chain at <b>1, 5, and 20 ticks</b> after the event. Each attempt is idempotent, so
 *       applying the same value multiple times is harmless. This triple-shot pattern covers:
 *       <ul>
 *         <li>1 tick — normal join, initialisation packets already finished.</li>
 *         <li>5 ticks — slightly slow initialisation or elevated server load.</li>
 *         <li>20 ticks — slow disk I/O, async player-data loading, high-load burst.</li>
 *       </ul>
 *   </li>
 *   <li><b>applyHealthScaleDelayed(player, config, targetWorld)</b> — same as above, but uses a
 *       caller-supplied world for the scale lookup. Used for PlayerRespawnEvent where
 *       {@code player.getWorld()} still returns the death world at the time of the event.</li>
 * </ul>
 *
 * <h3>Why GlobalRegionScheduler → EntityScheduler?</h3>
 * <p>In Folia/Canvas, the entity scheduler may <em>retire</em> (silently drop) a task if the
 * player entity is not yet fully registered into its region — which can happen within the first
 * few ticks after {@link org.bukkit.event.player.PlayerJoinEvent}. The GlobalRegionScheduler
 * always executes, so we use it for the outer delay and then delegate to the EntityScheduler
 * (the only thread-safe way to call player entity methods) once the entity should be stable.</p>
 *
 * <h3>Why remove {@code isValid()}?</h3>
 * <p>{@code entity.isValid()} returns {@code false} before the entity has been ticked even once,
 * meaning it can be {@code false} even though {@code isOnline()} is {@code true}. Checking it
 * caused tasks to silently abort during the exact window we needed them to run. We replace it
 * with an online-check via a fresh UUID re-fetch, which is immune to this race condition.</p>
 */
public class HealthScaleService {

    private final JavaPlugin plugin;

    public HealthScaleService(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Schedules an immediate health-scale update for {@code player} on their entity-scheduler
     * thread. Safe to call from any thread while the plugin is enabled.
     */
    public void applyHealthScale(@NotNull Player player, @NotNull PluginConfig config) {
        if (!config.enabled()) return;

        if (!plugin.isEnabled()) {
            // Plugin is shutting down — apply directly without scheduler
            applyDirect(player, config, null);
            return;
        }

        final UUID uuid = player.getUniqueId();
        player.getScheduler().run(plugin, task -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;
            double scale = config.scaleFor(p.getWorld());
            p.setHealthScaled(true);
            p.setHealthScale(scale);
        }, null);
    }

    /**
     * Schedules a health-scale update using the triple-shot GlobalRegionScheduler → EntityScheduler
     * pattern at 1, 5, and 20 ticks. Uses {@code player.getWorld()} at execution time for the
     * scale lookup.
     *
     * <p>For respawn scenarios where the destination world is known in advance, use
     * {@link #applyHealthScaleDelayed(Player, PluginConfig, World)} instead.
     */
    public void applyHealthScaleDelayed(@NotNull Player player, @NotNull PluginConfig config) {
        applyHealthScaleDelayed(player, config, null);
    }

    /**
     * Schedules a health-scale update using the triple-shot GlobalRegionScheduler → EntityScheduler
     * pattern at 1, 5, and 20 ticks, using {@code targetWorld} for the scale lookup.
     *
     * <p>This overload is required for {@link org.bukkit.event.player.PlayerRespawnEvent}:
     * when that event fires the player's world reference still points to the world they died in,
     * so calling {@code player.getWorld()} would yield the wrong per-world override.
     * Pass {@code event.getRespawnLocation().getWorld()} as {@code targetWorld} to ensure
     * the correct scale is applied after the respawn teleport completes.
     *
     * @param targetWorld the world to use for the scale lookup, or {@code null} to fall back
     *                    to {@code player.getWorld()} at execution time
     */
    public void applyHealthScaleDelayed(@NotNull Player player, @NotNull PluginConfig config,
                                        @Nullable World targetWorld) {
        if (!config.enabled()) return;

        if (!plugin.isEnabled()) {
            applyHealthScale(player, config);
            return;
        }

        final UUID uuid = player.getUniqueId();

        // Triple-shot: 1, 5, 20 ticks — covers all initialisation timing scenarios.
        // GlobalRegionScheduler guarantees execution (never retires).
        // EntityScheduler inside ensures thread-safe player entity access.
        scheduleGlobalToEntity(uuid, config, targetWorld, 1L);
        scheduleGlobalToEntity(uuid, config, targetWorld, 5L);
        scheduleGlobalToEntity(uuid, config, targetWorld, 20L);
    }

    /**
     * Removes health scaling for {@code player} on their entity-scheduler thread.
     */
    public void removeHealthScale(@NotNull Player player) {
        if (!plugin.isEnabled()) {
            try {
                if (player.isOnline()) {
                    player.setHealthScaled(false);
                }
            } catch (Throwable ignored) {}
            return;
        }

        final UUID uuid = player.getUniqueId();
        player.getScheduler().run(plugin, task -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;
            p.setHealthScaled(false);
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
     * Directly calls {@link Player#setHealthScaled(boolean)} without registering tasks on a
     * disabled plugin scheduler.
     */
    public void resetAllPlayersOnDisable() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            try {
                if (player.isOnline()) {
                    player.setHealthScaled(false);
                }
            } catch (Throwable ignored) {
                // Ignore any connection/scheduler exceptions during shutdown
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Schedules a single GlobalRegionScheduler delayed task that, upon execution, dispatches
     * the actual health-scale application to the player's EntityScheduler.
     *
     * <p>This two-step chain guarantees:
     * <ol>
     *   <li>The outer delay <b>always fires</b> (GlobalRegionScheduler never retires tasks).</li>
     *   <li>The inner player-entity operations run on the <b>correct region thread</b>
     *       (EntityScheduler is the only thread-safe way to call player methods in Folia/Canvas).</li>
     * </ol>
     *
     * <p>A fresh UUID re-fetch is used instead of {@code isValid()} to avoid the Paper/NMS
     * race condition where {@code isValid()} returns {@code false} before the entity's first tick
     * even though the player is fully online.
     *
     * @param uuid        the player's UUID (retained to avoid stale entity references)
     * @param config      the plugin config supplying the scale value
     * @param targetWorld the world to use for the scale lookup, or {@code null} to use
     *                    {@code player.getWorld()} at execution time
     * @param delayTicks  how many ticks to wait before executing
     */
    private void scheduleGlobalToEntity(@NotNull UUID uuid, @NotNull PluginConfig config,
                                         @Nullable World targetWorld, long delayTicks) {
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, globalTask -> {
            // Re-fetch the player by UUID to avoid stale references and skip isValid() pitfalls.
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) return;

            // Delegate to EntityScheduler for thread-safe player-entity access.
            p.getScheduler().run(plugin, entityTask -> {
                Player ep = Bukkit.getPlayer(uuid);
                if (ep == null || !ep.isOnline()) return;
                World world = (targetWorld != null) ? targetWorld : ep.getWorld();
                double scale = config.scaleFor(world);
                ep.setHealthScaled(true);
                ep.setHealthScale(scale);
            }, null);
            // Retired callback is null: if the entity scheduler retires at this point
            // (extremely unlikely since a later shot will also fire), we simply rely on
            // the next scheduled attempt (5 ticks or 20 ticks) to succeed.
        }, delayTicks);
    }

    /**
     * Applies health scale directly without any scheduler — used only during plugin shutdown
     * or when the scheduler is unavailable.
     */
    private void applyDirect(@NotNull Player player, @NotNull PluginConfig config,
                              @Nullable World targetWorld) {
        try {
            if (player.isOnline()) {
                World world = (targetWorld != null) ? targetWorld : player.getWorld();
                player.setHealthScaled(true);
                player.setHealthScale(config.scaleFor(world));
            }
        } catch (Throwable ignored) {}
    }
}
