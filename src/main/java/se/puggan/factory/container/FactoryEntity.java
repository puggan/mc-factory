package se.puggan.factory.container;

import java.util.Collection;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IRecipeHelperPopulator;
import net.minecraft.inventory.IRecipeHolder;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.BooleanProperty;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.Factory;
import se.puggan.factory.util.IntPair;
import se.puggan.factory.util.RegistryHandler;

// implements ISidedInventory
public class FactoryEntity extends LockableLootTileEntity implements ITickableTileEntity, IRecipeHolder, IRecipeHelperPopulator, ISidedInventory {
    public static final int resultSlotIndex = 9;
    public static final int outputSlotIndex = 19;
    public final int SIZE = 20;
    private final FactoryItemHandler itemHandler;
    private NonNullList<ItemStack> content = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private ICraftingRecipe recipe;
    private int timer;
    private boolean valid;
    private boolean accept;

    public FactoryEntity() {
        super(RegistryHandler.FACTORY_ENTITY.get());
        itemHandler = new FactoryItemHandler(this);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return content;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> newContent) {
        content = newContent;
        itemHandler.dirty();
    }

    @Nonnull
    @Override
    protected ITextComponent getDefaultName() {
        return FactoryBlock.menuTitle;
    }

    @Nonnull
    @Override
    public Container createMenu(
            int windowId,
            @Nullable PlayerInventory playerInventory
    ) {
        return new FactoryContainer(windowId, playerInventory, this);
    }

    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        ItemStackHelper.saveAllItems(compound, content);
        return compound;
    }

    //public void read(CompoundNBT compound) { #MCP
    public void func_230337_a_(BlockState p_230337_1_, CompoundNBT compound) {
        //super.read(compound); #MCP
        super.func_230337_a_(p_230337_1_, compound);
        content = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(compound, content);
        itemHandler.dirty();
    }

    @Nullable
    public boolean getState(BooleanProperty bp) {
        if (world == null) {
            Factory.LOGGER.warn("Failed to load state " + bp.getName() + ", no world");
            return false;
        }
        return world.getBlockState(pos).get(bp);
    }

    public void setState(BooleanProperty bp, boolean value) {
        if (world == null) {
            return;
        }
        BlockState oldState = world.getBlockState(pos);
        BlockState newState = oldState.with(bp, value);
        world.setBlockState(pos, newState, 3);
    }

    public void stateOpen(boolean value) {
        setState(FactoryBlock.openProperty, value);
    }

    public void stateEnabled(boolean value) {
        setState(FactoryBlock.enabledProperty, value);
        itemHandler.dirty();
    }

    public void stateLoaded(boolean value) {
        setState(FactoryBlock.loadedProperty, value);
    }

    @Override
    public int getSizeInventory() {
        return SIZE;
    }

    @Override
    public void tick() {
        if (world == null || world.isRemote) {
            return;
        }
        timer++;
        // 1s = 20 ticks = 10 redstone ticks, 8 ticks matches hopper cooldown
        int timerGoal = valid ? 8 : 20;
        if (timer < timerGoal) {
            return;
        }

        timer -= timerGoal;
        valid = validateCrafting();

        if (valid) {
            valid = doCrafting();
        } else if (accept) {
            doRebalance();
        }
    }

    private void doRebalance() {
        int fromIndex = 0, toIndex = 0, goalAmount = 0;

        // Make a map of slots and count per item
        TreeMap<String, TreeSet<IntPair>> map = new TreeMap<>();
        for (int recipeSlotIndex = 0; recipeSlotIndex < 9; recipeSlotIndex++) {
            int inputSlotIndex = recipeSlotIndex + 10;
            int count = 0;
            ItemStack recipeStack = content.get(recipeSlotIndex);
            if(recipeStack.isEmpty()) {
                continue;
            }
            Item recipeItem = recipeStack.getItem();
            ItemStack inputStack = content.get(inputSlotIndex);
             if(inputStack.getItem() != recipeItem) {
                 if(!inputStack.isEmpty()) {
                     continue;
                 }
            } else {
                count = inputStack.getCount();
            }
            String recipeItemKey = recipeItem.toString();
            if(map.containsKey(recipeItemKey)) {
                map.get(recipeItemKey).add(new IntPair(inputSlotIndex, count));
            } else {
                TreeSet<IntPair> list = new TreeSet<>();
                list.add(new IntPair(inputSlotIndex, count));
                map.put(recipeItemKey, list);
            }
        }

        // Find out what item to move, and make sure we have enough items
        for(TreeSet<IntPair> list : map.values()) {
            // Only one slot for this item
            int slotCount = list.size();
            if(slotCount < 2) {
                // The only slot for this item is empty, abort
                if(list.first().b < 1) {
                    return;
                }
            }
            // if the first slot in the sorted list is non empty, all slots are none empty, skip
            if(list.first().b > 0) {
                continue;
            }
            // Count the total count
            int sum = 0;
            for(IntPair p : list) {
                sum += p.b;
            }
            // If less items then slots, abort
            if(sum < slotCount) {
                return;
            }
            if(goalAmount > 0) {
                continue;
            }
            goalAmount = sum / slotCount;
            toIndex = list.first().a;
            fromIndex = list.last().a;
        }
        if(goalAmount < 1 || fromIndex == toIndex) {
            return;
        }
        if(!content.get(toIndex).isEmpty()) {
            ItemStack fromStack = content.get(fromIndex);
            ItemStack toStack = content.get(toIndex);
            return;
        }
        ItemStack fromStack = content.get(fromIndex);
        int moveCount = fromStack.getCount() - goalAmount;
        if(moveCount < 0) {
            return;
        }
        if(moveCount > goalAmount) {
            moveCount = goalAmount;
        }

        ItemStack toStack = fromStack.split(moveCount);
        setInventorySlotContents(toIndex, toStack);
    }

    private boolean validateCrafting() {
        if (world == null || world.isRemote) {
            accept = false;
            return false;
        }

        BlockState blockState = world.getBlockState(pos);
        if (!blockState.get(FactoryBlock.loadedProperty)) {
            accept = false;
            return false;
        }
        if (!blockState.get(FactoryBlock.enabledProperty)) {
            accept = false;
            return false;
        }

        if (recipe == null) {
            recipe = calculateRecipe();
            if (recipe == null) {
                accept = false;
                return false;
            }
        }

        CraftingInventory ci = new CraftingInventory(new DummyContainer(), 3, 3);
        for (int i = 0; i < 9; i++) {
            ci.setInventorySlotContents(i, content.get(i));
        }

        if (!recipe.matches(ci, world)) {
            recipe = null;
            accept = false;
            return false;
        }

        accept = true;

        for (int i = 0; i < 9; i++) {
            ci.setInventorySlotContents(i, content.get(i + 10));
        }

        return recipe.matches(ci, world);
    }

    private boolean doCrafting() {
        ItemStack gStack = getStackInSlot(9);
        if (gStack.isEmpty()) {
            return false;
        }

        int made = gStack.getCount();
        ItemStack oStack = getStackInSlot(19);
        if (!oStack.isEmpty()) {
            if (oStack.getItem() != gStack.getItem()) {
                return false;
            }
            if (oStack.getMaxStackSize() < oStack.getCount() + made) {
                return false;
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack rStack = getStackInSlot(i);
            if (rStack.isEmpty()) {
                continue;
            }
            ItemStack iStack = getStackInSlot(10 + i);
            iStack.shrink(1);
        }

        if (!oStack.isEmpty()) {
            oStack.grow(made);
        } else {
            setInventorySlotContents(19, gStack.copy());
        }
        return true;
    }

    @Override
    public void setRecipeUsed(@Nullable IRecipe<?> newRecipe) {
        boolean loaded = newRecipe instanceof ICraftingRecipe;
        recipe = loaded ? (ICraftingRecipe) newRecipe : null;
        if (world == null || world.isRemote) {
            return;
        }
        stateLoaded(loaded);
        setInventorySlotContents(resultSlotIndex, loaded ? newRecipe.getRecipeOutput() : ItemStack.EMPTY);
    }

    @Nullable
    @Override
    public ICraftingRecipe getRecipeUsed() {
        return recipe;
    }

    @Nullable
    public ICraftingRecipe calculateRecipe() {
        if (world == null) {
            return null;
        }

        CraftingInventory ci = new CraftingInventory(new DummyContainer(), 3, 3);
        for (int i = 0; i < 9; i++) {
            ci.setInventorySlotContents(i, content.get(i));
        }
        if (recipe != null && recipe.matches(ci, world)) {
            return recipe;
        }

        if (world.isRemote) {
            return null;
        }

        Optional<ICraftingRecipe> optional = world.getRecipeManager().getRecipe(IRecipeType.CRAFTING, ci, world);
        if (!optional.isPresent()) {
            if (recipe != null) {
                setRecipeUsed(null);
            }
            return null;
        }

        ICraftingRecipe newRecipe = optional.get();
        if (newRecipe != recipe) {
            setRecipeUsed(newRecipe);
        }

        return newRecipe;
    }

    @Override
    public void fillStackedContents(@Nonnull RecipeItemHelper helper) {
        for (int i = 0; i < 9; i++) {
            helper.accountPlainStack(content.get(i));
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
        itemHandler.dirty();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        boolean bottom = side == Direction.DOWN;
        return getSortedInboxSlots(!bottom, bottom);
    }

    public int[] getSortedInboxSlots(boolean insert, boolean extract) {
        if (!getState(FactoryBlock.enabledProperty)) {
            return extract ? new int[]{outputSlotIndex} : new int[]{};
        }
        Collection<IntPair> list = new TreeSet<IntPair>();
        if(insert) {
            int offset = resultSlotIndex + 1;
            for (int index = offset; index < outputSlotIndex; index++) {
                ItemStack rStack = content.get(index - offset);
                if (rStack.isEmpty()) {
                    continue;
                }
                ItemStack iStack = content.get(index);
                list.add(new IntPair(index, iStack.getCount()));
            }
        }
        if(extract) {
            list.add(new IntPair(outputSlotIndex, 99));
        }
        return IntPair.aArray(list);
    }

    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index <= resultSlotIndex) {
            return false;
        }
        if (index >= outputSlotIndex) {
            return false;
        }
        if (!accept) {
            return false;
        }
        int offset = outputSlotIndex - resultSlotIndex;
        ItemStack rStack = content.get(index - offset);
        Item item = stack.getItem();
        if (rStack.isEmpty() || rStack.getItem() != item) {
            return false;
        }
        ItemStack iStack = content.get(index);
        if (iStack.isEmpty()) {
            return true;
        }
        return iStack.getItem() == item;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, @Nullable Direction direction) {
        return isItemValidForSlot(index, stack);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
        return index == outputSlotIndex;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (!this.removed && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return LazyOptional.of(() -> itemHandler).cast();
        }
        return super.getCapability(capability, facing);
    }
}
