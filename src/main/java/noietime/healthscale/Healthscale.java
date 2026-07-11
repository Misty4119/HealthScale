package noietime.healthscale;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * HealthScale v2 — Folia-native health display scaling plugin.
 *
 * <p>Design principles:
 * <ul>
 *   <li>Commands registered via Paper's {@code LifecycleEvents.COMMANDS} (Brigadier) — no YAML command declarations.</li>
 *   <li>All entity-state mutations run inside {@code EntityScheduler} (Folia requirement).</li>
 *   <li>Config reload and bulk-player iteration run inside {@code GlobalRegionScheduler}.</li>
 *   <li>No static mutable state; all configuration is encapsulated in {@link PluginConfig}.</li>
 *   <li>Input is validated before use; invalid values fall back gracefully.</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class Healthscale extends JavaPlugin implements Listener {

    // -----------------------------------------------------------------------
    // Configuration record — immutable snapshot of the parsed config.yml
    // -----------------------------------------------------------------------

    /**
     * Immutable snapshot of the parsed config.yml.
     *
     * @param enabled       whether health scaling is active
     * @param globalScale   default health-scale value for all worlds
     * @param minScale      minimum allowed scale (for validation)
     * @param maxScale      maximum allowed scale (for validation)
     * @param worldOverrides per-world scale overrides; key = world name
     * @param messages      raw MiniMessage strings keyed by message id
     */
    private record PluginConfig(
            boolean enabled,
            double globalScale,
            double minScale,
            double maxScale,
            Map<String, Double> worldOverrides,
            Map<String, String> messages
    ) {
        /** Returns the effective scale for the given world, falling back to globalScale. */
        double scaleFor(@Nullable World world) {
            if (world == null) return globalScale;
            return worldOverrides.getOrDefault(world.getName(), globalScale);
        }
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private static final MiniMessage MM = MiniMessage.miniMessage();

    /** Current live configuration; replaced atomically on reload. */
    private volatile PluginConfig cfg;

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void onEnable() {
        saveDefaultConfig();

        PluginConfig loaded = parseConfig();
        if (loaded == null) {
            getLogger().severe("設定檔解析失敗，插件將停用。請修正 config.yml 後重新啟動伺服器。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        cfg = loaded;

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Register commands via Paper's Brigadier LifecycleEvents (required for paper-plugin.yml based plugins)
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final var commands = event.registrar();
            commands.register(
                    "healthscale",
                    "管理 HealthScale 插件設定",
                    List.of("hs", "hscale"),
                    new HealthScaleCommand()
            );
        });

        getLogger().info("HealthScale v" + getDescription().getVersion()
                + " 已啟動 | 全域縮放: " + cfg.globalScale()
                + " | 世界覆蓋: " + cfg.worldOverrides().size() + " 個"
                + " | Folia 多執行緒支援已啟用");
    }

    @Override
    public void onDisable() {
        // Reset all online players so health bars return to vanilla behaviour
        for (var player : getServer().getOnlinePlayers()) {
            player.getScheduler().run(this, task -> {
                if (player.isOnline() && player.isValid()) {
                    player.setHealthScaled(false);
                }
            }, null);
        }
        getLogger().info("HealthScale 已停用，所有玩家血量縮放已重置。");
    }

    // -----------------------------------------------------------------------
    // Configuration parsing
    // -----------------------------------------------------------------------

    /**
     * Reads config.yml and builds an immutable {@link PluginConfig}.
     * Returns {@code null} if any critical value is invalid.
     */
    @Nullable
    private PluginConfig parseConfig() {
        reloadConfig();
        var c = getConfig();

        boolean enabled = c.getBoolean("enabled", true);
        double minScale = c.getDouble("min-scale", 2.0);
        double maxScale = c.getDouble("max-scale", 2048.0);

        if (minScale <= 0 || maxScale <= 0 || minScale >= maxScale) {
            getLogger().warning("min-scale / max-scale 設定不合理，已還原為預設值 (2.0 / 2048.0)。");
            minScale = 2.0;
            maxScale = 2048.0;
        }

        double globalScale = c.getDouble("health-scale", 20.0);
        if (!isValidScale(globalScale, minScale, maxScale)) {
            getLogger().warning("health-scale 數值 " + globalScale
                    + " 超出範圍 [" + minScale + ", " + maxScale + "]，已還原為 20.0。");
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
                    getLogger().warning("世界 '" + worldName + "' 的 health-scale 無效，已忽略並使用全域值。");
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

    private boolean isValidScale(double value, double min, double max) {
        return Double.isFinite(value) && value >= min && value <= max;
    }

    // -----------------------------------------------------------------------
    // Core logic — Folia-safe health scale application
    // -----------------------------------------------------------------------

    /**
     * Schedules a health-scale update for {@code player} on their own entity thread.
     * Safe to call from any thread (Folia or Paper).
     */
    private void applyHealthScale(@NotNull Player player) {
        final PluginConfig snapshot = cfg;
        if (!snapshot.enabled()) return;

        player.getScheduler().run(this, task -> {
            if (!player.isOnline() || !player.isValid()) return;
            double scale = snapshot.scaleFor(player.getWorld());
            player.setHealthScaled(true);
            player.setHealthScale(scale);
        }, null);
    }

    /**
     * Removes health scaling for {@code player}.
     */
    private void removeHealthScale(@NotNull Player player) {
        player.getScheduler().run(this, task -> {
            if (!player.isOnline() || !player.isValid()) return;
            player.setHealthScaled(false);
        }, null);
    }

    // -----------------------------------------------------------------------
    // Messaging helpers
    // -----------------------------------------------------------------------

    private void sendMsg(@NotNull CommandSender sender, @NotNull String key) {
        String raw = cfg.messages().getOrDefault(key, "<red>" + key + "</red>");
        sender.sendMessage(MM.deserialize(raw));
    }

    private void sendMsgWithBounds(@NotNull CommandSender sender,
                                   @NotNull String key,
                                   @NotNull PluginConfig snapshot) {
        String raw = snapshot.messages().getOrDefault(key, "<red>無效數值。範圍: {min} ~ {max}</red>");
        sender.sendMessage(MM.deserialize(raw,
                Placeholder.unparsed("min", String.valueOf(snapshot.minScale())),
                Placeholder.unparsed("max", String.valueOf(snapshot.maxScale()))
        ));
    }

    // -----------------------------------------------------------------------
    // Event listeners
    // -----------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyHealthScale(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        applyHealthScale(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        applyHealthScale(event.getPlayer());
    }

    // -----------------------------------------------------------------------
    // Command implementation — BasicCommand (Paper Brigadier bridge)
    // -----------------------------------------------------------------------

    /**
     * Implements the {@code /healthscale} command using Paper's {@link BasicCommand}
     * interface, which bridges into the Brigadier command system without requiring
     * full Brigadier node trees.
     */
    private final class HealthScaleCommand implements BasicCommand {

        @Override
        public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
            CommandSender sender = stack.getSender();

            if (args.length == 0) {
                sendMsg(sender, "usage");
                return;
            }

            switch (args[0].toLowerCase()) {
                case "info" -> handleInfo(sender);
                case "reload" -> {
                    if (!sender.hasPermission("healthscale.admin")) {
                        sendMsg(sender, "no-permission");
                        return;
                    }
                    handleReload(sender);
                }
                case "set" -> {
                    if (!sender.hasPermission("healthscale.admin")) {
                        sendMsg(sender, "no-permission");
                        return;
                    }
                    if (args.length < 2) {
                        sendMsg(sender, "usage");
                        return;
                    }
                    handleSet(sender, args[1]);
                }
                default -> sendMsg(sender, "usage");
            }
        }

        @Override
        public @NotNull Collection<String> suggest(@NotNull CommandSourceStack stack, @NotNull String[] args) {
            CommandSender sender = stack.getSender();
            if (args.length == 1) {
                if (sender.hasPermission("healthscale.admin")) {
                    return List.of("reload", "set", "info");
                }
                if (sender.hasPermission("healthscale.use")) {
                    return List.of("info");
                }
            }
            if (args.length == 2 && "set".equalsIgnoreCase(args[0])
                    && sender.hasPermission("healthscale.admin")) {
                return List.of("20.0", "40.0", "80.0");
            }
            return List.of();
        }
    }

    // -----------------------------------------------------------------------
    // Command handlers
    // -----------------------------------------------------------------------

    private void handleInfo(@NotNull CommandSender sender) {
        PluginConfig snapshot = cfg;
        String raw = snapshot.messages().getOrDefault("info",
                "<aqua>HealthScale v{version} | 縮放: <white>{scale}</white> | 世界覆蓋: <white>{overrides}</white> 個</aqua>");
        sender.sendMessage(MM.deserialize(raw,
                Placeholder.unparsed("version", getDescription().getVersion()),
                Placeholder.unparsed("scale", String.valueOf(snapshot.globalScale())),
                Placeholder.unparsed("overrides", String.valueOf(snapshot.worldOverrides().size()))
        ));
    }

    private void handleReload(@NotNull CommandSender sender) {
        sendMsg(sender, "reload-start");

        getServer().getGlobalRegionScheduler().run(this, task -> {
            PluginConfig parsed = null;
            try {
                parsed = parseConfig();
            } catch (Exception ex) {
                getLogger().log(Level.SEVERE, "重載設定時發生例外", ex);
            }

            if (parsed == null) {
                PluginConfig old = cfg;
                String raw = old.messages().getOrDefault("reload-invalid-scale",
                        "<red>config.yml 數值無效，已還原為預設值。</red>");
                sender.sendMessage(MM.deserialize(raw,
                        Placeholder.unparsed("min", String.valueOf(old.minScale())),
                        Placeholder.unparsed("max", String.valueOf(old.maxScale()))
                ));
                return;
            }

            cfg = parsed;

            String raw = parsed.messages().getOrDefault("reload-success",
                    "<green>重載完成！全域縮放: <white>{scale}</white></green>");
            sender.sendMessage(MM.deserialize(raw,
                    Placeholder.unparsed("scale", String.valueOf(parsed.globalScale()))
            ));

            getLogger().info("設定已重載 | 全域縮放: " + parsed.globalScale()
                    + " | 世界覆蓋: " + parsed.worldOverrides().size() + " 個");

            for (var player : getServer().getOnlinePlayers()) {
                if (parsed.enabled()) {
                    applyHealthScale(player);
                } else {
                    removeHealthScale(player);
                }
            }
        });
    }

    private void handleSet(@NotNull CommandSender sender, @NotNull String valueStr) {
        PluginConfig snapshot = cfg;
        double newScale;
        try {
            newScale = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            sendMsgWithBounds(sender, "set-invalid", snapshot);
            return;
        }

        if (!isValidScale(newScale, snapshot.minScale(), snapshot.maxScale())) {
            sendMsgWithBounds(sender, "set-invalid", snapshot);
            return;
        }

        cfg = new PluginConfig(
                snapshot.enabled(),
                newScale,
                snapshot.minScale(),
                snapshot.maxScale(),
                snapshot.worldOverrides(),
                snapshot.messages()
        );

        String raw = snapshot.messages().getOrDefault("set-success",
                "<green>已將血量顯示縮放設為 <white>{scale}</white>。</green>");
        sender.sendMessage(MM.deserialize(raw,
                Placeholder.unparsed("scale", String.valueOf(newScale))
        ));

        getServer().getGlobalRegionScheduler().run(this, task -> {
            for (var player : getServer().getOnlinePlayers()) {
                applyHealthScale(player);
            }
        });
    }
}
