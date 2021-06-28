package se.puggan.factory.network;

import java.util.function.Supplier;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraft.network.PacketBuffer;

abstract public class NetworkMessage {
    // BiConsumer<MSG, PacketBuffer>
    abstract public void encode(PacketBuffer pb);
    public static <MSG extends NetworkMessage> void encoder(MSG message, PacketBuffer pb) {
        message.encode(pb);
    }

    // BiConsumer<MSG, Supplier<NetworkEvent.Context>>
    abstract public void handle(Supplier<NetworkEvent.Context> context);
    public static <MSG extends NetworkMessage> void handler(MSG message, Supplier<NetworkEvent.Context> context) {
        message.handle(context);
    }
}
