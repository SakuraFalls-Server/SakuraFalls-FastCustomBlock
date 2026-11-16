package net.sakurafalls.fast_custom_blocks.service;

import net.sakurafalls.fast_custom_blocks.FastCustomBlocksMain;
import net.sakurafalls.fast_custom_blocks.domain.CustomBlock;
import net.sakurafalls.fast_custom_blocks.repository.CustomBlockRepository;
import net.sakurafalls.fast_custom_blocks.repository.YamlCustomBlockRepository;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class CustomBlockService {

    private static final int REFRESH_RADIUS = 3;
    private static final long SAVE_DELAY_TICKS = 100L;

    private final Set<Long> dirtyChunks = ConcurrentHashMap.newKeySet();
    private final Map<Long, Location> dirtySamples = new ConcurrentHashMap<>();
    private final AtomicBoolean flushScheduled = new AtomicBoolean(false);

    private final CustomBlockRepository repository;

    public CustomBlockService() throws IOException {
        repository = new YamlCustomBlockRepository();
        repository.reload();
    }

    public void add(Location location, ItemStack item) {
        float pitch = location.getPitch();
        float yaw = location.getYaw();
        try {
            Location blockLocation = location.toBlockLocation();
            blockLocation.setPitch(0);
            blockLocation.setYaw(0);
            repository.add(blockLocation, item, pitch, yaw);
            markDirty(blockLocation);
            refreshAdd(CustomBlock.of(blockLocation, item, pitch, yaw));
        } catch (IOException ex) {
            FastCustomBlocksMain.getInstance().getLogger().severe("Could not add custom block " +
                location + ";" + item + ";" + pitch + ";" + yaw + "!");
            FastCustomBlocksMain.getInstance().getLogger().severe(ex.getClass() + ": " + ex.getMessage());
        }
    }

    public void remove(Location location) {
        try {
            Location blockLocation = location.toBlockLocation();
            blockLocation.setPitch(0);
            blockLocation.setYaw(0);
            repository.remove(blockLocation);
            markDirty(blockLocation);
            refreshRemove(blockLocation);
        } catch (IOException e) {
            FastCustomBlocksMain.getInstance().getLogger().severe("Could not remove custom block " +
                location + "!");
            FastCustomBlocksMain.getInstance().getLogger().severe(e.getClass() + ": " + e.getMessage());
        }
    }

    public CustomBlock getBlockAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        for (CustomBlock block : getBlocksAt(location.getChunk())) {
            Location at = block.location();
            if (at.getBlockX() == x && at.getBlockY() == y && at.getBlockZ() == z) {
                return block;
            }
        }
        return null;
    }

    public Collection<CustomBlock> getBlocksAt(Chunk chunk) {
        try {
            return repository.blocksAt(chunk);
        } catch (IOException e) {
            FastCustomBlocksMain.getInstance().getLogger().severe("Could not retrieve blocks at " +
                chunk.getX() + ";" + chunk.getZ());
            FastCustomBlocksMain.getInstance().getLogger().severe(e.getClass() + ": " + e.getMessage());
        }
        return Set.of();
    }

    public void saveAll() {
        try {
            repository.saveAll();
        } catch (IOException e) {
            FastCustomBlocksMain.getInstance().getLogger().severe("Could not save all! Something went wrong!");
            FastCustomBlocksMain.getInstance().getLogger().severe(e.getClass() + ": " + e.getMessage());
        }
    }

    private void markDirty(Location blockLocation) {
        Chunk chunk = blockLocation.getChunk();
        long key = ((long) chunk.getX() << 32) | (chunk.getZ() & 0xFFFFFFFFL);
        dirtyChunks.add(key);
        dirtySamples.put(key, blockLocation);
        scheduleFlush();
    }

    private void scheduleFlush() {
        if (!flushScheduled.compareAndSet(false, true)) {
            return;
        }
        FastCustomBlocksMain.getInstance().getServer().getScheduler()
            .runTaskLaterAsynchronously(FastCustomBlocksMain.getInstance(), () -> {
                try {
                    for (Long key : new HashSet<>(dirtyChunks)) {
                        Location sample = dirtySamples.get(key);
                        if (sample == null) {
                            dirtyChunks.remove(key);
                            continue;
                        }
                        try {
                            repository.saveOne(sample);
                        } catch (IOException e) {
                            FastCustomBlocksMain.getInstance().getLogger().severe(
                                "Could not save custom blocks, chunk key " + key + "!");
                            FastCustomBlocksMain.getInstance().getLogger().severe(e.getClass() + ": " +
                                e.getMessage());
                        }
                        dirtyChunks.remove(key);
                    }
                } finally {
                    flushScheduled.set(false);
                    if (!dirtyChunks.isEmpty()) {
                        scheduleFlush();
                    }
                }
            }, SAVE_DELAY_TICKS);
    }

    private void refreshAdd(CustomBlock block) {
        if (FastCustomBlocksMain.getFakeBlockRenderer() == null) {
            return;
        }
        Location location = block.location();
        if (location.getWorld() == null) {
            return;
        }
        int x = location.getBlockX() >> 4;
        int z = location.getBlockZ() >> 4;
        for (Player player : location.getWorld().getPlayers()) {
            Chunk playerChunk = player.getLocation().getChunk();
            if (Math.abs(playerChunk.getX() - x) > REFRESH_RADIUS ||
                Math.abs(playerChunk.getZ() - z) > REFRESH_RADIUS) {
                continue;
            }
            FastCustomBlocksMain.getFakeBlockRenderer().showSingle(player, block);
        }
    }

    private void refreshRemove(Location blockLocation) {
        if (FastCustomBlocksMain.getFakeBlockRenderer() == null
            || blockLocation.getWorld() == null) {
            return;
        }
        for (Player player : blockLocation.getWorld().getPlayers()) {
            FastCustomBlocksMain.getFakeBlockRenderer().removeSingle(player, blockLocation);
        }
    }

}
