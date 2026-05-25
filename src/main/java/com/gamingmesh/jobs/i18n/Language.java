/**
 * Jobs Plugin for Bukkit
 * Copyright (C) 2011 Zak Ford <zak.j.ford@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gamingmesh.jobs.i18n;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.config.YmlMaker;
import com.gamingmesh.jobs.container.Job;

import net.Zrips.CMILib.Colors.CMIChatColor;
import net.Zrips.CMILib.Messages.CMIMessages;

public class Language {

    public FileConfiguration enlocale, customlocale;

    private static final Pattern NEW_LINE_PATTERN = Pattern.compile("([ ]?[\\/][n][$|\\s])");
    private static final Pattern LANG_TAG_PATTERN = Pattern.compile("^<lang:([^>]+)>$");

    private static final Pattern JOB_NAME_PATTERN = Pattern.compile("(?i)[\\[%]jobname[\\]%]");
    private static final Pattern JOB_DISPLAY_NAME_PATTERN = Pattern.compile("(?i)[\\[%]jobdisplayname[\\]%]");
    private static final Pattern JOB_FULL_NAME_PATTERN = Pattern.compile("(?i)[\\[%]jobfullname[\\]%]");

    /**
     * Reloads the config
     */
    public void reload() {
        String ls = Jobs.getGCManager().localeString.toLowerCase();
        customlocale = new YmlMaker(Jobs.getFolder(), "locale/messages_" + ls + ".yml").getConfig();
        enlocale = new YmlMaker(Jobs.getFolder(), "locale/messages_en.yml").getConfig();
        if (customlocale == null)
            customlocale = enlocale;
    }

    public static String updateJob(String text, Job job) {
        text = replacePattern(JOB_NAME_PATTERN, text, job.getName());
        text = replacePattern(JOB_DISPLAY_NAME_PATTERN, text, job.getDisplayName());
        return replacePattern(JOB_FULL_NAME_PATTERN, text, job.getJobFullName());
    }

    private static String replacePattern(Pattern pattern, String text, String replacement) {
        return pattern.matcher(text).replaceAll(replacement);
    }

    public static void sendMessage(CommandSender sender, String key, Object... variables) {
        String msg = Jobs.getLanguage().getMessage(key, variables);
        if (msg.isEmpty())
            return;
        sender.sendMessage(msg);
    }

    /**
     * Get the message with the correct key
     * @param key - the key of the message
     * @return the message
     */
    public String getMessage(String key) {
        return getMessage(key, "");
    }

    public String getMessage(String key, Object... variables) {
        String missing = "MLF " + key;
        String msg = "";
        try {
            if (!customlocale.contains(key))
                msg = enlocale.isString(key) ? CMIChatColor.translate(enlocale.getString(key)) : missing;
            else
                msg = customlocale.isString(key) ? CMIChatColor.translate(customlocale.getString(key)) : missing;
        } catch (Exception e) {
            CMIMessages.consoleMessage("&e[Jobs] &2Can't read language file for: " + key);
            CMIMessages.consoleMessage(e.getLocalizedMessage());
            return "";
        }

        if (msg.isEmpty() || msg.equals(missing)) {
            msg = "";
            try {

                List<String> ls = null;

                if (customlocale.isList(key))
                    ls = colorsArray(customlocale.getStringList(key), true);
                else if (enlocale.isList(key)) {
                    ls = enlocale.getStringList(key);
                    ls = !ls.isEmpty() ? colorsArray(ls, true) : Arrays.asList(missing);
                }

                if (ls != null)
                    for (String one : ls) {
                        if (!msg.isEmpty())
                            msg += "\n";
                        msg += one;
                    }
            } catch (Exception e) {
                CMIMessages.consoleMessage("&e[Jobs] &2Can't read language file for: " + key);
                CMIMessages.consoleMessage(e.getLocalizedMessage());
                return "";
            }
        }

        if (isLangTag(msg)) {
            return appendLangArgs(msg, variables);
        }
        return applyVariables(msg, variables);
    }

    public String getMessage(Player player, String key, Object... variables) {
        String template = getMessage(key);
        if (player == null || template == null || template.isEmpty()) {
            return applyVariables(template, variables);
        }
        String msg = isLangTag(template) ? appendLangArgs(template, variables) : applyVariables(template, variables);
        msg = Jobs.getInstance().getPlaceholderAPIManager().updatePlaceHolders(player, msg);
        return isLangTag(template) ? msg : applyVariables(msg, variables);
    }

    /**
     * Get the message with the correct key
     * @param key - the key of the message
     * @return the message
     */
    public List<String> getMessageList(String key, Object... variables) {
        String missing = "MLF " + key + " ";

        List<String> ls;
        if (customlocale.isList(key))
            ls = colorsArray(customlocale.getStringList(key), true);
        else {
            ls = enlocale.getStringList(key);
            ls = !ls.isEmpty() ? colorsArray(ls, true) : Arrays.asList(missing);
        }

        if (variables != null && variables.length > 0)
            for (int i = 0; i < ls.size(); i++) {
                String msg = ls.get(i);
                msg = isLangTag(msg) ? appendLangArgs(msg, variables) : applyVariables(msg, variables);
                ls.set(i, CMIChatColor.translate(filterNewLine(msg)));
            }

        return ls;
    }

    public List<String> getMessageList(Player player, String key, Object... variables) {
        List<String> ls = getMessageList(key);
        if (player == null || ls == null || ls.isEmpty()) {
            for (int i = 0; i < ls.size(); i++) {
                ls.set(i, applyVariables(ls.get(i), variables));
            }
            return ls;
        }
        for (int i = 0; i < ls.size(); i++) {
            String line = ls.get(i);
            boolean langTag = isLangTag(line);
            line = langTag ? appendLangArgs(line, variables) : applyVariables(line, variables);
            line = Jobs.getInstance().getPlaceholderAPIManager().updatePlaceHolders(player, line);
            ls.set(i, langTag ? line : applyVariables(line, variables));
        }
        return ls;
    }

    private String applyVariables(String msg, Object... variables) {
        if (msg == null || msg.isEmpty() || variables == null || variables.length == 0)
            return msg;

        for (int i = 0; i < variables.length; i++) {
            if (variables[i] instanceof Job) {
                msg = updateJob(msg, (Job) variables[i]);
                continue;
            }

            if (variables.length <= i + 1)
                break;

            String rawToken = String.valueOf(variables[i]);
            String replacement = String.valueOf(variables[i + 1]);
            msg = msg.replace(rawToken, replacement);
            String normalizedToken = normalizeVariableToken(rawToken);
            if (normalizedToken != null && !normalizedToken.isEmpty()) {
                msg = replaceTokenIgnoreCase(msg, "%", "%", normalizedToken, replacement);
                msg = replaceTokenIgnoreCase(msg, "[", "]", normalizedToken, replacement);
            }
            i++;
        }
        return msg;
    }

    private String replaceTokenIgnoreCase(String input, String prefix, String suffix, String key, String replacement) {
        String pattern = "(?i)" + Pattern.quote(prefix) + Pattern.quote(key) + Pattern.quote(suffix);
        return input.replaceAll(pattern, Matcher.quoteReplacement(replacement));
    }

    private boolean isLangTag(String msg) {
        return msg != null && LANG_TAG_PATTERN.matcher(msg.trim()).matches();
    }

    private String appendLangArgs(String msg, Object... variables) {
        if (msg == null || msg.isEmpty() || variables == null || variables.length == 0) {
            return msg;
        }

        Matcher matcher = LANG_TAG_PATTERN.matcher(msg.trim());
        if (!matcher.matches()) {
            return msg;
        }

        StringBuilder suffix = new StringBuilder();
        for (int i = 0; i < variables.length; i++) {
            Object raw = variables[i];
            if (raw instanceof Job) {
                Job job = (Job) raw;
                appendLangArg(suffix, "jobname", job.getName());
                appendLangArg(suffix, "jobdisplayname", job.getDisplayName());
                appendLangArg(suffix, "jobfullname", job.getJobFullName());
                continue;
            }

            if (variables.length <= i + 1) {
                break;
            }

            String token = normalizeVariableToken(raw);
            if (token != null && !token.isEmpty()) {
                appendLangArg(suffix, token, String.valueOf(variables[i + 1]));
            }
            i++;
        }

        if (suffix.length() == 0) {
            return msg;
        }

        return "<lang:" + matcher.group(1) + suffix + ">";
    }

    private String normalizeVariableToken(Object tokenObj) {
        if (tokenObj == null) {
            return null;
        }
        String token = String.valueOf(tokenObj).trim();
        if (token.isEmpty()) {
            return null;
        }
        if (token.startsWith("%") && token.endsWith("%") && token.length() > 2) {
            token = token.substring(1, token.length() - 1);
        } else if (token.startsWith("[") && token.endsWith("]") && token.length() > 2) {
            token = token.substring(1, token.length() - 1);
        }
        token = token.replaceAll("[^a-zA-Z0-9_\\-]", "");
        return token.isEmpty() ? null : token;
    }

    private void appendLangArg(StringBuilder suffix, String key, String value) {
        if (value == null) {
            return;
        }
        suffix.append("|").append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    public String filterNewLine(String msg) {
        Matcher match = NEW_LINE_PATTERN.matcher(msg);
        while (match.find()) {
            msg = msg.replace(match.group(0), "\n");
        }
        return msg;
    }

    public List<String> colorsArray(List<String> text, boolean colorize) {
        List<String> temp = new ArrayList<>();

        for (String part : text) {
            if (colorize)
                part = CMIChatColor.translate(part);

            temp.add(part);
        }

        return temp;
    }
}
