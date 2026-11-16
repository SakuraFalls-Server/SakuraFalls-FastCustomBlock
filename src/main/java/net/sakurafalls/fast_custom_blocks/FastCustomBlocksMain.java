package net.sakurafalls.fast_custom_blocks;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import net.sakurafalls.fast_custom_blocks.denizen.commands.FastCustomBlockCommand;
import net.sakurafalls.fast_custom_blocks.denizen.objects.FastCustomBlockTag;
import net.sakurafalls.fast_custom_blocks.game.listener.PlayerListener;
import net.sakurafalls.fast_custom_blocks.game.nms.FakeBlockRenderer;
import net.sakurafalls.fast_custom_blocks.service.CustomBlockService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class FastCustomBlocksMain extends JavaPlugin {

    private static FastCustomBlocksMain instance;
    public static FastCustomBlocksMain getInstance() {
        return instance;
    }

    private static CustomBlockService customBlockService;
    public static CustomBlockService getCustomBlockService() {
        return customBlockService;
    }

    private static FakeBlockRenderer fakeBlockRenderer;
    public static FakeBlockRenderer getFakeBlockRenderer() {
        return fakeBlockRenderer;
    }

    @Override
    public void onEnable() {
        instance = this;

        try {
            customBlockService = new CustomBlockService();
        } catch (IOException e) {
            this.getServer().getPluginManager().disablePlugin(this);
            throw new RuntimeException(e);
        }
        fakeBlockRenderer = new FakeBlockRenderer();

        DenizenCore.commandRegistry.registerCommand(FastCustomBlockCommand.class);
        ObjectFetcher.registerWithObjectFetcher(FastCustomBlockTag.class, FastCustomBlockTag.tagProcessor)
            .generateBaseTag();

        this.getServer().getPluginManager().registerEvents(new PlayerListener(), this);
    }

    @Override
    public void onDisable() {
        customBlockService.saveAll();
    }

}
