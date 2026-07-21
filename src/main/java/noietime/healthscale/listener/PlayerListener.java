package noietime.healthscale.listener;

import noietime.healthscale.Healthscale;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Event listener for updating player health scale on join, respawn, and world change.
 */
public class PlayerListener implements Listener {

    private final Healthscale plugin;

    public PlayerListener(@NotNull Healthscale plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getHealthScaleService().applyHealthScale(event.getPlayer(), plugin.getPluginConfig());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        plugin.getHealthScaleService().applyHealthScale(event.getPlayer(), plugin.getPluginConfig());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        plugin.getHealthScaleService().applyHealthScale(event.getPlayer(), plugin.getPluginConfig());
    }
}
