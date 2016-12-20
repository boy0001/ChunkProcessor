package com.boydti.chunkprocessor;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ChunkListener extends JavaPlugin implements Listener {
    
    public static ChunkListener plugin;

    @Override
    public void onEnable() {
        new Settings(new File(getDataFolder(), "settings.yml"));
        Bukkit.getPluginManager().registerEvents(this, this);
        ChunkListener.plugin = this;
    }

    private Chunk lastChunk = null;
    
    public ChunkListener() {
        if (!Settings.CHUNK_PROCESSOR_GC) {
            return;
        }
        // TODO auto trimming (not implemented now as too much work to copy over)
    }
    

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent event) {
        if (Settings.CHUNK_PROCESSOR_TRIM_ON_SAVE && SendChunk.get() != null) {
            final Chunk chunk = event.getChunk();
            final String world = chunk.getWorld().getName();
            if (SendChunk.get().unloadChunk(world, chunk)) {
                return;
            }
        }
        if (processChunk(event.getChunk(), true)) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onChunkLoad(final ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        processChunk(chunk, false);
        if (Settings.CHUNK_PROCESSOR_FIX_LIGHTING && SendChunk.get() != null) {
            SendChunk.get().fixLighting(chunk);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemSpawn(final ItemSpawnEvent event) {
        final Item entity = event.getEntity();
        final Chunk chunk = entity.getLocation().getChunk();
        if (chunk == lastChunk) {
            event.getEntity().remove();
            event.setCancelled(true);
            return;
        }
        final Entity[] entities = chunk.getEntities();
        if (entities.length > Settings.CHUNK_PROCESSOR_MAX_ENTITIES) {
            event.getEntity().remove();
            event.setCancelled(true);
            lastChunk = chunk;
        } else {
            lastChunk = null;
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(final BlockPhysicsEvent event) {
        if (Settings.CHUNK_PROCESSOR_DISABLE_PHYSICS) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySpawn(final CreatureSpawnEvent event) {
        final LivingEntity entity = event.getEntity();
        final Chunk chunk = entity.getLocation().getChunk();
        if (chunk == lastChunk) {
            event.getEntity().remove();
            event.setCancelled(true);
            return;
        }
        final Entity[] entities = chunk.getEntities();
        if (entities.length > Settings.CHUNK_PROCESSOR_MAX_ENTITIES) {
            event.getEntity().remove();
            event.setCancelled(true);
            lastChunk = chunk;
        } else {
            lastChunk = null;
        }
    }
    
    public void cleanChunk(final Chunk chunk) {
        TaskManager.index.incrementAndGet();
        final Integer currentIndex = TaskManager.index.get();
        final Integer task = TaskManager.taskRepeat(new Runnable() {
            @Override
            public void run() {
                if (!chunk.isLoaded()) {
                    Bukkit.getScheduler().cancelTask(TaskManager.tasks.get(currentIndex));
                    TaskManager.tasks.remove(currentIndex);
                    System.out.println("[ChunkProcessor] &aSuccessfully processed and unloaded chunk!");
                    chunk.unload(true, true);
                    return;
                }
                final BlockState[] tiles = chunk.getTileEntities();
                if (tiles.length == 0) {
                    Bukkit.getScheduler().cancelTask(TaskManager.tasks.get(currentIndex));
                    TaskManager.tasks.remove(currentIndex);
                    System.out.println("[ChunkProcessor] &aSuccessfully processed and unloaded chunk!");
                    chunk.unload(true, true);
                    return;
                }
                final long start = System.currentTimeMillis();
                int i = 0;
                while ((System.currentTimeMillis() - start) < 250) {
                    if (i >= tiles.length) {
                        Bukkit.getScheduler().cancelTask(TaskManager.tasks.get(currentIndex));
                        TaskManager.tasks.remove(currentIndex);
                        System.out.println("[ChunkProcessor] &aSuccessfully processed and unloaded chunk!");
                        chunk.unload(true, true);
                        return;
                    }
                    tiles[i].getBlock().setType(Material.AIR, false);
                    i++;
                }
            }
        }, 5);
        TaskManager.tasks.put(currentIndex, task);
    }
    
    public boolean processChunk(final Chunk chunk, final boolean unload) {
        final Entity[] entities = chunk.getEntities();
        final BlockState[] tiles = chunk.getTileEntities();
        if (entities.length > Settings.CHUNK_PROCESSOR_MAX_ENTITIES) {
            for (final Entity ent : entities) {
                if (!(ent instanceof Player)) {
                    ent.remove();
                }
            }
            System.out.println("[ChunkProcessor] &a detected unsafe chunk and processed: " + (chunk.getX() << 4) + "," + (chunk.getX() << 4));
        }
        if (tiles.length > Settings.CHUNK_PROCESSOR_MAX_BLOCKSTATES) {
            if (unload) {
                System.out.println("[ChunkProcessor] &c detected unsafe chunk: " + (chunk.getX() << 4) + "," + (chunk.getX() << 4));
                cleanChunk(chunk);
                return true;
            }
            for (final BlockState tile : tiles) {
                tile.getBlock().setType(Material.AIR, false);
            }
        }
        return false;
    }
}
