package se.puggan.factory.network;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import se.puggan.factory.Factory;

public class FactoryNetwork {
    public final static String VERSION = "0.0.2";
    public final static int STATE_ENABLED_MESSAGE_ID = 1;
    public final static SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Factory.MOD_ID, "factory"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals
    );

    public static void init() {
        CHANNEL.registerMessage(
                STATE_ENABLED_MESSAGE_ID,
                StateEnabledMessage.class,
                StateEnabledMessage::encoder,
                StateEnabledMessage::new,
                StateEnabledMessage::handler
        );
    }
}
