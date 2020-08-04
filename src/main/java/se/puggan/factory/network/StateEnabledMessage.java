package se.puggan.factory.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketContext;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import se.puggan.factory.Factory;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.container.FactoryContainer;
import se.puggan.factory.container.FactoryEntity;

public class StateEnabledMessage {
    private BlockPos pos;
    private final boolean enabled;

    public StateEnabledMessage(BlockPos pos, boolean enabled) {
        this.pos = pos;
        this.enabled = enabled;
    }

    public StateEnabledMessage(PacketByteBuf pb) {
        pos = pb.readBlockPos();
        enabled = pb.readBoolean();
    }

    public PacketByteBuf encode() {
        PacketByteBuf pb = new PacketByteBuf(Unpooled.buffer());
        encode(pb);
        return pb;
    }

    public void encode(PacketByteBuf pb) {
        pb.writeBlockPos(pos);
        pb.writeBoolean(enabled);
    }

    public void handle(PacketContext context) {
        PlayerEntity player = context.getPlayer();

        if (player == null) {
            Factory.LOGGER.error("StateEnabledMessage.handle() no sender");
            return;
        }

        World world = player.world;
        if (world.isClient) {
            Factory.LOGGER.error("StateEnabledMessage.handle() is not server");
            return;
        }

        if (!(player.currentScreenHandler instanceof FactoryContainer)) {
            Factory.LOGGER.error("StateEnabledMessage.handle() Container not opened");
            return;
        }

        FactoryContainer fc = (FactoryContainer) player.currentScreenHandler;
        FactoryEntity fi = fc.fInventory;

        if (pos.equals(BlockPos.ZERO)) {
            pos = fi.getPos();
        }

        BlockState bs = world.getBlockState(pos);
        if (!bs.contains(FactoryBlock.enabledProperty)) {
            Factory.LOGGER.error("StateEnabledMessage.handle() bad BS at " + pos.toString());
        }

        world.setBlockState(pos, bs.with(FactoryBlock.enabledProperty, enabled));

        fc.updateEnabled(false);
    }

    public static void sendToServer(BlockPos pos, boolean enabled) {
        StateEnabledMessage message = new StateEnabledMessage(pos, enabled);
        ClientSidePacketRegistry.INSTANCE.sendToServer(Factory.factory_id, message.encode());
    }

    public static void receiveFromClient(PacketContext packetContext, PacketByteBuf attachedData) {
        StateEnabledMessage message = new StateEnabledMessage(attachedData);
        message.handle(packetContext);
    }
}
