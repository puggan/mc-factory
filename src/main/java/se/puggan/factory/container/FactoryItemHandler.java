package se.puggan.factory.container;

import javax.annotation.Nonnull;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public class FactoryItemHandler implements IItemHandlerModifiable {

    private final FactoryEntity entity;
    private boolean dirty = true;
    private int[] availableSlots;

    public FactoryItemHandler(FactoryEntity entity) {
        this.entity = entity;
    }

    public void dirty()
    {
        dirty = true;
    }

    public void makeCache()
    {
        availableSlots = entity.getSortedInboxSlots(true, true);
        dirty = false;
    }

    @Override
    public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
        if(!isItemValid(slot, stack)) {
            return;
        }
        int realSlot = availableSlots[slot];
        entity.setInventorySlotContents(realSlot, stack);
        entity.markDirty();
    }

    @Override
    public int getSlots() {
        if(dirty) {
            makeCache();
        }
        return availableSlots.length;
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        if(dirty) {
            makeCache();
        }
        if(slot < 0 || slot >= availableSlots.length) {
            return ItemStack.EMPTY;
        }
        return entity.getStackInSlot(availableSlots[slot]);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        if(stack.isEmpty() || !isItemValid(slot, stack)) {
            return stack;
        }
        int realSlot = availableSlots[slot];
        ItemStack stackInSlot = entity.getStackInSlot(realSlot);
        int maxStackSize = stackInSlot.getMaxStackSize();
        int oldCount = stackInSlot.getCount();
        int maxInsertedCount = maxStackSize - oldCount;
        if (maxInsertedCount < 1) {
            return stack;
        }
        int suggestedCount = stack.getCount();
        ItemStack copy = stack.copy();
        ItemStack leftOvers = ItemStack.EMPTY;
        if (maxInsertedCount <= suggestedCount) {
            leftOvers = copy.split(maxInsertedCount);
        }
        if (!simulate) {
            if(oldCount > 0) {
                copy.grow(oldCount);
            }
            entity.setInventorySlotContents(realSlot, copy);
            entity.markDirty();
        }
        return leftOvers;
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if(dirty) {
            makeCache();
        }
        if(slot < 0 || slot >= availableSlots.length || amount < 1) {
            return ItemStack.EMPTY;
        }
        int realSlot = availableSlots[slot];

        if(realSlot != FactoryEntity.outputSlotIndex) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = entity.getStackInSlot(realSlot);

        if (stackInSlot.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (simulate)
        {
            ItemStack copy = stackInSlot.copy();
            if (copy.getCount() >= amount) {
                copy.setCount(amount);
            }
            return copy;
        }
        else
        {
            int extractCount = Math.min(stackInSlot.getCount(), amount);
            ItemStack extractedStack = entity.decrStackSize(realSlot, extractCount);
            entity.markDirty();
            return extractedStack;
        }
    }

    @Override
    public int getSlotLimit(int slot) {
        if(dirty) {
            makeCache();
        }
        if(slot < 0 || slot >= availableSlots.length) {
            return 0;
        }
        return getStackInSlot(slot).getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        if(dirty) {
            makeCache();
        }
        if(slot < 0 || slot >= availableSlots.length) {
            return false;
        }
        int realSlot = availableSlots[slot];
        return entity.isItemValidForSlot(realSlot, stack);
    }
}
