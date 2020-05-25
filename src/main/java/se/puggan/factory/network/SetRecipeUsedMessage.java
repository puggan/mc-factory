package se.puggan.factory.network;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;
import se.puggan.factory.Factory;
import se.puggan.factory.container.FactoryEntity;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class SetRecipeUsedMessage extends NetworkMessage {
    private final BlockPos pos;
    private final ResourceLocation recipe;

    public SetRecipeUsedMessage(BlockPos pos, @Nullable IRecipe<?> recipe) {
        this.pos = pos;
        this.recipe = recipe == null ? FactoryNetwork.EMPTY_RL : recipe.getId();
    }

    public SetRecipeUsedMessage(PacketBuffer pb) {
        pos = pb.readBlockPos();
        recipe = pb.readResourceLocation();
    }

    @Override
    public void encode(PacketBuffer pb) {
        pb.writeBlockPos(pos);
        pb.writeResourceLocation(recipe);
    }

    @Override
    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        Factory.LOGGER.info("SetRecipeUsedMessage.handle()");
        NetworkEvent.Context contextPromise = contextSupplier.get();
        if (!FMLEnvironment.dist.isClient()) {
            Factory.LOGGER.error("SetRecipeUsedMessage.handle() is not client");
            return;
        }

        Factory.LOGGER.info("SetRecipeUsedMessage.handle() is client");
        contextPromise.enqueueWork(
            () -> {
                Factory.LOGGER.info("SetRecipeUsedMessage.handle() enqueueWork");
                PlayerEntity player = Minecraft.getInstance().player;
                if(player == null) {
                    Factory.LOGGER.error("SetRecipeUsedMessage.handle() no player");
                    return;
                }
                BlockState bs = player.world.getBlockState(pos);
                INamedContainerProvider genericEntity = bs.getContainer(player.world, pos);

                if (!(genericEntity instanceof FactoryEntity)) {
                    Factory.LOGGER.error("SetRecipeUsedMessage.handle() entity no FactoryEntity");
                    return;
                }

                FactoryEntity factoryEntity = (FactoryEntity) genericEntity;
                Factory.LOGGER.info("SetRecipeUsedMessage.handle() setRecipeUsed()");
                factoryEntity.setRecipeUsed(player.world, recipe);
            }
        );
        contextPromise.setPacketHandled(true);
    }

    public void sendToPlayer(ServerPlayerEntity player) {
        FactoryNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), this);
    }
}
