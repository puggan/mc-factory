package se.puggan.factory.container.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.item.ItemStack;

/**
 * A lockable ItemSlot with max stack-size of 1
 */
public class ReceiptSlot extends Slot {
    public boolean locked;

    public ReceiptSlot(Inventory inventoryIn, int index, int xPosition, int yPosition, boolean locked) {
        super(inventoryIn, index, xPosition, yPosition);
        this.locked = locked;
    }

    @Override
    public int getMaxItemCount() {
        return 1;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return !locked;
    }

    @Override
    public boolean canTakeItems(PlayerEntity playerIn) {
        return !locked;
    }

    @Override
    public boolean isEnabled() {
        return !locked || hasStack();
    }

}
