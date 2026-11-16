package net.sakurafalls.fast_custom_blocks.denizen.commands;

import com.denizenscript.denizen.objects.ItemTag;
import com.denizenscript.denizen.objects.LocationTag;
import com.denizenscript.denizencore.exceptions.InvalidArgumentsRuntimeException;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.commands.AbstractCommand;
import com.denizenscript.denizencore.scripts.commands.generator.ArgDefaultNull;
import com.denizenscript.denizencore.scripts.commands.generator.ArgLinear;
import com.denizenscript.denizencore.scripts.commands.generator.ArgName;
import net.sakurafalls.fast_custom_blocks.FastCustomBlocksMain;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public class FastCustomBlockCommand extends AbstractCommand {

    public FastCustomBlockCommand() {
        setName("fastcustomblock");
        setSyntax("fastcustomblock [<location>] [<item>] [cancel]");
        setRequiredArguments(2, 2);
        isProcedural = false;
        autoCompile();
    }

    public static void autoExecute(ScriptEntry scriptEntry,
                                   @ArgName("location") @ArgLinear @ArgDefaultNull LocationTag locationTag,
                                   @ArgName("material") @ArgLinear @ArgDefaultNull ItemTag itemTag,
                                   @ArgName("cancel") boolean cancel) {
        if (locationTag == null) {
            throw new InvalidArgumentsRuntimeException("Must specify a valid location!");
        }
        Location location = locationTag.getBlockLocation().toLocation(locationTag.getWorld());
        location.setPitch(locationTag.getPitch());
        location.setYaw(locationTag.getYaw());
        if (!cancel) {
            if (itemTag == null) {
                throw new InvalidArgumentsRuntimeException("Must specify a valid material!");
            }
            ItemStack item = itemTag.getItemStack();
            FastCustomBlocksMain.getCustomBlockService().add(location, item);
        } else {
            FastCustomBlocksMain.getCustomBlockService().remove(location);
        }
    }

}
