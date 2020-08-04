package se.puggan.factory.container;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.screen.AbstractRecipeScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import se.puggan.factory.Factory;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.container.slot.HiddenSlot;
import se.puggan.factory.container.slot.ItemSlot;
import se.puggan.factory.container.slot.LockedSlot;
import se.puggan.factory.container.slot.PlayerSlot;
import se.puggan.factory.container.slot.ReceiptSlot;
import se.puggan.factory.network.StateEnabledMessage;

public class FactoryContainer extends AbstractRecipeScreenHandler<CraftingInventory> {
    private List<FactoryScreen> screens;
    private boolean enabled;
    private final PlayerInventory pInventory;
    public FactoryEntity fInventory;
    private boolean loading;
    private final boolean clientSide;

    public FactoryContainer(int windowId, PlayerInventory playerInventory, Inventory inventory) {
        super(Factory.containerType, windowId);
        screens = new ArrayList<>();
        loading = true;
        pInventory = playerInventory;
        clientSide = !(pInventory.player instanceof ServerPlayerEntity);
        if (!(inventory instanceof FactoryEntity)) {
            throw new RuntimeException("Bad inventory type, expected FactoryEntity");
        }
        fInventory = (FactoryEntity) inventory;
        // TODO why is fInventory.getPos() BlockPos.ZERO in the client
        if (playerInventory.player.world.isClient) {
            fInventory.setLocation(FactoryBlock.lastWorld, FactoryBlock.lastBlockPosition);
            FactoryBlock.lastWorld = null;
            FactoryBlock.lastBlockPosition = null;
        }
        enabled = fInventory.getState(FactoryBlock.enabledProperty);
        fInventory.onOpen(playerInventory.player);
        slotInventory();
        loading = false;
        if (enabled) {
            activate(false);
        } else {
            deactivate(false);
        }
        lockInput();
        onContentChanged(fInventory);
    }

    public FactoryContainer(int windowId, PlayerInventory playerInventory, PacketByteBuf extraData) {
        this(windowId, playerInventory, new FactoryEntity());
    }

    private void slotInventory() {
        // Receipt Inventory, 0-8
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new ReceiptSlot(fInventory, 3 * y + x, 80 + 18 * x, 17 + 18 * y, enabled));
            }
        }

        // Receipt output, 9
        addSlot(new HiddenSlot(fInventory, FactoryEntity.resultSlotIndex, 152, 17));

        // InBox, 10-18
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int index = 3 * y + x;
                ItemSlot slot = new ItemSlot(fInventory, FactoryEntity.resultSlotIndex + 1 + index, 8 + 18 * x, 17 + 18 * y, enabled);
                addSlot(slot);
                if (enabled) {
                    slot.lockItem(slots.get(index));
                }
            }
        }
        // Outbox, 19
        ItemSlot outSlot = new ItemSlot(fInventory, FactoryEntity.outputSlotIndex, 152, 53, false);
        addSlot(outSlot);
        if (enabled) {
            outSlot.lockItem(outSlot.getStack());
        }

        // Player Hotbar, 0-8
        for (int x = 0; x < 9; ++x) {
            addSlot(new PlayerSlot(pInventory, x, 8 + x * 18, 142));
        }

        // Player Inventory, 9-35
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                addSlot(new PlayerSlot(pInventory, 9 + 9 * y + x, 8 + 18 * x, 84 + 18 * y));
            }
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public void addScreen(FactoryScreen factoryScreen) {
        screens.add(factoryScreen);
    }

    @Override
    public void populateRecipeFinder(RecipeFinder finder) {
        for (int index = 0; index < FactoryEntity.resultSlotIndex; ++index) {
            ItemStack itemstack = fInventory.getStack(index);
            finder.addNormalItem(itemstack);
        }
    }

    @Override
    public void clearCraftingSlots() {
        fInventory.clear();
    }

    @Override
    public boolean matches(Recipe<? super CraftingInventory> recipe) {
        CraftingInventory ci = new CraftingInventory(new DummyContainer(), 3, 3);
        for (int i = 0; i < 9; i++) {
            ci.setStack(i, fInventory.getStack(i));
        }
        return recipe.matches(ci, pInventory.player.world);
    }

    @Override
    public int getCraftingResultSlotIndex() {
        // Crafting output, not factory output
        return FactoryEntity.resultSlotIndex;
    }

    @Override
    public int getCraftingWidth() {
        return 3;
    }

    @Override
    public int getCraftingHeight() {
        return 3;
    }

    @Override
    public int getCraftingSlotCount() {
        return 10;
    }

    /*
    @Override
    public List<RecipeBookGroup> getRecipeBookCategories() {
        return Lists.newArrayList(
                RecipeBookGroup.SEARCH,
                RecipeBookGroup.CRAFTING_EQUIPMENT,
                RecipeBookGroup.CRAFTING_BUILDING_BLOCKS,
                RecipeBookGroup.CRAFTING_MISC,
                RecipeBookGroup.CRAFTING_REDSTONE
        );
    }
    */

    public void lockInput() {
        for (int i = 0; i < 10; i++) {
            Slot slot = slots.get(i);
            if (!(slot instanceof ReceiptSlot)) {
                continue;
            }

            ReceiptSlot rSlot = (ReceiptSlot) slot;
            if (rSlot.locked != enabled) {
                rSlot.locked = enabled;
            }
        }

        for (int i = 10; i < 20; i++) {
            Slot slot = slots.get(i);
            if (!(slot instanceof ItemSlot)) {
                continue;
            }

            ItemSlot itemSlot = (ItemSlot) slot;
            if (!enabled) {
                if (itemSlot.enabled) {
                    itemSlot.lockItem((Item) null);
                }
                continue;
            }

            ItemStack stack = slots.get(i - 10).getStack();
            if (stack.isEmpty()) {
                if (itemSlot.enabled) {
                    itemSlot.lockItem((Item) null);
                }
                continue;
            }

            itemSlot.lockItem(stack);
        }
    }

    public boolean activate() {
        return activate(true);
    }

    public boolean activate(boolean send) {
        //<editor-fold desc="ServerSide">
        if (pInventory.player.world != null && !pInventory.player.world.isClient) {
            enabled = true;
            lockInput();
            return true;
        }
        //</editor-fold>

        //<editor-fold desc="Client Side">
        Slot rOut = slots.get(FactoryEntity.resultSlotIndex);
        if (rOut.getStack().isEmpty()) {
            deactivate(send);
        }

        for (FactoryScreen screen : screens) {
            screen.enable();
        }

        if (enabled) {
            return true;
        }
        enabled = true;
        lockInput();
        fInventory.stateEnabled(true);
        if (send) {
            BlockPos pos = fInventory.getPos();
            StateEnabledMessage.sendToServer(pos, true);
        }
        return true;
        //</editor-fold>
    }

    public boolean deactivate() {
        return deactivate(true);
    }

    public boolean deactivate(boolean send) {
        //<editor-fold desc="Server Side">
        if (pInventory.player.world != null && !pInventory.player.world.isClient) {
            BlockPos pos = fInventory.getPos();
            for (int i = FactoryEntity.resultSlotIndex + 1; i <= FactoryEntity.outputSlotIndex; i++) {
                Slot slot = slots.get(i);
                if (slot instanceof ItemSlot) {
                    ItemStack stack = slot.getStack();
                    if (!stack.isEmpty()) {
                        pInventory.offerOrDrop(pInventory.player.world, stack);
                    }
                    ((ItemSlot) slot).enabled = false;
                }
            }

            enabled = false;
            lockInput();
            sendContentUpdates();
            return true;
        }
        //</editor-fold>

        //<editor-fold desc="Client Side">
        for (FactoryScreen screen : screens) {
            screen.disable();
        }
        if (!enabled) {
            return true;
        }
        for (int i = 10; i < 20; i++) {
            Slot slot = slots.get(i);
            if (slot instanceof ItemSlot) {
                ((ItemSlot) slot).enabled = false;
            }
        }

        enabled = false;
        lockInput();
        fInventory.stateEnabled(false);
        if (send) {
            BlockPos pos = fInventory.getPos();
            StateEnabledMessage.sendToServer(pos, false);
        }
        return true;
        //</editor-fold>
    }

    @Override
    public void onContentChanged(Inventory inventoryIn) {
        if (loading) {
            Factory.LOGGER.warn("onContentChanged() white loading");
            return;
        }
        super.onContentChanged(inventoryIn);

        if (pInventory.player.world.isClient) {
            return;
        }

        ItemStack oldStack = slots.get(FactoryEntity.resultSlotIndex).getStack().copy();
        CraftingRecipe recipe = fInventory.calculateRecipe();
        if (recipe == null) {
            setRecipeResult(ItemStack.EMPTY);
        } else {
            setRecipeResult(recipe.getOutput());
        }
        ItemStack newStack = slots.get(FactoryEntity.resultSlotIndex).getStack();
        if (!oldStack.isItemEqual(newStack)) {
            sendContentUpdates();
        }
    }

    private void setRecipeResult(ItemStack stack) {
        Slot slot = slots.get(FactoryEntity.resultSlotIndex);
        if (slot instanceof LockedSlot) {
            ((LockedSlot) slot).enabled = !stack.isEmpty();
        }
        slot.setStack(stack);
        ((ItemSlot) slots.get(FactoryEntity.outputSlotIndex)).lockItem(stack);
        sendContentUpdates();
    }

    @Override
    public void close(PlayerEntity playerIn) {
        super.close(playerIn);
        fInventory.onClose(playerIn);
        if (clientSide) {
            return;
        }
        fInventory.stateOpen(false);
    }

    public void updateEnabled(boolean send) {
        if (fInventory == null) {
            return;
        }
        boolean enabledState = fInventory.getState(FactoryBlock.enabledProperty);
        if (enabledState && !enabled) {
            activate(send);
        } else if (enabled && !enabledState) {
            deactivate(send);
        }
        lockInput();
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        // Tranfer to player invetory
        int from = FactoryEntity.outputSlotIndex + 1;
        int to = FactoryEntity.outputSlotIndex + 9 * 4;
        boolean reverse = true;

        ItemStack slotStack = slot.getStack();
        ItemStack newStack = slotStack.copy();

        // Transfer from player (when clicking in the player invetory)
        if (slot instanceof PlayerSlot) {
            // To recipe (when disabled)
            from = 0;
            to = FactoryEntity.resultSlotIndex - 1;
            reverse = false;

            if (enabled) {
                // To input (when enabled)
                from = FactoryEntity.resultSlotIndex + 1;
                to = FactoryEntity.outputSlotIndex - 1;
            }
        }

        //
        if (!this.insertItem(slotStack, from, to + 1, reverse)) {
            return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return newStack;
    }

    @Override
    public ItemStack onSlotClick(int slotId, int dragType, SlotActionType clickType, PlayerEntity player) {
        ItemStack itemStack = super.onSlotClick(slotId, dragType, clickType, player);
        if (slotId < FactoryEntity.resultSlotIndex) {
            onContentChanged(fInventory);
        }
        return itemStack;
    }

    @Environment(EnvType.CLIENT)
    public void updateSlotStacks(List<ItemStack> itemList) {
        super.updateSlotStacks(itemList);
        lockInput();
    }
}
