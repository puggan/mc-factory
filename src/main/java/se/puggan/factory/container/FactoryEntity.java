package se.puggan.factory.container;

import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.RecipeUnlocker;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Tickable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import se.puggan.factory.Factory;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.util.IntPair;

// implements ISidedInventory
public class FactoryEntity extends LootableContainerBlockEntity implements RecipeUnlocker, RecipeInputProvider, SidedInventory, ExtendedScreenHandlerFactory, Tickable {
    public static final int resultSlotIndex = 9;
    public static final int outputSlotIndex = 19;
    public final int SIZE = 20;
    private DefaultedList<ItemStack> content = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
    private CraftingRecipe recipe;
    private int timer;
    private boolean valid;
    private boolean accept;

    public FactoryEntity() {
        super(Factory.blockEntityType);
    }

    @Override
    protected DefaultedList<ItemStack> getInvStackList() {
        return content;
    }

    @Override
    protected void setInvStackList(DefaultedList<ItemStack> newContent) {
        content = newContent;
    }

    @Override
    protected TranslatableText getContainerName() {
        return FactoryBlock.menuTitle;
    }

    @Override
    public ScreenHandler createScreenHandler(
            int windowId,
            @Nullable PlayerInventory playerInventory
    ) {
        return new FactoryContainer(windowId, playerInventory, this);
    }

    public CompoundTag toTag(CompoundTag compound) {
        super.toTag(compound);
        Inventories.toTag(compound, content);
        return compound;
    }

    public void fromTag(BlockState state, CompoundTag compound) {
        super.fromTag(state, compound);
        content = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.fromTag(compound, content);
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
    public int size() {
        return SIZE;
    }

    @Override
    public void tick() {
        if (world == null || world.isClient) {
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
        if (world == null || world.isClient) {
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
            ci.setStack(i, content.get(i));
        }

        if (!recipe.matches(ci, world)) {
            recipe = null;
            accept = false;
            return false;
        }

        accept = true;

        for (int i = 0; i < 9; i++) {
            ci.setStack(i, content.get(i + 10));
        }

        return recipe.matches(ci, world);
    }

    private boolean doCrafting() {
        ItemStack gStack = getStack(9);
        if (gStack.isEmpty()) {
            return false;
        }

        int made = gStack.getCount();
        ItemStack oStack = getStack(19);
        if (!oStack.isEmpty()) {
            if (oStack.getItem() != gStack.getItem()) {
                return false;
            }
            if (oStack.getMaxCount() < oStack.getCount() + made) {
                return false;
            }
        }

        for (int i = 0; i < 9; i++) {
            ItemStack rStack = getStack(i);
            if (rStack.isEmpty()) {
                continue;
            }
            ItemStack iStack = getStack(10 + i);
            iStack.decrement(1);
        }

        if (!oStack.isEmpty()) {
            oStack.increment(made);
        } else {
            setStack(19, gStack.copy());
        }
        return true;
    }

    @Override
    public void setLastRecipe(@Nullable Recipe<?> newRecipe) {
        boolean loaded = newRecipe instanceof CraftingRecipe;
        recipe = loaded ? (CraftingRecipe) newRecipe : null;
        if (world == null || world.isClient) {
            return;
        }
        stateLoaded(loaded);
        setStack(resultSlotIndex, loaded ? newRecipe.getOutput() : ItemStack.EMPTY);
    }

    @Nullable
    @Override
    public CraftingRecipe getLastRecipe() {
        return recipe;
    }

    @Nullable
    public CraftingRecipe calculateRecipe() {
        if (world == null) {
            return null;
        }

        CraftingInventory ci = new CraftingInventory(new DummyContainer(), 3, 3);
        for (int i = 0; i < 9; i++) {
            ci.setStack(i, content.get(i));
        }
        if (recipe != null && recipe.matches(ci, world)) {
            return recipe;
        }

        if (world.isClient) {
            return null;
        }

        Optional<CraftingRecipe> optional = world.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, ci, world);
        if (!optional.isPresent()) {
            if (recipe != null) {
                setLastRecipe(null);
            }
            return null;
        }

        CraftingRecipe newRecipe = optional.get();
        if (newRecipe != recipe) {
            setLastRecipe(newRecipe);
        }

        return newRecipe;
    }

    @Override
    public void provideRecipeInputs(RecipeFinder finder) {
        for (int i = 0; i < 9; i++) {
            finder.addNormalItem(content.get(i));
        }
    }

    @Override
    public void markDirty() {
        super.markDirty();
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
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
    public boolean canInsert(int index, ItemStack stack, @Nullable Direction direction) {
        return isItemValidForSlot(index, stack);
    }

    @Override
    public boolean canExtract(int index, ItemStack stack, Direction direction) {
        return index == outputSlotIndex;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
        if (pos == null || pos.compareTo(BlockPos.ZERO) == 0) {
            world = serverPlayerEntity.world;
            pos = FactoryBlock.lastBlockPosition;
        }
        packetByteBuf.writeBlockPos(pos);
    }
}
