package se.puggan.factory;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.puggan.factory.util.RegistryHandler;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("factory")
public class Factory {
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();
    public static String MOD_ID = "factory";

    public Factory() {
        // Register the setup method for modloading
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setupClient);
        modEventBus.addListener(this::setupServer);

        RegistryHandler.init();
    }

    private void setupClient(final FMLClientSetupEvent event) {
        RegistryHandler.initClientOnly();
    }

    private void setupServer(final FMLDedicatedServerSetupEvent event) {
        RegistryHandler.initServerOnly();
    }
}
