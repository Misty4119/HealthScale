package noietime.healthscale.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import noietime.healthscale.Healthscale;
import noietime.healthscale.config.PluginConfig;
import noietime.healthscale.util.MessageUtil;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Handles the `/healthscale` command execution and tab suggestions.
 */
@SuppressWarnings("UnstableApiUsage")
public class HealthScaleCommand implements BasicCommand {

    private final Healthscale plugin;

    public HealthScaleCommand(@NotNull Healthscale plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length == 0) {
            MessageUtil.sendMsg(sender, plugin.getPluginConfig(), "usage");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "info" -> handleInfo(sender);
            case "reload" -> {
                if (!sender.hasPermission("healthscale.admin")) {
                    MessageUtil.sendMsg(sender, plugin.getPluginConfig(), "no-permission");
                    return;
                }
                handleReload(sender);
            }
            case "set" -> {
                if (!sender.hasPermission("healthscale.admin")) {
                    MessageUtil.sendMsg(sender, plugin.getPluginConfig(), "no-permission");
                    return;
                }
                if (args.length < 2) {
                    MessageUtil.sendMsg(sender, plugin.getPluginConfig(), "usage");
                    return;
                }
                handleSet(sender, args[1]);
            }
            default -> MessageUtil.sendMsg(sender, plugin.getPluginConfig(), "usage");
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

    private void handleInfo(@NotNull CommandSender sender) {
        PluginConfig config = plugin.getPluginConfig();
        MessageUtil.sendMsg(sender, config, "info",
                Placeholder.unparsed("version", plugin.getPluginMeta().getVersion()),
                Placeholder.unparsed("scale", String.valueOf(config.globalScale())),
                Placeholder.unparsed("overrides", String.valueOf(config.worldOverrides().size()))
        );
    }

    private void handleReload(@NotNull CommandSender sender) {
        MessageUtil.sendMsg(sender, plugin.getPluginConfig(), "reload-start");

        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            PluginConfig parsed = null;
            try {
                parsed = plugin.getConfigManager().loadConfig();
            } catch (Exception ex) {
                plugin.getLogger().log(Level.SEVERE, "Exception occurred while reloading configuration", ex);
            }

            if (parsed == null) {
                MessageUtil.sendMsgWithBounds(sender, plugin.getPluginConfig(), "reload-invalid-scale");
                return;
            }

            plugin.setPluginConfig(parsed);

            MessageUtil.sendMsg(sender, parsed, "reload-success",
                    Placeholder.unparsed("scale", String.valueOf(parsed.globalScale()))
            );

            plugin.getLogger().info("Configuration reloaded | Global scale: " + parsed.globalScale()
                    + " | World overrides: " + parsed.worldOverrides().size());

            plugin.getHealthScaleService().updateAllPlayers(parsed);
        });
    }

    private void handleSet(@NotNull CommandSender sender, @NotNull String valueStr) {
        PluginConfig current = plugin.getPluginConfig();
        double newScale;
        try {
            newScale = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            MessageUtil.sendMsgWithBounds(sender, current, "set-invalid");
            return;
        }

        if (!plugin.getConfigManager().isValidScale(newScale, current.minScale(), current.maxScale())) {
            MessageUtil.sendMsgWithBounds(sender, current, "set-invalid");
            return;
        }

        PluginConfig updated = new PluginConfig(
                current.enabled(),
                newScale,
                current.minScale(),
                current.maxScale(),
                current.worldOverrides(),
                current.messages()
        );

        plugin.setPluginConfig(updated);

        MessageUtil.sendMsg(sender, updated, "set-success",
                Placeholder.unparsed("scale", String.valueOf(newScale))
        );

        plugin.getServer().getGlobalRegionScheduler().run(plugin, task ->
                plugin.getHealthScaleService().updateAllPlayers(updated)
        );
    }
}
