package se.puggan.factory.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

public class DummyContainer extends ScreenHandler {
    protected DummyContainer() {
        super(ScreenHandlerType.CRAFTING, 0);
    }

    @Override
    public boolean canUse(PlayerEntity playerIn) {
        return false;
    }

    @Override
    public void onContentChanged(Inventory inventoryIn) {
    }
}
