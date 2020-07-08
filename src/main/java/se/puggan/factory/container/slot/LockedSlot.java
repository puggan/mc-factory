package se.puggan.factory.container.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Slot;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * Slot is always locked, the item can not be changed by user
 */
public class LockedSlot extends Slot {
    public boolean enabled;

    public LockedSlot(IInventory inventory, int index, int xPosition, int yPosition, boolean enabled) {
        super(inventory, index, xPosition, yPosition);
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeStack(PlayerEntity playerIn) {
        return false;
    }
}
