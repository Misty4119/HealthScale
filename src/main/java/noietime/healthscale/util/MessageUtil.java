package noietime.healthscale.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import noietime.healthscale.config.PluginConfig;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for formatting and sending MiniMessage components to command senders.
 */
public final class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private MessageUtil() {
    }

    /**
     * Sends a configured message by key to a sender.
     */
    public static void sendMsg(@NotNull CommandSender sender, @NotNull PluginConfig config, @NotNull String key) {
        String raw = config.messages().getOrDefault(key, "<red>" + key + "</red>");
        sender.sendMessage(MM.deserialize(raw));
    }

    /**
     * Sends a configured message with custom TagResolvers.
     */
    public static void sendMsg(@NotNull CommandSender sender, @NotNull PluginConfig config, @NotNull String key, @NotNull TagResolver... resolvers) {
        String raw = config.messages().getOrDefault(key, "<red>" + key + "</red>");
        sender.sendMessage(MM.deserialize(raw, resolvers));
    }

    /**
     * Sends a bounds error message with min/max placeholders.
     */
    public static void sendMsgWithBounds(@NotNull CommandSender sender, @NotNull PluginConfig config, @NotNull String key) {
        sendMsg(sender, config, key,
                Placeholder.unparsed("min", String.valueOf(config.minScale())),
                Placeholder.unparsed("max", String.valueOf(config.maxScale()))
        );
    }
}
