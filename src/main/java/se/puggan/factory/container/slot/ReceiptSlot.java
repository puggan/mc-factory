package se.puggan.factory.container.slot;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;

public class ReceiptSlot extends Slot {
    public ReceiptSlot(IInventory inventoryIn, int index, int xPosition, int yPosition) {
        super(inventoryIn, index, xPosition, yPosition);
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }
}
