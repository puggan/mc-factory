package se.puggan.factory.network;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import se.puggan.factory.Factory;

public class FactoryNetwork {
    public final static String VERSION = "0.0.1";
    public final static int SET_RECIPE_USED_MESSAGE_ID = 1;
    public final static int STATE_ENABLED_MESSAGE_ID = 2;
    public final static SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Factory.MOD_ID, "factory"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals
    );
    public final static ResourceLocation EMPTY_RL = new ResourceLocation("");

    public static void init() {
        CHANNEL.registerMessage(
                SET_RECIPE_USED_MESSAGE_ID,
                SetRecipeUsedMessage.class,
                SetRecipeUsedMessage::encoder,
                SetRecipeUsedMessage::new,
                SetRecipeUsedMessage::handler
        );
        CHANNEL.registerMessage(
                STATE_ENABLED_MESSAGE_ID,
                StateEnabledMessage.class,
                StateEnabledMessage::encoder,
                StateEnabledMessage::new,
                StateEnabledMessage::handler
        );
    }
}
