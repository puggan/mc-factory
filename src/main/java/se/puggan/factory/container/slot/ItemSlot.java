package se.puggan.factory.container.slot;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

/**
 * A slot for a specific item, No other items can be put here
 */
public class ItemSlot extends Slot {
    public Item lockedItem = null;
    public boolean enabled;

    public ItemSlot(IInventory inventoryIn, int index, int xPosition, int yPosition, boolean enabled) {
        super(inventoryIn, index, xPosition, yPosition);
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return lockedItem != null && stack.getItem() == lockedItem;
    }

    public void lockItem(Slot slot) {
        lockItem(slot.getStack().getItem());
    }
    public void lockItem(ItemStack stack) {
        lockItem(stack.getItem());
    }
    public void lockItem(@Nullable Item item) {
        lockedItem = item;
        enabled = item != null;
    }
}
