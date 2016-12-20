package com.boydti.chunkprocessor;

import static com.boydti.chunkprocessor.ReflectionUtils.getRefClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.boydti.chunkprocessor.ReflectionUtils.RefClass;
import com.boydti.chunkprocessor.ReflectionUtils.RefConstructor;
import com.boydti.chunkprocessor.ReflectionUtils.RefField;
import com.boydti.chunkprocessor.ReflectionUtils.RefMethod;

/**
 * An utility that can be used to send chunks, rather than using bukkit code to do so (uses heavy NMS)
 *

 */
public class SendChunk {
    
    private static SendChunk manager;
    
    static {
        try {
            manager = new SendChunk();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static SendChunk get() {
        return manager;
    }
    
    //    // Ref Class
    private final RefClass classEntityPlayer = getRefClass("{nms}.EntityPlayer");
    private final RefClass classMapChunk = getRefClass("{nms}.PacketPlayOutMapChunk");
    private final RefClass classPacket = getRefClass("{nms}.Packet");
    private final RefClass classConnection = getRefClass("{nms}.PlayerConnection");
    private final RefClass classChunk = getRefClass("{nms}.Chunk");
    private final RefClass classWorld = getRefClass("{nms}.World");
    private final RefClass classCraftPlayer = getRefClass("{cb}.entity.CraftPlayer");
    private final RefClass classCraftChunk = getRefClass("{cb}.CraftChunk");
    
    private final RefField mustSave = classChunk.getField("mustSave");
    
    private final RefClass classBlockPosition = getRefClass("{nms}.BlockPosition");
    private final RefClass classChunkSection = getRefClass("{nms}.ChunkSection");
    
    private final RefMethod methodGetHandlePlayer;
    private final RefMethod methodGetHandleChunk;
    private final RefConstructor MapChunk;
    private final RefField connection;
    private final RefMethod send;
    private final RefMethod methodInitLighting;
    private final RefConstructor classBlockPositionConstructor;
    private final RefMethod methodX;
    private final RefField fieldSections;
    private final RefField fieldWorld;
    private final RefMethod methodGetIdArray;
    private final short[] CACHE_ID;
    private final byte[][] CACHE_X;
    private final short[][] CACHE_Y;
    private final byte[][] CACHE_Z;
    
    /**
     * Constructor
     *
     * @throws NoSuchMethodException
     */
    public SendChunk() throws NoSuchMethodException {
        this.methodGetHandlePlayer = this.classCraftPlayer.getMethod("getHandle");
        this.methodGetHandleChunk = this.classCraftChunk.getMethod("getHandle");
        this.methodInitLighting = this.classChunk.getMethod("initLighting");
        this.MapChunk = this.classMapChunk.getConstructor(this.classChunk.getRealClass(), boolean.class, int.class);
        this.connection = this.classEntityPlayer.getField("playerConnection");
        this.send = this.classConnection.getMethod("sendPacket", this.classPacket.getRealClass());
        this.classBlockPositionConstructor = classBlockPosition.getConstructor(int.class, int.class, int.class);
        this.methodX = classWorld.getMethod("x", classBlockPosition.getRealClass());
        this.fieldSections = classChunk.getField("sections");
        this.fieldWorld = classChunk.getField("world");
        this.methodGetIdArray = classChunkSection.getMethod("getIdArray");
        this.CACHE_ID = new short[65535];
        for (int i = 0; i < 65535; i++) {
            int j = i >> 4;
            int k = i & 0xF;
            CACHE_ID[i] = (short) j;
        }
        this.CACHE_X = new byte[16][4096];
        this.CACHE_Y = new short[16][4096];
        this.CACHE_Z = new byte[16][4096];
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 256; y++) {
                    short i = (short) (y >> 4);
                    short j = (short) (((y & 0xF) << 8) | (z << 4) | x);
                    CACHE_X[i][j] = (byte) x;
                    CACHE_Y[i][j] = (short) y;
                    CACHE_Z[i][j] = (byte) z;
                }
            }
        }
    }
    
    public boolean unloadChunk(final String world, final Chunk chunk) {
        final Object c = methodGetHandleChunk.of(chunk).call();
        mustSave.of(c).set(false);
        if (chunk.isLoaded()) {
            chunk.unload(false, false);
        }
        return true;
    }
    
    public void fixLighting(Chunk chunk) {
        try {
            // Initialize lighting
            final Object c = this.methodGetHandleChunk.of(chunk).call();
            this.methodInitLighting.of(c).call();
            
            Object[] sections = (Object[]) fieldSections.of(c).get();
            Object w = fieldWorld.of(c).get();
            
            int X = chunk.getX() << 4;
            int Z = chunk.getZ() << 4;
            
            for (int j = 0; j < sections.length; j++) {
                Object section = sections[j];
                if (section == null) {
                    continue;
                }
                char[] array = (char[]) methodGetIdArray.of(section).call();
                for (int k = 0; k < array.length; k++) {
                    int i = array[k];
                    if (i < 16) {
                        continue;
                    }
                    short id = CACHE_ID[i];
                    switch (id) { // Lighting
                        case 10:
                        case 11:
                        case 39:
                        case 40:
                        case 50:
                        case 51:
                        case 62:
                        case 74:
                        case 76:
                        case 89:
                        case 122:
                        case 124:
                        case 130:
                        case 138:
                        case 169:
                            int x = CACHE_X[j][k];
                            int y = CACHE_Y[j][k];
                            int z = CACHE_Z[j][k];
                            Object pos = classBlockPositionConstructor.create(X + x, y, Z + z);
                            methodX.of(w).call(pos);
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        // Check for light sources
        
    }
    
    public void sendChunk(final Collection<Chunk> input) {
        final HashSet<Chunk> chunks = new HashSet<Chunk>(input);
        final HashMap<String, ArrayList<Chunk>> map = new HashMap<>();
        final int view = Bukkit.getServer().getViewDistance();
        for (final Chunk chunk : chunks) {
            final String world = chunk.getWorld().getName();
            ArrayList<Chunk> list = map.get(world);
            if (list == null) {
                list = new ArrayList<>();
                map.put(world, list);
            }
            list.add(chunk);
            fixLighting(chunk);
        }
        for (final Player player : Bukkit.getOnlinePlayers()) {
            final String world = player.getWorld().getName();
            final ArrayList<Chunk> list = map.get(world);
            if (list == null) {
                continue;
            }
            final Location loc = player.getLocation();
            final int cx = loc.getBlockX() >> 4;
            final int cz = loc.getBlockZ() >> 4;
            final Object entity = this.methodGetHandlePlayer.of(player).call();
            
            for (final Chunk chunk : list) {
                final int dx = Math.abs(cx - chunk.getX());
                final int dz = Math.abs(cz - chunk.getZ());
                if ((dx > view) || (dz > view)) {
                    continue;
                }
                final Object c = this.methodGetHandleChunk.of(chunk).call();
                chunks.remove(chunk);
                final Object con = this.connection.of(entity).get();
                final Object packet = this.MapChunk.create(c, true, 65535);
                this.send.of(con).call(packet);
            }
        }
        for (final Chunk chunk : chunks) {
            TaskManager.task(new Runnable() {
                @Override
                public void run() {
                    try {
                        chunk.unload(true, false);
                    } catch (final Exception e) {
                        final String worldname = chunk.getWorld().getName();
                        System.out.println("$4Could not save chunk: " + worldname + ";" + chunk.getX() + ";" + chunk.getZ());
                        System.out.println("$3 - $4File may be open in another process (e.g. MCEdit)");
                        System.out.println("$3 - $4" + worldname + "/level.dat or " + worldname + "level_old.dat may be corrupt (try repairing or removing these)");
                    }
                }
            });
        }
    }
    
    public void sendChunk(final String worldname, final List<Chunk> locs) {
        sendChunk(locs);
    }
}
