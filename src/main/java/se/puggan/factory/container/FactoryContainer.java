package se.puggan.factory.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;

import javax.annotation.Nonnull;

public class FactoryContainer extends Container {
    public FactoryContainer(int windowId, PlayerInventory playerInventory, IInventory inventory) {
        super(ContainerType.GENERIC_3X3, windowId);

        // Factory Inventory
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                this.addSlot(new Slot(inventory, 3 * y + x, x, y));
            }
        }

        // Player Inventory
        for(int y = 0; y < 3; ++y) {
            for(int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInventory, 9 + 9 * y + x, 8 + 18 * x, 85 + 18 * y));
            }
        }

        // Player Hotbar
        for(int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 143));
        }
    }

    @Override
    public boolean canInteractWith(@Nonnull PlayerEntity player) {
        return true;
    }
}
