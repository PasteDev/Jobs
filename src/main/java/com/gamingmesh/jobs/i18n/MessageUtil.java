package com.gamingmesh.jobs.i18n;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.gamingmesh.jobs.Jobs;

import net.Zrips.CMILib.ActionBar.CMIActionBar;
import net.Zrips.CMILib.Colors.CMIChatColor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class MessageUtil {

    private static final Pattern LANG_TAG_PATTERN = Pattern.compile("<lang:[^>]+>");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.builder()
        .character('&')
        .hexColors()
        .useUnusualXRepeatedCharacterHexFormat()
        .build();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();

    private MessageUtil() {
    }

    public static boolean looksLikeMiniMessage(String message) {
        if (message == null || message.isEmpty() || message.indexOf('<') < 0) {
            return false;
        }
        String withoutLang = LANG_TAG_PATTERN.matcher(message).replaceAll("");
        return withoutLang.matches(".*<[a-zA-Z#/!?].*");
    }

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        if (looksLikeMiniMessage(message)) {
            return message;
        }
        return CMIChatColor.translate(message);
    }

    public static void send(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        sendComponent(sender, toComponent(message));
    }

    public static void broadcast(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        Component component = toComponent(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendComponent(player, component);
        }
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        if (Jobs.getInstance().isKyoriSupported() && looksLikeMiniMessage(message) && player instanceof Audience) {
            ((Audience) player).sendActionBar(toComponent(message));
            return;
        }
        CMIActionBar.send(player, colorize(message));
    }

    private static Component toComponent(String message) {
        if (Jobs.getInstance().isKyoriSupported() && looksLikeMiniMessage(message)) {
            try {
                if (message.contains("<lang:")) {
                    return deserializeWithLangTags(message);
                }
                return MINI_MESSAGE.deserialize(message);
            } catch (Exception ignored) {
            }
        }
        return LEGACY_AMPERSAND.deserialize(colorize(message));
    }

    private static Component deserializeWithLangTags(String message) {
        Matcher matcher = LANG_TAG_PATTERN.matcher(message);
        List<String> parts = new ArrayList<>();
        int last = 0;
        while (matcher.find()) {
            if (matcher.start() > last) {
                parts.add(message.substring(last, matcher.start()));
            }
            parts.add(matcher.group());
            last = matcher.end();
        }
        if (last < message.length()) {
            parts.add(message.substring(last));
        }

        Component result = Component.empty();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (part.startsWith("<lang:")) {
                result = result.append(Component.text(part));
            } else {
                result = result.append(MINI_MESSAGE.deserialize(part));
            }
        }
        return result;
    }

    private static void sendComponent(CommandSender sender, Component component) {
        if (sender instanceof Audience) {
            ((Audience) sender).sendMessage(component);
            return;
        }
        sender.sendMessage(LEGACY_SECTION.serialize(component));
    }
}
