package se.puggan.factory.container;

import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
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
    private NonNullList<ItemStack> content = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private ICraftingRecipe recipe;
    private int timer;
    private boolean valid;
    private boolean accept;

    public FactoryEntity() {
        super(RegistryHandler.FACTORY_ENTITY.get());
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return content;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> newContent) {
        content = newContent;
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
        }
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
        // TODO Needed?
        //tellListners(this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{19};
        }
        if (!getState(FactoryBlock.enabledProperty)) {
            return new int[]{0};
        }
        Collection<IntPair> list = new TreeSet<IntPair>();
        for (int index = 10; index < 19; index++) {
            ItemStack rStack = content.get(index - 10);
            if (rStack.isEmpty()) {
                continue;
            }
            ItemStack iStack = content.get(index);
            list.add(new IntPair(index, iStack.getCount()));
        }
        return IntPair.aArray(list);
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, @Nullable Direction direction) {
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
    public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
        return index == outputSlotIndex;
    }

    LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (!this.removed && facing != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (facing == Direction.UP)
                return handlers[0].cast();
            else if (facing == Direction.DOWN)
                return handlers[1].cast();
            else
                return handlers[2].cast();
        }
        return super.getCapability(capability, facing);
    }
}
