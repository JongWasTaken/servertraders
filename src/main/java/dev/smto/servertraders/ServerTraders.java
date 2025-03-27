package dev.smto.servertraders;

import dev.smto.simpleconfig.SimpleConfig;
import dev.smto.simpleconfig.api.ConfigAnnotations;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import dev.smto.servertraders.command.CommandManager;
import dev.smto.servertraders.trading.TraderManager;
import java.nio.file.Path;

public class ServerTraders implements ModInitializer {
    public static final String MOD_ID = "servertraders";
    public static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ServerTraders.MOD_ID);
    public static final Path CONFIG_FOLDER_PATH = Path.of(FabricLoader.getInstance().getConfigDir().toString(), ServerTraders.MOD_ID);
    public static final String VERSION = FabricLoader.getInstance().getModContainer(ServerTraders.MOD_ID).orElseThrow().getMetadata().getVersion().getFriendlyString();
    public static final SimpleConfig CONFIG_MANAGER = new SimpleConfig(ServerTraders.CONFIG_FOLDER_PATH.resolve("config.conf"), Config.class);

    @Override
    public void onInitialize() {
        ServerTraders.CONFIG_MANAGER.read();
        CommandRegistrationCallback.EVENT.register((dispatcher, reg, environment) -> CommandManager.register(dispatcher, reg));
        TraderManager.reloadFromDisk();
        ServerTraders.LOGGER.info("ServerTraders v{} started!", ServerTraders.VERSION);
        ServerWorldEvents.LOAD.register((x, y) -> TraderManager.reloadFromDisk());
    }

    public static class Config {
        @ConfigAnnotations.Holds(type = Boolean.class)
        @ConfigAnnotations.Comment(comment = "The message displayed to a player when a server-managed trader is invalid. (applying changes requires server restart)")
        public static Boolean enableShopCommand = true;

        @ConfigAnnotations.Holds(type = String.class)
        @ConfigAnnotations.Comment(comment = "The command used to open the master menu, e.g. \"shop\" for \"/shop\". (applying changes requires server restart)")
        public static String shopCommand = "shop";

        @ConfigAnnotations.Holds(type = Boolean.class)
        @ConfigAnnotations.Comment(comment = "If true, players with the vanilla permission level set below can bypass required LuckPerms(or other) permissions.")
        public static boolean enablePermissionFallback = true;

        @ConfigAnnotations.Holds(type = Integer.class)
        @ConfigAnnotations.Comment(comment = "The vanilla permission level required to bypass LuckPerms(or other) permission requirements.")
        public static int fallbackPermissionLevel = 4;

        @ConfigAnnotations.Holds(type = String.class)
        @ConfigAnnotations.Comment(comment = "The title of the master shop menu.")
        public static String shopMenuTitleText = "Server Shops";

        @ConfigAnnotations.Holds(type = String.class)
        @ConfigAnnotations.Comment(comment = "The title of the trader placing menu.")
        public static String shopTraderPlacingTitleText = "Shop Selection";

        @ConfigAnnotations.Holds(type = String.class)
        @ConfigAnnotations.Comment(comment = "The message displayed to a player when a server-managed trader is invalid.")
        public static String invalidTraderText = "This server-managed trader contains invalid data. Please contact a server moderator.";
    }
}
