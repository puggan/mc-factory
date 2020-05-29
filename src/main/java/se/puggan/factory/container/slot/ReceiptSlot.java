package se.puggan.factory.container.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

public class ReceiptSlot extends Slot {
    public boolean locked = false;
    public ReceiptSlot(IInventory inventoryIn, int index, int xPosition, int yPosition, boolean locked) {
        super(inventoryIn, index, xPosition, yPosition);
        this.locked = locked;
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return !locked;
    }

    @Override
    public boolean canTakeStack(PlayerEntity playerIn) {
        return !locked;
    }
}
