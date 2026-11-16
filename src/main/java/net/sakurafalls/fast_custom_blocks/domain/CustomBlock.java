package net.sakurafalls.fast_custom_blocks.domain;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public record CustomBlock(Location location, ItemStack item, float pitch, float yaw) {

    public static CustomBlock of(Location location, ItemStack item, float pitch, float yaw) {
        return new CustomBlock(location, item, pitch, yaw);
    }

}
