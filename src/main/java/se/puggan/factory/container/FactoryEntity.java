package se.puggan.factory.container;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IRecipeHelperPopulator;
import net.minecraft.inventory.IRecipeHolder;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.BooleanProperty;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import se.puggan.factory.Factory;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.util.RegistryHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// implements ISidedInventory
public class FactoryEntity extends LockableLootTileEntity implements ITickableTileEntity, IRecipeHolder, IRecipeHelperPopulator /*, IContainerListener*/ {
    public final int SIZE = 20;
    public boolean modeCrafting = false;
    private NonNullList<ItemStack> content = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private ICraftingRecipe recipt;

    public FactoryEntity() {
        super(RegistryHandler.FACTORY_ENTITY.get());
        recipt = null;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return content;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> newContent) {
        content = newContent;
    }

    @Nonnull
    @Override
    protected ITextComponent getDefaultName() {
        return FactoryBlock.menuTitle;
    }

    @Nonnull
    @Override
    public Container createMenu(
            int windowId,
            @Nullable PlayerInventory playerInventory
    ) {
        return new FactoryContainer(windowId, playerInventory, this);
    }

    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        if (!this.checkLootAndWrite(compound)) {
            ItemStackHelper.saveAllItems(compound, content);
        }
        return compound;
    }

    public void read(CompoundNBT compound) {
        super.read(compound);
        content = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        if (!this.checkLootAndRead(compound)) {
            ItemStackHelper.loadAllItems(compound, content);
        }
    }

    @Nullable
    public boolean getState(BooleanProperty bp) {
        if (world == null) {
            return false;
        }
        return world.getBlockState(pos).get(bp);
    }

    public void setState(BooleanProperty bp, boolean value) {
        if (world == null) {
            return;
        }
        BlockState oldState = world.getBlockState(pos);
        BlockState newState = oldState.with(bp, value);
        world.setBlockState(pos, newState, 3);
    }

    public void stateOpen(boolean value) {
        setState(FactoryBlock.openProperty, value);
    }

    public void stateEnabled(boolean value) {
        setState(FactoryBlock.enabledProperty, value);
    }

    public void stateLoaded(boolean value) {
        setState(FactoryBlock.loadedProperty, value);
    }

    @Override
    public int getSizeInventory() {
        return modeCrafting ? 9 : SIZE;
    }

    @Override
    public void tick() {

    }

    @Override
    public void setRecipeUsed(@Nullable IRecipe<?> newRecipt) {
        if (newRecipt instanceof ICraftingRecipe) {
            recipt = (ICraftingRecipe) newRecipt;
            stateLoaded(true);
        } else {
            recipt = null;
            stateLoaded(false);
        }
    }

    @Nullable
    @Override
    public ICraftingRecipe getRecipeUsed() {
        return recipt;
    }

    @Override
    public void fillStackedContents(@Nonnull RecipeItemHelper helper) {
        for (int i = 0; i < 9; i++) {
            helper.accountPlainStack(content.get(i));
        }
    }

    /*
    public void makeListner(FactoryContainer container) {
        container.addListener(this);
    }

    @Override
    public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
        Factory.LOGGER.info("sendAllContents triggered");
    }

    @Override
    public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
        Factory.LOGGER.info("sendSlotContents triggered");
    }

    @Override
    public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {
        Factory.LOGGER.info("sendWindowProperty triggered");
    }
    */
}
