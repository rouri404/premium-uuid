package com.coto.premiumuuid.command;

import com.coto.premiumuuid.cache.UUIDCache;
import com.coto.premiumuuid.cache.UUIDCache.CacheEntry;
import com.coto.premiumuuid.config.PluginConfig;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles {@code /premiumuuid} (alias {@code /puuid}) with subcommands:
 * reload, lookup, clearcache.
 */
public final class PremiumUUIDCommand implements TabExecutor {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(java.time.ZoneOffset.UTC);

    private final PluginConfig config;
    private final UUIDCache cache;

    public PremiumUUIDCommand(PluginConfig config, UUIDCache cache) {
        this.config = config;
        this.cache = cache;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "lookup" -> handleLookup(sender, args, label);
            case "clearcache" -> handleClearCache(sender, args);
            default -> {
                sendUsage(sender, label);
                yield true;
            }
        };
    }

    // ── Subcommands ─────────────────────────────────────────────────────

    private boolean handleReload(CommandSender sender) {
        config.reload();
        sender.sendMessage(Component.text("PremiumUUID config reloaded.", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleLookup(CommandSender sender, String[] args, String label) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /" + label + " lookup <player>", NamedTextColor.RED));
            return true;
        }

        String target = args[1].toLowerCase();
        CacheEntry entry = cache.get(target);

        if (entry == null) {
            sender.sendMessage(Component.text("No cache entry for '" + args[1] + "'.", NamedTextColor.YELLOW));
            return true;
        }

        Instant checkedAt = Instant.ofEpochMilli(entry.lastChecked());
        boolean expired = !cache.isValid(entry, config.getCacheTtlMinutes());
        Duration age = Duration.between(checkedAt, Instant.now());

        sender.sendMessage(Component.text("─── Cache: " + args[1] + " ───", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  UUID: ", NamedTextColor.GRAY)
                .append(Component.text(entry.uuid().toString(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Premium: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(entry.premium()),
                        entry.premium() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text("  Last checked: ", NamedTextColor.GRAY)
                .append(Component.text(FORMATTER.format(checkedAt), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Age: ", NamedTextColor.GRAY)
                .append(Component.text(formatDuration(age), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  Expired: ", NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(expired),
                        expired ? NamedTextColor.RED : NamedTextColor.GREEN)));

        return true;
    }

    private boolean handleClearCache(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String target = args[1].toLowerCase();
            if (cache.remove(target)) {
                cache.save();
                sender.sendMessage(Component.text("Cache cleared for '" + args[1] + "'.", NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("No cache entry for '" + args[1] + "'.", NamedTextColor.YELLOW));
            }
        } else {
            cache.clear();
            cache.save();
            sender.sendMessage(Component.text("Entire UUID cache cleared.", NamedTextColor.GREEN));
        }
        return true;
    }

    // ── Tab completion ──────────────────────────────────────────────────

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return filterStartsWith(List.of("reload", "lookup", "clearcache"), args[0]);
        }
        if (args.length == 2 && ("lookup".equalsIgnoreCase(args[0]) || "clearcache".equalsIgnoreCase(args[0]))) {
            List<String> cached = new ArrayList<>();
            for (var entry : cache.entrySet()) {
                cached.add(entry.getKey());
            }
            return filterStartsWith(cached, args[1]);
        }
        return List.of();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(Component.text("Usage: /" + label + " <reload|lookup|clearcache> [player]", NamedTextColor.YELLOW));
    }

    private static List<String> filterStartsWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).toList();
    }

    private static String formatDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        if (hours > 0) {
            return hours + "h " + minutes + "m ago";
        }
        return minutes + "m ago";
    }
}
