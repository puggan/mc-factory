package se.puggan.factory.container;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.state.BooleanProperty;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.items.CapabilityItemHandler;
import se.puggan.factory.Factory;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.container.slot.IInventorySender;
import se.puggan.factory.network.FactoryNetwork;
import se.puggan.factory.util.RegistryHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

// implements ISidedInventory
public class FactoryEntity extends LockableLootTileEntity implements ITickableTileEntity, IRecipeHolder, IRecipeHelperPopulator, IInventorySender, ISidedInventory {
    public final int SIZE = 20;
    private NonNullList<ItemStack> content = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private ICraftingRecipe recipt;
    private ResourceLocation reciptRL;
    private int timer;
    private boolean valid;
    private boolean accept;

    public FactoryEntity() {
        super(RegistryHandler.FACTORY_ENTITY.get());
        recipt = null;
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
        if (!this.checkLootAndWrite(compound)) {
            ItemStackHelper.saveAllItems(compound, content);
        }
        compound.putString("recipt", reciptRL == null ? "" : reciptRL.toString());
        return compound;
    }

    public void read(CompoundNBT compound) {
        super.read(compound);
        content = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        if (!this.checkLootAndRead(compound)) {
            ItemStackHelper.loadAllItems(compound, content);
        }
        reciptRL = new ResourceLocation(compound.getString("recipt"));
        if(world != null) {
            setRecipeUsed(world, reciptRL);
        }
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
        if(world == null || world.isRemote) {
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

        if(valid) {
            valid = doCrafting();
        }
    }

    private boolean validateCrafting() {
        if(world == null || world.isRemote) {
            accept = false;
            return false;
        }
        if(recipt == null) {
            accept = false;
            return false;
        }

        BlockState blockState = world.getBlockState(pos);
        if(!blockState.get(FactoryBlock.loadedProperty)) {
            accept = false;
            return false;
        }
        if(!blockState.get(FactoryBlock.enabledProperty)) {
            accept = false;
            return false;
        }

        CraftingInventory ci = new CraftingInventory(new DummyContainer(), 3, 3);
        for(int i = 0; i < 9; i++) {
            ci.setInventorySlotContents(i, content.get(i));
        }
        if(!recipt.matches(ci, world)) {
            recipt = null;
            reciptRL = null;
            accept = false;
            return false;
        }

        accept = true;

        for(int i = 0; i < 9; i++) {
            ci.setInventorySlotContents(i, content.get(i + 10));
        }
        return recipt.matches(ci, world);
    }

    private boolean doCrafting() {
        ItemStack gStack = getStackInSlot(9);
        if(gStack.isEmpty()) {
            return false;
        }

        int made = gStack.getCount();
        ItemStack oStack = getStackInSlot(19);
        if(!oStack.isEmpty()) {
            if(oStack.getItem() != gStack.getItem()) {
                return false;
            }
            if(oStack.getMaxStackSize() < oStack.getCount() + made) {
                return false;
            }
        }

        for(int i = 0; i < 9; i++) {
            ItemStack rStack = getStackInSlot(i);
            if(rStack.isEmpty()) {
                continue;
            }
            ItemStack iStack = getStackInSlot(10 + i);
            iStack.shrink(1);
        }

        if(!oStack.isEmpty()) {
            oStack.grow(made);
        } else {
            setInventorySlotContents(19, gStack.copy());
        }
        return true;
    }

    public void setRecipeUsed(@Nonnull World world, @Nonnull ResourceLocation recipt) {
        reciptRL = recipt;
        if(recipt.getPath().length() <= 0) {
            setRecipeUsed(null);
            return;
        }

        Optional<? extends IRecipe<?>> optionalIRecipe = world.getRecipeManager().getRecipe(recipt);
        if (!optionalIRecipe.isPresent()) {
            Factory.LOGGER.error("setRecipeUsed() failed " + recipt);
            return;
        }

        setRecipeUsed(optionalIRecipe.get());
    }

    @Override
    public void setRecipeUsed(@Nullable IRecipe<?> newRecipt) {
        boolean loaded = newRecipt instanceof ICraftingRecipe;
        reciptRL = loaded ? newRecipt.getId() : null;
        recipt = loaded ? (ICraftingRecipe) newRecipt : null;
        if(world == null || world.isRemote) {
            return;
        }
        stateLoaded(loaded);
    }

    @Nullable
    @Override
    public ICraftingRecipe getRecipeUsed() {
        return recipt;
    }

    @Override
    public void fillStackedContents(@Nonnull RecipeItemHelper helper) {
        for (int i = 0; i < 9; i++) {
            helper.accountPlainStack(content.get(i));
        }
    }

    public ResourceLocation getRecipeRL() {
        return reciptRL;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        tellListners(this);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.DOWN) {
            return new int[]{19};
        }
        return new int[]{10, 11, 12, 13, 14, 15, 16, 17, 18};
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, @Nullable Direction direction) {
        if(index < 10) {
            return false;
        }
        if(index > 18) {
            return false;
        }
        if(!accept) {
            return false;
        }
        ItemStack rStack = content.get(index - 10);
        Item item = stack.getItem();
        if(rStack.isEmpty() || rStack.getItem() != item) {
            return false;
        }
        ItemStack iStack = content.get(index);
        if(iStack.isEmpty()) {
            return true;
        }
        return iStack.getItem() == item;
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, Direction direction) {
        if(index != 19) {
            return false;
        }
        return true;
    }

    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }
}
