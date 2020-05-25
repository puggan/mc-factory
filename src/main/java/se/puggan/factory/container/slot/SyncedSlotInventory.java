package se.puggan.factory.container.slot;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

public class SyncedSlotInventory extends SlotInvetory {
    private final IInventory other;
    private final int otherIndex;

    public SyncedSlotInventory(IInventory inventory, int inventoryIndex) {
        super(inventory.getStackInSlot(inventoryIndex));
        other = inventory;
        otherIndex = inventoryIndex;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack old = super.removeStackFromSlot(index);
        other.removeStackFromSlot(otherIndex);
        return old;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        super.setInventorySlotContents(index, stack);
        other.setInventorySlotContents(otherIndex, stack);
    }

    @Override
    public void clear() {
        super.clear();
        other.removeStackFromSlot(otherIndex);
    }
}
