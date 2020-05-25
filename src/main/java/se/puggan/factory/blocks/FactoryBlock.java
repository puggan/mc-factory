package se.puggan.factory.blocks;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import se.puggan.factory.Factory;
import se.puggan.factory.container.FactoryContainer;
import se.puggan.factory.container.FactoryEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class FactoryBlock extends ContainerBlock {
    public static final ITextComponent menuTitle = new TranslationTextComponent("container.factory");
    public static final BooleanProperty loadedProperty = BooleanProperty.create("loaded");
    public static final BooleanProperty enabledProperty = BlockStateProperties.ENABLED;
    public static final BooleanProperty openProperty = BlockStateProperties.OPEN;

    public static World lastWorld;
    public static BlockPos lastBlockPosition;

    public FactoryBlock() {
        super(Properties.from(Blocks.CRAFTING_TABLE));
        BlockState bs = getDefaultState();
        bs = bs.with(loadedProperty, false);
        bs = bs.with(enabledProperty, false);
        bs = bs.with(openProperty, false);
        setDefaultState(bs);
    }

    @Override
    @Nonnull
    @Deprecated
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        Factory.LOGGER.warn("onBlockActivated()");
        ActionResultType defaultType = super.onBlockActivated(state, world, pos, player, hand, hit);

        if (defaultType != ActionResultType.PASS) {
            return defaultType;
        }

        if (world.isRemote) {
            lastWorld = world;
            lastBlockPosition = pos;
            return ActionResultType.SUCCESS;
        }

        INamedContainerProvider container = state.getContainer(world, pos);

        player.openContainer(container);
        return ActionResultType.SUCCESS;
    }


    @Override
    @Deprecated
    public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity instanceof IInventory) {
                if(tileentity instanceof FactoryEntity) {
                    // don't drop crafting exemple
                    ((FactoryEntity)tileentity).removeStackFromSlot(9);
                }
                InventoryHelper.dropInventoryItems(worldIn, pos, (IInventory)tileentity);
                worldIn.updateComparatorOutputLevel(pos, this);
            }

            super.onReplaced(state, worldIn, pos, newState, isMoving);
        }
    }

    public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
        TileEntity tileentity = worldIn.getTileEntity(pos);
        if (tileentity instanceof FactoryEntity) {
            ((FactoryEntity)tileentity).tick();
        }
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(loadedProperty, enabledProperty, openProperty);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(IBlockReader worldIn) {
        return new FactoryEntity();
    }
}
