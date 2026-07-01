package com.gamingmesh.jobs.stuff.complement;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;

import com.gamingmesh.jobs.i18n.MessageUtil;

public interface Complement {

    String getDisplayName(Player player);

    String getLine(SignChangeEvent event, int line);

    String getLine(Sign sign, int line);

    void setLine(SignChangeEvent event, int line, String text);

    void setLine(Sign sign, int line, String text);

    default void broadcastMessage(String message) {
	MessageUtil.broadcast(message);
    }

    default void broadcastMessage(List<String> messages) {
	for (String msg : messages) {
	    MessageUtil.broadcast(msg);
	}
    }
}
