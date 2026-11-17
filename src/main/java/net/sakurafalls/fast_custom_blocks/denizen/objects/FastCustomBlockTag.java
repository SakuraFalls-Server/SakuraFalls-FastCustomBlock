package net.sakurafalls.fast_custom_blocks.denizen.objects;

import com.denizenscript.denizen.objects.ChunkTag;
import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizen.objects.MaterialTag;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import net.sakurafalls.fast_custom_blocks.FastCustomBlocksMain;
import net.sakurafalls.fast_custom_blocks.domain.CustomBlock;
import org.bukkit.Location;

public class FastCustomBlockTag implements ObjectTag {

    private final CustomBlock block;

    private String prefix = "FastCustomBlock";

    public FastCustomBlockTag(CustomBlock block) {
        this.block = block;
    }

    public CustomBlock getBlock() {
        return block;
    }

    @Fetchable("fastcustomblock")
    public static FastCustomBlockTag valueOf(String string, TagContext context) {
        if (string == null || string.isEmpty()) {
            return null;
        }
        if (string.startsWith("fastcustomblock@")) {
            string = string.substring("fastcustomblock@".length());
        }
        LocationTag location;
        try {
            location = LocationTag.valueOf(string, context);
        } catch (Exception e) {
            return null;
        }
        if (location == null || location.getWorld() == null) {
            return null;
        }
        if (FastCustomBlocksMain.getCustomBlockService() == null) {
            return null;
        }
        CustomBlock found = FastCustomBlocksMain.getCustomBlockService().getBlockAt(location);
        if (found == null) {
            return null;
        }
        return new FastCustomBlockTag(found);
    }

    public static boolean matches(String arg) {
        if (arg == null) {
            return false;
        }
        String inner = arg.startsWith("fastcustomblock@")
            ? arg.substring("fastcustomblock@".length())
            : arg;
        if (CoreUtilities.toLowerCase(arg).startsWith("fastcustomblock@")) {
            return true;
        }
        return LocationTag.matches(inner);
    }

    public static void register() {
        tagProcessor.registerTag(MaterialTag.class, "material",
            (_, object) -> new MaterialTag(object.block.item().getType()));

        tagProcessor.registerTag(ItemTag.class, "item",
            (_, object) -> new ItemTag(object.block.item()));

        tagProcessor.registerTag(LocationTag.class, "location", (_, object) -> {
            LocationTag at = new LocationTag(object.block.location());
            at.setPitch(object.block.pitch());
            at.setYaw(object.block.yaw());
            return at;
        });

        ChunkTag.tagProcessor.registerTag(ListTag.class, "fast_custom_blocks", (_, object) -> {
            ListTag list = new ListTag();
            for (CustomBlock customBlock : FastCustomBlocksMain.getCustomBlockService().getBlocksAt(object.getChunk())) {
                list.addObject(new FastCustomBlockTag(customBlock));
            }
            return list;
        });
    }

    public static ObjectTagProcessor<FastCustomBlockTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public FastCustomBlockTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String identify() {
        Location at = block.location();
        String world = at.getWorld() != null ? at.getWorld().getName() : "null";
        return "fastcustomblock@" + at.getBlockX() + "," + at.getBlockY() + "," + at.getBlockZ() + "," + world;
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public Object getJavaObject() {
        return block;
    }
}
