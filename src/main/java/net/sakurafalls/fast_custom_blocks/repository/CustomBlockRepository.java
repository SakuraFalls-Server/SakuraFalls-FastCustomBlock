package net.sakurafalls.fast_custom_blocks.repository;

import net.sakurafalls.fast_custom_blocks.domain.CustomBlock;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Collection;

public interface CustomBlockRepository {

    void add(Location location, ItemStack item, float pitch, float yaw) throws IOException;
    void remove(Location location) throws IOException;

    Collection<CustomBlock> blocksAt(Chunk chunk) throws IOException;

    void saveAll() throws IOException;
    void saveOne(Location location) throws IOException;
    void reload() throws IOException;

}
