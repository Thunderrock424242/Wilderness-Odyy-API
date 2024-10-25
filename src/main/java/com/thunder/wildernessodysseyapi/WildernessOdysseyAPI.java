package com.thunder.wildernessodysseyapi;

import com.thunder.wildernessodysseyapi.command.*;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * The type Wilderness odyssey api.
 */
public class WildernessOdysseyAPI {

    public static final String MODID = "wildernessodysseyapi";

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {

    }

    /**
     * The constant MOD_ID.
     */
    public static final String MOD_ID = "wildernessodysseyapi";
    private static final Logger LOGGER = LogManager.getLogger();
    /**
     * The constant ENABLE_OUTLINE.
     */
    public static boolean ENABLE_OUTLINE = true; // Default is false, meant to be used in dev environment.

    // Hardcoded Server Whitelist - Only these servers can use the anti-cheat feature
    private static final Set<String> SERVER_WHITELIST = Set.of(
            "server-id-1",
            "server-id-2",
            "server-id-3"
    );

    /**
     * The constant antiCheatEnabled.
     */
// Configuration flags
    public static boolean antiCheatEnabled;
    /**
     * The constant globalLoggingEnabled.
     */
    public static boolean globalLoggingEnabled;

    // Scheduled Executor for periodic checks
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Instantiates a new Wilderness odyssey api.
     *
     * @param modBus the mod event bus
     */
    public WildernessOdysseyAPI(@NotNull IEventBus modBus) {
        modBus.register(this);

        // Register mod lifecycle events
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::onLoadComplete);

        // Register server events
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(ModWhitelistChecker.class);

        // Register configuration (wip)

        // Load config settings
        loadConfig();

        // If terms are not agreed to, terminate server startup
        if (!ConfigGenerator.AGREE_TO_TERMS.get()) {
            LOGGER.fatal("You must agree to the terms outlined in the README.md file by setting 'agreeToTerms' to true in the configuration file.");
            throw new RuntimeException("Server cannot start without agreement to the mod's terms and conditions.");
        }

        // Enable anti-cheat only if the server is whitelisted
        String currentServerId = "server-unique-id";  // Replace with logic to fetch the current server's unique ID
        antiCheatEnabled = SERVER_WHITELIST.contains(currentServerId);

        // Generate README file during initialization
        READMEGenerator.generateReadme();
        // Start the periodic sync with GitHub to update banned players
        startBanSyncTask();

        LOGGER.info("Wilderness Oddessy Anti-Cheat Mod Initialized. Anti-cheat enabled: {}", antiCheatEnabled);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Client setup complete");
    }

    private void onLoadComplete(final FMLLoadCompleteEvent event) {
        LOGGER.info("Load complete");
    }

    /**
     * On server starting event.
     *
     * @param event the event
     */
    public void onServerStartingEvent( ServerStartingEvent event) {
        BanCommand.register(event.getServer().getCommands().getDispatcher());
        ClearItemsCommand.register(event.getServer().getCommands().getDispatcher());
        AdminCommand.register(event.getServer().getCommands().getDispatcher());
        HealCommand.register(event.getServer().getCommands().getDispatcher());
        DimensionTPCommand.register(event.getServer().getCommands().getDispatcher());
        LOGGER.info("Ban command registered");

        LOGGER.info("Server starting setup complete. Anti-cheat enabled: {}", antiCheatEnabled);
    }

    private void loadConfig() {
        // Load settings from configuration
        globalLoggingEnabled = ConfigGenerator.GLOBAL_LOGGING_ENABLED.get();
    }

    private void startBanSyncTask() {
        // Schedule periodic sync with GitHub to update the ban list every 10 minutes
        scheduler.scheduleAtFixedRate(() -> {
            try {
                BanManager.syncBanListFromGitHub();
                LOGGER.info("Ban list synced with GitHub");
            } catch (Exception e) {
                LOGGER.error("Failed to sync ban list with GitHub", e);
            }
        }, 0, 10, TimeUnit.MINUTES);
    }

    /**
     * Is global logging enabled boolean.
     *
     * @return the boolean
     */
    public static boolean isGlobalLoggingEnabled() {
        return globalLoggingEnabled;
    }
}
