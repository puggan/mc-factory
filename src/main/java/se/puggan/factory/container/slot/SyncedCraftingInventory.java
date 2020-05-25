package se.puggan.factory.container.slot;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;

public class SyncedCraftingInventory extends CraftingInventory {
    IInventory other;
    int otherIndex;

    public SyncedCraftingInventory(Container eventHandlerIn, int width, int height, IInventory inventory, int inventoryIndex) {
        super(eventHandlerIn, width, height);
        other = inventory;
        otherIndex = inventoryIndex;
        int max = this.getSizeInventory();
        for(int index = 0; index < max; index++) {
            super.setInventorySlotContents(index, other.getStackInSlot(index + otherIndex));
        }
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack old = super.removeStackFromSlot(index);
        other.removeStackFromSlot(otherIndex + index);
        return old;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        super.setInventorySlotContents(index, stack);
        other.setInventorySlotContents(otherIndex + index, stack);
    }

    @Override
    public void clear() {
        super.clear();
        int max = otherIndex + this.getSizeInventory();
        for(int index = otherIndex; index < max; index++) {
            other.removeStackFromSlot(index);
        }
    }
}
