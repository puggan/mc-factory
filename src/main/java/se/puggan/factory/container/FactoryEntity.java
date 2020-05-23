package se.puggan.factory.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.util.RegistryHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// implements ISidedInventory, IRecipeHolder, IRecipeHelperPopulator
public class FactoryEntity extends LockableLootTileEntity implements ITickableTileEntity {
    NonNullList<ItemStack> content = NonNullList.withSize(9, ItemStack.EMPTY);

    public FactoryEntity() {
        super(RegistryHandler.FACTORY_ENTITY.get());
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

    @Override
    public int getSizeInventory() {
        return 9;
    }

    @Override
    public void tick() {

    }
}
