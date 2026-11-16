package net.sakurafalls.fast_custom_blocks.game.listener;

import it.unimi.dsi.fastutil.ints.IntIntImmutablePair;
import net.sakurafalls.fast_custom_blocks.FastCustomBlocksMain;
import net.sakurafalls.fast_custom_blocks.domain.CustomBlock;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerListener implements Listener {

    private static final int RADIUS = 3; // 7x7 chunks
    private static final long RATE_LIMIT_MILLIS = 250L; // 5 ticks

    private final Map<UUID, Long> lastRender = new ConcurrentHashMap<>();
    private final Map<UUID, IntIntImmutablePair> lastRendered = new ConcurrentHashMap<>();

    @EventHandler
    public void onPlayerJoins(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Chunk chunk = player.getLocation().getChunk();
        renderAround(player, chunk);
        lastRender.put(player.getUniqueId(), System.currentTimeMillis());
        lastRendered.put(player.getUniqueId(), IntIntImmutablePair.of(chunk.getX(), chunk.getZ()));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (!from.getWorld().equals(to.getWorld())) {
            FastCustomBlocksMain.getFakeBlockRenderer().clearAll(player, true);
            renderAround(player, to.getChunk());
            lastRender.put(uuid, System.currentTimeMillis());
            lastRendered.put(uuid, IntIntImmutablePair.of(to.getChunk().getX(), to.getChunk().getZ()));
            return;
        }
        Long previous = lastRender.get(uuid);
        long now = System.currentTimeMillis();
        if (!(event instanceof PlayerTeleportEvent)
            && previous != null && now - previous < RATE_LIMIT_MILLIS) {
            return;
        }
        Chunk target = to.getChunk();
        IntIntImmutablePair last = lastRendered.get(uuid);
        if (last != null && last.leftInt() == target.getX() && last.rightInt() == target.getZ()) {
            return;
        }
        lastRender.put(uuid, now);
        renderAround(player, target);
        lastRendered.put(uuid, IntIntImmutablePair.of(target.getX(), target.getZ()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        FastCustomBlocksMain.getFakeBlockRenderer().clearAll(event.getPlayer(), false);
        lastRender.remove(uuid);
        lastRendered.remove(uuid);
    }

    private void renderAround(Player player, Chunk center) {
        World world = center.getWorld();
        showAround(player, world, center.getX(), center.getZ());
        FastCustomBlocksMain.getFakeBlockRenderer().pruneOutside(player, center.getX(), center.getZ(), RADIUS);
    }

    private void showAround(Player player, World world, int centerX, int centerZ) {
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                showAt(player, world, centerX + dx, centerZ + dz);
            }
        }
    }

    private void showAt(Player player, World world, int x, int z) {
        Chunk chunk = world.getChunkAt(x, z);
        Collection<CustomBlock> blocks = FastCustomBlocksMain.getCustomBlockService().getBlocksAt(chunk);
        FastCustomBlocksMain.getFakeBlockRenderer().show(player, chunk, blocks);
    }
}
