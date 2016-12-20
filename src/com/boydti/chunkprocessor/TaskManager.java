package com.boydti.chunkprocessor;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;

public class TaskManager {
    public static int taskRepeat(final Runnable r, final int interval) {
        return ChunkListener.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(ChunkListener.plugin, r, interval, interval);
    }

    public static AtomicInteger index = new AtomicInteger(0);
    public static HashMap<Integer, Integer> tasks = new HashMap<>();

    public static void taskAsync(final Runnable r) {
        if (r == null) {
            return;
        }
        ChunkListener.plugin.getServer().getScheduler().runTaskAsynchronously(ChunkListener.plugin, r).getTaskId();
    }

    public static void task(final Runnable r) {
        if (r == null) {
            return;
        }
        ChunkListener.plugin.getServer().getScheduler().runTask(ChunkListener.plugin, r).getTaskId();
    }

    public static void taskLater(final Runnable r, final int delay) {
        if (r == null) {
            return;
        }
        ChunkListener.plugin.getServer().getScheduler().runTaskLater(ChunkListener.plugin, r, delay).getTaskId();
    }

    public static void taskLaterAsync(final Runnable r, final int delay) {
        ChunkListener.plugin.getServer().getScheduler().runTaskLaterAsynchronously(ChunkListener.plugin, r, delay);
    }

    public static void cancelTask(final int task) {
        if (task != -1) {
            Bukkit.getScheduler().cancelTask(task);
        }
    }
}
