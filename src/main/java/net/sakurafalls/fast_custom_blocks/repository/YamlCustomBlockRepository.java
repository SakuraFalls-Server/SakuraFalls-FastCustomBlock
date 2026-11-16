package net.sakurafalls.fast_custom_blocks.repository;

import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import net.sakurafalls.fast_custom_blocks.FastCustomBlocksMain;
import net.sakurafalls.fast_custom_blocks.domain.CustomBlock;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public class YamlCustomBlockRepository implements CustomBlockRepository {

    private final File yamlPath = new File(FastCustomBlocksMain.getInstance().getDataFolder(), "data");
    private final Map<IntIntImmutablePair, ObjectObjectImmutablePair<File, YamlConfiguration>> yamlFiles
        = new Object2ObjectOpenHashMap<>();

    private final Map<IntIntImmutablePair, Map<Location, CustomBlock>> chunksInMemory =
        new Object2ObjectOpenHashMap<>();

    @Override
    public void add(Location location, ItemStack item, float pitch, float yaw) throws IOException {
        IntIntImmutablePair key = chunkToXZPair(location.getChunk());
        ensureChunkIsInMemory(key);
        Location id = keyOf(location);
        chunksInMemory.get(key).put(id, CustomBlock.of(id, item, pitch, yaw));
        if (!yamlFiles.containsKey(key)) {
            writeNewChunkFile(key);
        }
        YamlConfiguration yaml = yamlFiles.get(key).right();
        yaml.set("d." + locationToString(location) + ".x", location.getBlockX());
        yaml.set("d." + locationToString(location) + ".y", location.getBlockY());
        yaml.set("d." + locationToString(location) + ".z", location.getBlockZ());
        yaml.set("d." + locationToString(location) + ".w", location.getWorld().getName());
        yaml.set("d." + locationToString(location) + ".i", item);
        yaml.set("d." + locationToString(location) + ".p", pitch);
        yaml.set("d." + locationToString(location) + ".y", yaw);
    }

    @Override
    public void remove(Location location) throws IOException {
        IntIntImmutablePair key = chunkToXZPair(location.getChunk());
        ensureChunkIsInMemory(key);
        chunksInMemory.get(key).remove(keyOf(location));
        if (!yamlFiles.containsKey(key)) {
            throw new IOException("No blocks at chunk " + key + "!");
        }
        YamlConfiguration yaml = yamlFiles.get(key).right();
        yaml.set("d." + locationToString(location), null);
    }

    @Override
    public Collection<CustomBlock> blocksAt(Chunk chunk) throws IOException {
        IntIntImmutablePair key = chunkToXZPair(chunk);
        ensureChunkIsInMemory(key);
        return chunksInMemory.get(key).values();
    }

    @Override
    public synchronized void saveAll() throws IOException {
        for (ObjectObjectImmutablePair<File, YamlConfiguration> filePair : yamlFiles.values()) {
            filePair.right().save(filePair.left());
        }
    }

    @Override
    public void saveOne(Location location) throws IOException {
        IntIntImmutablePair key = chunkToXZPair(location.getChunk());
        ObjectObjectImmutablePair<File, YamlConfiguration> filePair = yamlFiles.get(key);
        if (filePair == null) {
            throw new IOException("No blocks at chunk " + key + "!");
        }
        synchronized (filePair) {
            filePair.right().save(filePair.left());
        }
    }

    @Override
    public void reload() throws IOException {
        if (!yamlPath.exists()) {
            if (!yamlPath.mkdirs()) {
                throw new IOException("Cannot create data directory!");
            }
        }
        File[] files = yamlPath.listFiles();
        assert files != null;
        for (File file : files) {
            String[] chunkSplit = file.getName().split("\\.")[0].split(",");
            int x = Integer.parseInt(chunkSplit[0]);
            int z = Integer.parseInt(chunkSplit[1]);
            IntIntImmutablePair chunkPair = IntIntImmutablePair.of(x, z);
            ObjectObjectImmutablePair<File, YamlConfiguration> filePair =
                ObjectObjectImmutablePair.of(file, YamlConfiguration.loadConfiguration(file));
            yamlFiles.put(chunkPair, filePair);
        }
    }

    private Map<Location, CustomBlock> readChunkFromYaml(IntIntImmutablePair key) {
        Map<Location, CustomBlock> result = new Object2ObjectOpenHashMap<>();
        ObjectObjectImmutablePair<File, YamlConfiguration> filePair = yamlFiles.get(key);
        if (filePair == null) {
            return result; // must be mutable!
        }
        synchronized (filePair) {
            YamlConfiguration yaml = filePair.right();
            ConfigurationSection data = yaml.getConfigurationSection("d");
            if (data == null) {
                return result;
            }
            for (String locationString : data.getKeys(false)) {
                String worldName = data.getString(locationString + ".w");
                if (worldName == null) {
                    continue;
                }
                World world = FastCustomBlocksMain.getInstance().getServer().getWorld(worldName);
                if (world == null) {
                    continue;
                }
                int x = data.getInt(locationString + ".x");
                int y = data.getInt(locationString + ".y");
                int z = data.getInt(locationString + ".z");
                Location location = new Location(world, x, y, z);
                ItemStack item = data.getItemStack(locationString + ".i");
                float pitch = (float) data.getDouble(locationString + ".p");
                float yaw = (float) data.getDouble(locationString + ".y");
                result.put(location, CustomBlock.of(location, item, pitch, yaw));
            }
            return result;
        }
    }

    private void ensureChunkIsInMemory(IntIntImmutablePair chunkPair) throws IOException {
        if (!chunksInMemory.containsKey(chunkPair)) {
            chunksInMemory.put(chunkPair, readChunkFromYaml(chunkPair));
        }
    }

    private void writeNewChunkFile(IntIntImmutablePair chunkPair) throws IOException {
        String fileName = chunkPairToString(chunkPair) + ".yml";
        File file = new File(yamlPath, fileName);
        if (!file.createNewFile()) {
             throw new IOException("Cannot create " + fileName + "!");
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yamlFiles.put(chunkPair, ObjectObjectImmutablePair.of(file, yaml));
    }

    private static IntIntImmutablePair chunkToXZPair(Chunk chunk) {
        return IntIntImmutablePair.of(chunk.getX(), chunk.getZ());
    }

    private static Location keyOf(Location location) {
        return new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    private static String chunkPairToString(IntIntImmutablePair chunk) {
        return chunk.leftInt() + "," + chunk.rightInt();
    }

    private static String locationToString(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + "," +
            location.getWorld().getName();
    }

}
