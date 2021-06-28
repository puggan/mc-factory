package se.puggan.factory.network;

import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.container.FactoryContainer;
import se.puggan.factory.container.FactoryEntity;
import se.puggan.factory.Factory;

public class StateEnabledMessage extends NetworkMessage {
    private BlockPos pos;
    private final boolean enabled;

    public StateEnabledMessage(BlockPos pos, boolean enabled) {
        this.pos = pos;
        this.enabled = enabled;
    }

    public StateEnabledMessage(PacketBuffer pb) {
        pos = pb.readBlockPos();
        enabled = pb.readBoolean();
    }

    @Override
    public void encode(PacketBuffer pb) {
        pb.writeBlockPos(pos);
        pb.writeBoolean(enabled);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context contextPromise = contextSupplier.get();
        ServerPlayerEntity player = contextPromise.getSender();
        if (player == null) {
            Factory.LOGGER.error("StateEnabledMessage.handle() no sender");
            return;
        }
        World world = player.world;
        if (world.isRemote) {
            Factory.LOGGER.error("StateEnabledMessage.handle() is not server");
            return;
        }

        if (!(player.openContainer instanceof FactoryContainer)) {
            Factory.LOGGER.error("StateEnabledMessage.handle() Container not opened");
            return;
        }
        FactoryContainer fc = (FactoryContainer) player.openContainer;
        FactoryEntity fi = fc.fInventory;

        if (pos.equals(BlockPos.ZERO)) {
            pos = fi.getPos();
        }

        BlockState bs = world.getBlockState(pos);
        if (!bs.hasProperty(FactoryBlock.enabledProperty)) {
            Factory.LOGGER.error("StateEnabledMessage.handle() bad BS at " + pos.toString());
        }

        world.setBlockState(pos, bs.with(FactoryBlock.enabledProperty, enabled));

        fc.updateEnabled(false);

        contextPromise.setPacketHandled(true);
    }

    public static void sendToServer(BlockPos pos, boolean enabled) {
        FactoryNetwork.CHANNEL.sendToServer(new StateEnabledMessage(pos, enabled));
    }
}
