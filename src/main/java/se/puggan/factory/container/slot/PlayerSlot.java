package se.puggan.factory.container.slot;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;

/**
 * A Wraper for Plauers inventory slots, so we can use instanceOf to check it its a player slot.
 */
public class PlayerSlot extends Slot {
    public PlayerSlot(IInventory inventoryIn, int index, int xPosition, int yPosition) {
        super(inventoryIn, index, xPosition, yPosition);
    }
}
