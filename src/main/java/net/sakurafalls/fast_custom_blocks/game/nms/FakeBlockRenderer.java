package net.sakurafalls.fast_custom_blocks.game.nms;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.sakurafalls.fast_custom_blocks.domain.CustomBlock;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fake block renderer based on Denizen implementation.
 *
 * @see <a href="https://github.com/DenizenScript/Denizen/blob/dev/v26_1/src/main/java/com/denizenscript/denizen/nms/v26_1/helpers/PlayerHelperImpl.java#L154">DenizenScript </a>
 */
public final class FakeBlockRenderer {

    private static final float VIEW_RANGE = 2.0F;

    private final Map<UUID, Map<Long, Map<Long, Integer>>> visible = new ConcurrentHashMap<>();

    public void show(Player player, Chunk chunk, Collection<CustomBlock> blocks) {
        if (!player.isOnline() || !player.getWorld().equals(chunk.getWorld()) || blocks.isEmpty()) {
            return;
        }
        Map<Long, Integer> shown = visible
            .computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
            .computeIfAbsent(chunkKey(chunk), ignored -> new ConcurrentHashMap<>());
        ServerLevel level = ((CraftWorld) chunk.getWorld()).getHandle();
        ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        for (CustomBlock block : blocks) {
            Location at = block.location();
            long blockKey = BlockPos.asLong(at.getBlockX(), at.getBlockY(), at.getBlockZ());
            if (shown.containsKey(blockKey)) {
                continue;
            }
            shown.put(blockKey, spawnDisplay(nmsPlayer, level, block));
        }
    }

    public void showSingle(Player player, CustomBlock block) {
        Location at = block.location();
        if (at.getWorld() == null || !player.isOnline() || !player.getWorld().equals(at.getWorld())) {
            return;
        }
        long chunkKey = ((long) (at.getBlockX() >> 4) << 32)
            | ((at.getBlockZ() >> 4) & 0xFFFFFFFFL);
        Map<Long, Integer> shown = visible
            .computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
            .computeIfAbsent(chunkKey, ignored -> new ConcurrentHashMap<>());
        long blockKey = BlockPos.asLong(at.getBlockX(), at.getBlockY(), at.getBlockZ());
        if (shown.containsKey(blockKey)) {
            return;
        }
        ServerLevel level = ((CraftWorld) at.getWorld()).getHandle();
        ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        shown.put(blockKey, spawnDisplay(nmsPlayer, level, block));
    }

    private static int spawnDisplay(ServerPlayer nmsPlayer, ServerLevel level, CustomBlock block) {
        Location at = block.location();
        int x = at.getBlockX();
        int y = at.getBlockY();
        int z = at.getBlockZ();
        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level);
        display.setId(Bukkit.getUnsafe().nextEntityId()); // we are aware of the risk here
        display.setUUID(UUID.randomUUID());
        display.setPos(x + 0.5, y + 0.5, z + 0.5);
        display.setXRot(block.pitch());
        display.setYRot(block.yaw());
        display.setItemStack(CraftItemStack.asNMSCopy(block.item()));
        display.setItemTransform(ItemDisplayContext.FIXED);
        display.setViewRange(VIEW_RANGE);
        nmsPlayer.connection.send(new ClientboundAddEntityPacket(display.getId(), display.getUUID(),
            x + 0.5, y + 0.5, z + 0.5, block.pitch(), block.yaw(),
            EntityType.ITEM_DISPLAY, 0, Vec3.ZERO, 0.0));
        List<SynchedEntityData.DataValue<?>> nonDefaultValues = display.getEntityData().getNonDefaultValues();
        assert nonDefaultValues != null;
        nmsPlayer.connection.send(new ClientboundSetEntityDataPacket(display.getId(), nonDefaultValues));
        return display.getId();
    }

    public void removeSingle(Player player, Location location) {
        Map<Long, Map<Long, Integer>> byChunk = visible.get(player.getUniqueId());
        if (byChunk == null) {
            return;
        }
        long chunkKey = ((long) (location.getBlockX() >> 4) << 32)
            | ((location.getBlockZ() >> 4) & 0xFFFFFFFFL);
        Map<Long, Integer> shown = byChunk.get(chunkKey);
        if (shown == null) {
            return;
        }
        Integer id = shown.remove(BlockPos.asLong(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        if (id == null) {
            return;
        }
        if (shown.isEmpty()) {
            byChunk.remove(chunkKey);
        }
        if (player.isOnline() && player.getWorld().equals(location.getWorld())) {
            ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(id));
        }
    }

    public void pruneOutside(Player player, int centerX, int centerZ, int radius) {
        Map<Long, Map<Long, Integer>> byChunk = visible.get(player.getUniqueId());
        if (byChunk == null || byChunk.isEmpty()) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        Iterator<Map.Entry<Long, Map<Long, Integer>>> it = byChunk.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Map<Long, Integer>> entry = it.next();
            long key = entry.getKey();
            int x = (int) (key >> 32);
            int z = (int) key;
            if (Math.abs(x - centerX) <= radius && Math.abs(z - centerZ) <= radius) {
                continue;
            }
            ids.addAll(entry.getValue().values());
            it.remove();
        }
        if (!ids.isEmpty() && player.isOnline()) {
            int[] arr = ids.stream().mapToInt(Integer::intValue).toArray();
            ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(arr));
        }
    }

    public void clear(Player player, Chunk chunk) {
        Map<Long, Map<Long, Integer>> byChunk = visible.get(player.getUniqueId());
        if (byChunk == null) {
            return;
        }
        Map<Long, Integer> shown = byChunk.remove(chunkKey(chunk));
        if (shown == null || shown.isEmpty()) {
            return;
        }
        if (!player.isOnline()) {
            return;
        }
        int[] ids = shown.values().stream().mapToInt(Integer::intValue).toArray();
        ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(ids));
    }

    public void clearAll(Player player, boolean sendPackets) {
        if (sendPackets) {
            Map<Long, Map<Long, Integer>> byChunk = visible.remove(player.getUniqueId());
            if (byChunk == null || byChunk.isEmpty()) {
                return;
            }
            int[] ids = byChunk.values().stream()
                .flatMapToInt(shown -> shown.values().stream().mapToInt(Integer::intValue))
                .toArray();
            if (ids.length > 0 && player.isOnline()) {
                ((CraftPlayer) player).getHandle().connection.send(new ClientboundRemoveEntitiesPacket(ids));
            }
        } else {
            visible.remove(player.getUniqueId());
        }
    }

    private static long chunkKey(Chunk chunk) {
        return ((long) chunk.getX() << 32) | (chunk.getZ() & 0xFFFFFFFFL);
    }
}
