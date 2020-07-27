package se.puggan.factory.container.slot;

import net.minecraft.inventory.Inventory;

/**
 * A slot that automaticly hides it selfs when empty
 */
public class HiddenSlot extends LockedSlot {
    private final int slotIndex;

    public HiddenSlot(Inventory inventory, int index, int xPosition, int yPosition) {
        super(inventory, index, xPosition, yPosition, !inventory.getStack(index).isEmpty());
        slotIndex = index;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        enabled = !inventory.getStack(slotIndex).isEmpty();
    }
}
