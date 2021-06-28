package se.puggan.factory.blocks;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import se.puggan.factory.Factory;
import se.puggan.factory.container.FactoryEntity;

public class FactoryBlock extends BlockWithEntity {
    public static final TranslatableText menuTitle = new TranslatableText("container.factory");
    public static final BooleanProperty loadedProperty = BooleanProperty.of("loaded");
    public static final BooleanProperty enabledProperty = Properties.ENABLED;
    public static final BooleanProperty openProperty = Properties.OPEN;

    public static World lastWorld;
    public static BlockPos lastBlockPosition;

    public FactoryBlock() {
        super(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE));
        BlockState bs = getDefaultState();
        bs = bs.with(loadedProperty, false);
        bs = bs.with(enabledProperty, false);
        bs = bs.with(openProperty, false);
        setDefaultState(bs);
    }

    @Override
    @Deprecated
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ActionResult defaultType = super.onUse(state, world, pos, player, hand, hit);

        if (defaultType != ActionResult.PASS) {
            return defaultType;
        }

        if (world.isClient) {
            lastWorld = world;
            lastBlockPosition = pos;
            return ActionResult.SUCCESS;
        }

        lastWorld = world;
        lastBlockPosition = pos;
        BlockEntity blockEntity = world.getBlockEntity(pos);
        lastWorld = null;
        lastBlockPosition = null;

        if(!(blockEntity instanceof FactoryEntity)) {
            Factory.LOGGER.error("blockEntity at " + pos + " is not an FactoryEntity");
            return ActionResult.FAIL;
        }

        FactoryEntity entity = (FactoryEntity) blockEntity;

        player.openHandledScreen(entity);

        return ActionResult.SUCCESS;
    }

    @Override
    @Deprecated
    public void onStateReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity tileentity = worldIn.getBlockEntity(pos);
            if (tileentity instanceof Inventory) {
                if (tileentity instanceof FactoryEntity) {
                    // don't drop crafting exemple
                    ((FactoryEntity) tileentity).removeStack(9);
                }
                ItemScatterer.spawn(worldIn, pos, (Inventory) tileentity);
                worldIn.updateComparators(pos, this);
            }

            super.onStateReplaced(state, worldIn, pos, newState, isMoving);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(loadedProperty, enabledProperty, openProperty);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        FactoryEntity entity = new FactoryEntity();
        if(lastBlockPosition != null) {
            entity.setLocation(lastWorld, lastBlockPosition);
        }
        return entity;
    }
}
