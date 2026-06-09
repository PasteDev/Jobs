package com.gamingmesh.jobs;

import com.gamingmesh.jobs.economy.VaultEconomy;
import net.Zrips.CMILib.Messages.CMIMessages;
import net.Zrips.CMILib.Version.Schedulers.CMIScheduler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class HookEconomyTask {

    private static final int MAX_ATTEMPTS = 40;

    private HookEconomyTask() {
    }

    public static void schedule() {
        if (!HookVault.isVaultEnable()) {
            return;
        }
        CMIScheduler.runTask(Jobs.getInstance(), () -> tryHook(0));
    }

    private static void tryHook(int attempt) {
        RegisteredServiceProvider<Economy> provider = Jobs.getInstance()
                .getServer()
                .getServicesManager()
                .getRegistration(Economy.class);

        if (provider != null) {
            Jobs.setEconomy(new VaultEconomy(provider.getProvider()));
            CMIMessages.consoleMessage("&e[" + Jobs.getInstance().getName() + "] Successfully linked with Vault economy. ("
                    + provider.getPlugin().getName() + ")");
            return;
        }

        if (attempt + 1 < MAX_ATTEMPTS) {
            CMIScheduler.runTaskLater(Jobs.getInstance(), () -> tryHook(attempt + 1), 1L);
            return;
        }

        Jobs.getPluginLogger().severe("==================== " + Jobs.getInstance().getDescription().getName() + " ====================");
        Jobs.getPluginLogger().severe("Vault detected but Economy plugin still missing!");
        Jobs.getPluginLogger().severe("Please install Vault supporting Economy plugin!");
        Jobs.getPluginLogger().severe("==============================================");
    }
}
