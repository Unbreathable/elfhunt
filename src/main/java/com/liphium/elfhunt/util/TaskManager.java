package com.liphium.elfhunt.util;

import com.liphium.elfhunt.Elfhunt;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;

public class TaskManager {

    private final ArrayList<Runnable> runnables = new ArrayList<>();
    private final ArrayList<Runnable> toRemove = new ArrayList<>();

    public void initTask() {

        new BukkitRunnable() {
            @Override
            public void run() {

                for (Runnable rem : toRemove) {
                    runnables.remove(rem);
                }

                for (Runnable runnable : runnables) {
                    runnable.run();
                }
            }
        }.runTaskTimer(Elfhunt.getInstance(), 0, 1);

    }

    public void inject(Runnable runnable) {
        runnables.add(runnable);
    }

    public void uninject(Runnable runnable) {
        toRemove.add(runnable);
    }

}
