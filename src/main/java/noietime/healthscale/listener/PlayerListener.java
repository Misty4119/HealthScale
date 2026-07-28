package noietime.healthscale.listener;

import noietime.healthscale.Healthscale;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event listener for updating player health scale on join, respawn, and world change.
 *
 * <p>Event selection rationale:
 * <ul>
 *   <li>{@link PlayerJoinEvent} — fires when a player first connects.
 *       {@code applyHealthScaleDelayed} is used because the entity may not be fully
 *       initialised into the world at this exact moment.</li>
 *   <li>{@link PlayerRespawnEvent} — fires before the respawn teleport completes, so
 *       {@code player.getWorld()} still returns the death world. We capture the target
 *       world from {@code event.getRespawnLocation().getWorld()} and pass it explicitly
 *       to the service, ensuring the correct per-world scale override is applied even
 *       though the player hasn't physically moved yet.</li>
 *   <li>{@link PlayerChangedWorldEvent} — fires after the world switch is complete;
 *       {@code player.getWorld()} is already the new world, so no delay is needed.</li>
 * </ul>
 */
public class PlayerListener implements Listener {

    private final Healthscale plugin;

    public PlayerListener(@NotNull Healthscale plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Delayed application: entity may not be fully initialised when this event fires.
        // The service's retired-callback fallback ensures the scale is applied even on
        // server startup or under high load where 1 tick is insufficient.
        plugin.getHealthScaleService().applyHealthScaleDelayed(event.getPlayer(), plugin.getPluginConfig());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Capture the respawn target world from the event NOW, before the player is teleported.
        // At this point player.getWorld() still returns the world the player died in.
        // Passing the respawn world explicitly ensures the correct per-world health-scale
        // override is looked up, regardless of when the delayed task actually executes.
        World respawnWorld = event.getRespawnLocation().getWorld();
        plugin.getHealthScaleService().applyHealthScaleDelayed(
                event.getPlayer(), plugin.getPluginConfig(), respawnWorld);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        // Player is already in the new world when this event fires — no delay needed.
        plugin.getHealthScaleService().applyHealthScale(event.getPlayer(), plugin.getPluginConfig());
    }
}
