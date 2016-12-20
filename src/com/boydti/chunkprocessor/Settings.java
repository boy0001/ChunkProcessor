package com.boydti.chunkprocessor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.configuration.file.YamlConfiguration;

public class Settings
{

    public static boolean CHUNK_PROCESSOR_GC;
    public static boolean CHUNK_PROCESSOR_TRIM_ON_SAVE;
    public static int CHUNK_PROCESSOR_MAX_ENTITIES = Integer.MAX_VALUE;
    public static int CHUNK_PROCESSOR_MAX_BLOCKSTATES = Integer.MAX_VALUE;
    public static boolean CHUNK_PROCESSOR_DISABLE_PHYSICS;
    public static boolean CHUNK_PROCESSOR_FIX_LIGHTING;

    public Settings(final File file)
    {
        if (!file.exists())
        {
            file.getParentFile().mkdirs();
            try
            {
                file.createNewFile();
            }
            catch (final IOException e)
            {
                e.printStackTrace();
            }
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        HashMap<String, Object> options = new HashMap<String, Object>();
        
        options.put("chunk-processor.auto-unload", Settings.CHUNK_PROCESSOR_GC);
        options.put("chunk-processor.auto-trim", Settings.CHUNK_PROCESSOR_TRIM_ON_SAVE);
        options.put("chunk-processor.max-blockstates", Settings.CHUNK_PROCESSOR_MAX_BLOCKSTATES);
        options.put("chunk-processor.max-entities", Settings.CHUNK_PROCESSOR_MAX_ENTITIES);
        options.put("chunk-processor.disable-physics", Settings.CHUNK_PROCESSOR_DISABLE_PHYSICS);
        options.put("chunk-processor.fix-lighting", Settings.CHUNK_PROCESSOR_FIX_LIGHTING);
        
        
        boolean changed = false;
        for (Entry<String, Object> entry : options.entrySet()) {
            if (!config.contains(entry.getKey())) {
                config.set(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        
        Settings.CHUNK_PROCESSOR_GC = config.getBoolean("chunk-processor.auto-unload");
        Settings.CHUNK_PROCESSOR_TRIM_ON_SAVE = config.getBoolean("chunk-processor.auto-trim");
        Settings.CHUNK_PROCESSOR_MAX_BLOCKSTATES = config.getInt("chunk-processor.max-blockstates");
        Settings.CHUNK_PROCESSOR_MAX_ENTITIES = config.getInt("chunk-processor.max-entities");
        Settings.CHUNK_PROCESSOR_DISABLE_PHYSICS = config.getBoolean("chunk-processor.disable-physics");
        Settings.CHUNK_PROCESSOR_FIX_LIGHTING = config.getBoolean("chunk-processor.fix-lighting");

        if (changed) {
            try {
                config.save(file);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
