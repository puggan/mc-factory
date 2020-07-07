package se.puggan.factory.container.slot;

import net.minecraft.inventory.IInventory;

/**
 * A slot that automaticly hides it selfs when empty
 */
public class HiddenSlot extends LockedSlot {
    private final int slotIndex;
    public HiddenSlot(IInventory inventory, int index, int xPosition, int yPosition) {
        super(inventory, index, xPosition, yPosition, !inventory.getStackInSlot(index).isEmpty());
        slotIndex = index;
    }

    @Override
    public void onSlotChanged() {
        super.onSlotChanged();
        enabled = !inventory.getStackInSlot(slotIndex).isEmpty();
    }
}
