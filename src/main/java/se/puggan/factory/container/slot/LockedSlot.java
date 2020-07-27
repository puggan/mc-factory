package se.puggan.factory.container.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * Slot is always locked, the item can not be changed by user
 */
public class LockedSlot extends Slot {
    public boolean enabled;

    public LockedSlot(Inventory inventory, int index, int xPosition, int yPosition, boolean enabled) {
        super(inventory, index, xPosition, yPosition);
        this.enabled = enabled;
    }

    @Override
    public boolean doDrawHoveringEffect() {
        return enabled;
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeItems(PlayerEntity playerIn) {
        return false;
    }
}
