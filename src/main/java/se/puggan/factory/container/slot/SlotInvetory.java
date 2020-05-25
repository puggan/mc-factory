package se.puggan.factory.container.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;

public class SlotInvetory implements IInventory {
    ItemStack content;
    public boolean dirty;

    public SlotInvetory(ItemStack content) {
        this.content = content;
    }

    @Override
    public int getSizeInventory() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return content.isEmpty();
    }

    private void indexCheck(int index) {
        if (index != 0) {
            throw new IndexOutOfBoundsException("None zero index");
        }
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        indexCheck(index);
        return content;
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        indexCheck(index);
        return content.split(count);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        indexCheck(index);
        ItemStack old = content;
        content = ItemStack.EMPTY;
        return old;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        indexCheck(index);
        content = stack;
    }

    @Override
    public void markDirty() {
        dirty = true;
    }

    @Override
    public boolean isUsableByPlayer(PlayerEntity player) {
        return false;
    }

    @Override
    public void clear() {
        content = ItemStack.EMPTY;
    }
}
