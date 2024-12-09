package com.liphium.elfhunt;

import com.liphium.core.Core;
import com.liphium.elfhunt.command.SetCommand;
import com.liphium.elfhunt.command.TimerCommand;
import com.liphium.elfhunt.game.GameManager;
import com.liphium.elfhunt.listener.ChatListener;
import com.liphium.elfhunt.listener.GameListener;
import com.liphium.elfhunt.listener.JoinQuitListener;
import com.liphium.elfhunt.listener.machines.Machine;
import com.liphium.elfhunt.listener.machines.MachineManager;
import com.liphium.elfhunt.screens.BrewingScreen;
import com.liphium.elfhunt.screens.ItemShopScreen;
import com.liphium.elfhunt.screens.TeamSelectionScreen;
import com.liphium.elfhunt.util.TaskManager;
import com.onarandombox.MultiverseCore.MultiverseCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class Elfhunt extends JavaPlugin {
    public static final Component PREFIX = Component.text("[", NamedTextColor.DARK_GRAY)
            .append(Component.text("Vampires", NamedTextColor.RED))
            .append(Component.text("]", NamedTextColor.DARK_GRAY))
            .append(Component.text(" "));

    private static Elfhunt instance;
    private MultiverseCore core;

    private TaskManager taskManager;

    private GameManager gameManager;

    private MachineManager machineManager;

    @Override
    public void onEnable() {
        instance = this;
        Core.init();

        // Initialize multiverse core and stuff
        core = (MultiverseCore) getServer().getPluginManager().getPlugin("Multiverse-Core");
        assert core != null;
        core.cloneWorld("world", "vampires", "");

        taskManager = new TaskManager();
        taskManager.initTask();

        gameManager = new GameManager();

        machineManager = new MachineManager();

        Listener[] listeners = new Listener[]{new GameListener(), new ChatListener(), new JoinQuitListener()};
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }

        getCommand("set").setExecutor(new SetCommand());
        getCommand("timer").setExecutor(new TimerCommand());

        Core.getInstance().getScreens().register(new TeamSelectionScreen(), new BrewingScreen(), new ItemShopScreen());
    }

    @Override
    public void onDisable() {
        for (Machine machine : machineManager.getMachines()) {
            machine.destroy();
        }

        core.deleteWorld("vampires");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public MachineManager getMachineManager() {
        return machineManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public static Elfhunt getInstance() {
        return instance;
    }
}
