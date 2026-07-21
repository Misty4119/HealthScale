package noietime.healthscale.config;

import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Immutable snapshot of the parsed config.yml.
 *
 * @param enabled        whether health scaling is active
 * @param globalScale    default health-scale value for all worlds
 * @param minScale       minimum allowed scale
 * @param maxScale       maximum allowed scale
 * @param worldOverrides per-world scale overrides (key = world namespaced key or world name)
 * @param messages       raw MiniMessage strings keyed by message key
 */
public record PluginConfig(
        boolean enabled,
        double globalScale,
        double minScale,
        double maxScale,
        Map<String, Double> worldOverrides,
        Map<String, String> messages
) {
    /**
     * Returns the effective health scale for the given world, falling back to globalScale.
     * Lookup order:
     * 1. Exact NamespacedKey match (e.g. "rpg:T1")
     * 2. World name match (e.g. "T1") for backwards compatibility
     * 3. Global scale fallback
     */
    public double scaleFor(@Nullable World world) {
        if (world == null) return globalScale;
        String key = world.getKey().toString();
        String name = world.getName();
        return worldOverrides.getOrDefault(key,
               worldOverrides.getOrDefault(name, globalScale));
    }
}
