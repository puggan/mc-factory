package se.puggan.factory.container;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IRecipeHelperPopulator;
import net.minecraft.inventory.IRecipeHolder;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Container;
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
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;
import se.puggan.factory.Factory;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.network.FactoryNetwork;
import se.puggan.factory.util.RegistryHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

// implements ISidedInventory
public class FactoryEntity extends LockableLootTileEntity implements ITickableTileEntity, IRecipeHolder, IRecipeHelperPopulator /*, IContainerListener*/ {
    public final int SIZE = 20;
    public boolean modeCrafting = false;
    private NonNullList<ItemStack> content = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private ICraftingRecipe recipt;
    private ResourceLocation reciptRL;

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
        compound.put("recipt", StringNBT.valueOf(reciptRL == null ? "" : reciptRL.toString()));
        Factory.LOGGER.warn("Saved reciept: " + (reciptRL == null ? "" : reciptRL.toString()));
        return compound;
    }

    public void read(CompoundNBT compound) {
        super.read(compound);
        content = NonNullList.withSize(this.getSizeInventory(), ItemStack.EMPTY);
        if (!this.checkLootAndRead(compound)) {
            ItemStackHelper.loadAllItems(compound, content);
        }
        INBT reciptNBT = compound.get("recipt");
        if(reciptNBT instanceof StringNBT) {
            reciptRL = new ResourceLocation(reciptNBT.getString());
            Factory.LOGGER.warn("Loaded reciept: " + reciptRL);
            if(world != null) {
                setRecipeUsed(world, new ResourceLocation(reciptNBT.getString()));
            }
        } else {
            Factory.LOGGER.warn("Loaded reciept: null");
            reciptRL = null;
            recipt = null;
        }
    }

    @Nullable
    public boolean getState(BooleanProperty bp) {
        if (world == null) {
            Factory.LOGGER.warn("Failed to load state " + bp.getName() + ", no world");
            return false;
        }
        Factory.LOGGER.warn("Loading state " + bp.getName());
        return world.getBlockState(pos).get(bp);
    }

    public void setState(BooleanProperty bp, boolean value) {
        if (world == null) {
            Factory.LOGGER.warn("setState(" + bp.getName() + ", " + (value ? "true" : "false") + ") -> no world");
            return;
        }
        if(world.isRemote) {
            Factory.LOGGER.warn("setState(" + bp.getName() + ", " + (value ? "true" : "false") + ") -> remote");
        }
        Factory.LOGGER.warn("setState(" + bp.getName() + ", " + (value ? "true" : "false") + ") -> update");
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
        return modeCrafting ? 9 : SIZE;
    }

    @Override
    public void tick() {

    }

    public void setRecipeUsed(@Nonnull World world, @Nonnull ResourceLocation recipt) {
        reciptRL = recipt;
        if(recipt.getPath().length() <= 0) {
            Factory.LOGGER.info("setRecipeUsed() empty");
            setRecipeUsed(null);
            return;
        }

        Optional<? extends IRecipe<?>> optionalIRecipe = world.getRecipeManager().getRecipe(recipt);
        if (!optionalIRecipe.isPresent()) {
            Factory.LOGGER.error("setRecipeUsed() failed " + recipt);
            return;
        }

        Factory.LOGGER.info("setRecipeUsed() found " + recipt);
        setRecipeUsed(optionalIRecipe.get());
    }

    @Override
    public void setRecipeUsed(@Nullable IRecipe<?> newRecipt) {
        boolean loaded = newRecipt instanceof ICraftingRecipe;
        reciptRL = loaded ? newRecipt.getId() : null;
        Factory.LOGGER.info("setRecipeUsed() " + newRecipt + " is " + (loaded ? "ICraftingRecipe" : "NOT ICraftingRecipe") + " @ " + this.toString());
        recipt = loaded ? (ICraftingRecipe) newRecipt : null;
        if(world == null || world.isRemote) {
            return;
        }
        stateLoaded(loaded);
    }

    @Nullable
    @Override
    public ICraftingRecipe getRecipeUsed() {
        Factory.LOGGER.info("getRecipeUsed() -> " + (recipt == null ? "null" : recipt.getId()) + " @ " + this.toString());
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

    /*
    public void makeListner(FactoryContainer container) {
        container.addListener(this);
    }

    @Override
    public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
        Factory.LOGGER.info("sendAllContents triggered");
    }

    @Override
    public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
        Factory.LOGGER.info("sendSlotContents triggered");
    }

    @Override
    public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {
        Factory.LOGGER.info("sendWindowProperty triggered");
    }
    */
}
