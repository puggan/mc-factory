package se.puggan.factory.container;

import java.util.Collection;
import java.util.Optional;
import java.util.TreeMap;
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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeMatcher;
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
import org.jetbrains.annotations.NotNull;
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

    @NotNull
    @Override
    protected DefaultedList<ItemStack> getInvStackList() {
        return content;
    }

    @Override
    protected void setInvStackList(@NotNull DefaultedList<ItemStack> newContent) {
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

    @NotNull
    @Override
    public NbtCompound writeNbt(NbtCompound compound) {

        super.writeNbt(compound);
        Inventories.writeNbt(compound, content);
        return compound;
    }

    @Override
    public void fromTag(BlockState state, NbtCompound compound) {
        super.fromTag(state, compound);
        content = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(compound, content);
    }

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
        setStack(toIndex, toStack);
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
    public void provideRecipeInputs(RecipeMatcher finder) {
        for (int i = 0; i < 9; i++) {
            finder.addInput(content.get(i));
        }
    }

    @Override
    public int[] getAvailableSlots(@NotNull Direction side) {
        boolean bottom = side == Direction.DOWN;
        return getSortedInboxSlots(!bottom, bottom);
    }

    public int[] getSortedInboxSlots(boolean insert, boolean extract) {
        if (!getState(FactoryBlock.enabledProperty)) {
            return extract ? new int[]{outputSlotIndex} : new int[]{};
        }
        Collection<IntPair> list = new TreeSet<>();
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

    public boolean isItemValidForSlot(int index, @NotNull ItemStack stack) {
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
    public boolean canInsert(int index, @NotNull ItemStack stack, @Nullable Direction direction) {
        return isItemValidForSlot(index, stack);
    }

    @Override
    public boolean canExtract(int index, @NotNull ItemStack stack, @NotNull Direction direction) {
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
