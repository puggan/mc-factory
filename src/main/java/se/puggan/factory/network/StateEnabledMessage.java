package se.puggan.factory.network;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkEvent;
import se.puggan.factory.Factory;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.container.FactoryEntity;

import java.util.function.Supplier;

public class StateEnabledMessage extends NetworkMessage {
    private BlockPos pos;
    private boolean enabled;

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
        if(player == null) {
            Factory.LOGGER.error("StateEnabledMessage.handle() no sender");
            return;
        }
        World world = player.world;
        if (world.isRemote) {
            Factory.LOGGER.error("StateEnabledMessage.handle() is not server");
            return;
        }

        BlockState bs = world.getBlockState(pos);
        if (!enabled) {
            world.setBlockState(pos, bs.with(FactoryBlock.enabledProperty, false));
            contextPromise.setPacketHandled(true);
            return;
        }

        INamedContainerProvider entity = bs.getContainer(world, pos);

        if (!(entity instanceof FactoryEntity)) {
            Factory.LOGGER.error("StateEnabledMessage.handle() position dosn't contain a FactoryEntity");
            return;
        }
        FactoryEntity fInvetory = (FactoryEntity) entity;

        if (fInvetory.getRecipeRL() == null) {
            Factory.LOGGER.error("StateEnabledMessage.handle() position dosn't have a recipe");
            (new SetRecipeUsedMessage(pos, null)).sendToPlayer(player);
            return;
        }

        world.setBlockState(pos, bs.with(FactoryBlock.enabledProperty, enabled));
        contextPromise.setPacketHandled(true);
        fInvetory.container.updateEnabled(false);
    }
}
