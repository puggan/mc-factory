package se.puggan.factory.container.slot;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.inventory.container.Slot;

public class LockedSlot extends Slot {
    public static final ResourceLocation GUI = new ResourceLocation("factory", "textures/gui/slot_off_256.png");
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
