package se.puggan.factory.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;

import javax.annotation.Nullable;

public class DummyContainer extends Container {
    protected DummyContainer() {
        super(ContainerType.CRAFTING, 0);
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return false;
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
    }
}
