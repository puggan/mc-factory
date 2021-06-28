package se.puggan.factory.container;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.client.util.RecipeBookCategories;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.RecipeBookContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.container.slot.HiddenSlot;
import se.puggan.factory.container.slot.ItemSlot;
import se.puggan.factory.container.slot.LockedSlot;
import se.puggan.factory.container.slot.PlayerSlot;
import se.puggan.factory.container.slot.ReceiptSlot;
import se.puggan.factory.Factory;
import se.puggan.factory.network.StateEnabledMessage;
import se.puggan.factory.util.RegistryHandler;

public class FactoryContainer extends RecipeBookContainer<CraftingInventory> {
    private final List<FactoryScreen> screens;
    private boolean enabled;
    private final PlayerInventory pInventory;
    public FactoryEntity fInventory;
    private boolean loading;
    private final boolean clientSide;

    public FactoryContainer(int windowId, PlayerInventory playerInventory, IInventory inventory) {
        super(RegistryHandler.FACTORY_CONTAINER.get(), windowId);
        screens = new ArrayList<>();
        loading = true;
        pInventory = playerInventory;
        clientSide = !(pInventory.player instanceof ServerPlayerEntity);
        if (!(inventory instanceof FactoryEntity)) {
            throw new RuntimeException("Bad inventory type, expected FactoryEntity");
        }
        fInventory = (FactoryEntity) inventory;
        // TODO why is fInventory.getPos() BlockPos.ZERO in the client
        if (playerInventory.player.world.isRemote) {
            fInventory.setWorld(FactoryBlock.lastWorld);
            fInventory.setPos(FactoryBlock.lastBlockPosition);
            FactoryBlock.lastWorld = null;
            FactoryBlock.lastBlockPosition = null;
        }
        enabled = fInventory.getState(FactoryBlock.enabledProperty);
        fInventory.openInventory(playerInventory.player);
        slotInventory();
        loading = false;
        if (enabled) {
            activate(false);
        } else {
            deactivate(false);
        }
        lockInput();
        onCraftMatrixChanged(fInventory);
    }

    public FactoryContainer(int windowId, PlayerInventory playerInventory, PacketBuffer extraData) {
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
                    slot.lockItem(inventorySlots.get(index));
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
    public boolean canInteractWith(@Nonnull PlayerEntity player) {
        return true;
    }

    public void addScreen(FactoryScreen factoryScreen) {
        screens.add(factoryScreen);
    }

    @Override
    // MCP-name: func_201771_a -> fillStackedContents
    public void func_201771_a(@Nonnull RecipeItemHelper itemHelper) {
        for (int index = 0; index < FactoryEntity.resultSlotIndex; ++index) {
            ItemStack itemstack = fInventory.getStackInSlot(index);
            itemHelper.accountPlainStack(itemstack);
        }
    }

    @Override
    public void clear() {
        fInventory.clear();
    }

    @Override
    public boolean matches(IRecipe<? super CraftingInventory> recipeIn) {
        CraftingInventory ci = new CraftingInventory(new DummyContainer(), 3, 3);
        for (int i = 0; i < 9; i++) {
            ci.setInventorySlotContents(i, fInventory.getStackInSlot(i));
        }
        return recipeIn.matches(ci, pInventory.player.world);
    }

    @Override
    public int getOutputSlot() {
        // Crafting output, not factory output
        return FactoryEntity.resultSlotIndex;
    }

    @Override
    public int getWidth() {
        return 3;
    }

    @Override
    public int getHeight() {
        return 3;
    }

    @Override
    public int getSize() {
        return 10;
    }

    @Override
    @Nonnull
    public List<RecipeBookCategories> getRecipeBookCategories() {
        return Lists.newArrayList(
                RecipeBookCategories.SEARCH,
                RecipeBookCategories.EQUIPMENT,
                RecipeBookCategories.BUILDING_BLOCKS,
                RecipeBookCategories.MISC,
                RecipeBookCategories.REDSTONE
        );
    }

    public void lockInput() {
        for (int i = 0; i < 10; i++) {
            Slot slot = inventorySlots.get(i);
            if (!(slot instanceof ReceiptSlot)) {
                continue;
            }

            ReceiptSlot rSlot = (ReceiptSlot) slot;
            if (rSlot.locked != enabled) {
                rSlot.locked = enabled;
            }
        }

        for (int i = 10; i < 20; i++) {
            Slot slot = inventorySlots.get(i);
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

            ItemStack stack = inventorySlots.get(i - 10).getStack();
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
        if (pInventory.player.world != null && !pInventory.player.world.isRemote) {
            enabled = true;
            lockInput();
            return true;
        }
        //</editor-fold>

        //<editor-fold desc="Client Side">
        Slot rOut = inventorySlots.get(FactoryEntity.resultSlotIndex);
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
        if (pInventory.player.world != null && !pInventory.player.world.isRemote) {
            BlockPos pos = fInventory.getPos();
            for (int i = FactoryEntity.resultSlotIndex + 1; i <= FactoryEntity.outputSlotIndex; i++) {
                Slot slot = inventorySlots.get(i);
                if (slot instanceof ItemSlot) {
                    ItemStack stack = slot.getStack();
                    if (!stack.isEmpty()) {
                        pInventory.addItemStackToInventory(stack);
                    }
                    if (!stack.isEmpty()) {
                        InventoryHelper.spawnItemStack(pInventory.player.world, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                    ((ItemSlot) slot).enabled = false;
                }
            }

            enabled = false;
            lockInput();
            detectAndSendChanges();
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
            Slot slot = inventorySlots.get(i);
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
    public void onCraftMatrixChanged(@Nonnull IInventory inventoryIn) {
        if (loading) {
            Factory.LOGGER.warn("onCraftMatrixChanged() white loading");
            return;
        }
        super.onCraftMatrixChanged(inventoryIn);

        if (pInventory.player.world.isRemote) {
            return;
        }

        ItemStack oldStack = inventorySlots.get(FactoryEntity.resultSlotIndex).getStack().copy();
        ICraftingRecipe recipe = fInventory.calculateRecipe();
        if (recipe == null) {
            setRecipeResult(ItemStack.EMPTY);
        } else {
            setRecipeResult(recipe.getRecipeOutput());
        }
        ItemStack newStack = inventorySlots.get(FactoryEntity.resultSlotIndex).getStack();
        if (!oldStack.equals(newStack, false)) {
            detectAndSendChanges();
        }
    }

    private void setRecipeResult(ItemStack stack) {
        Slot slot = inventorySlots.get(FactoryEntity.resultSlotIndex);
        if (slot instanceof LockedSlot) {
            ((LockedSlot) slot).enabled = !stack.isEmpty();
        }
        slot.putStack(stack);
        ((ItemSlot) inventorySlots.get(FactoryEntity.outputSlotIndex)).lockItem(stack);
        detectAndSendChanges();
    }

    @Override
    public void onContainerClosed(@Nonnull PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);
        fInventory.closeInventory(playerIn);
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
    public ItemStack transferStackInSlot(PlayerEntity player, int index) {
        Slot slot = this.inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) {
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
        if (!this.mergeItemStack(slotStack, from, to + 1, reverse)) {
            return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }

        onCraftMatrixChanged(fInventory);

        return newStack;
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, PlayerEntity player) {
        ItemStack itemStack = super.slotClick(slotId, dragType, clickType, player);
        if (slotId < FactoryEntity.resultSlotIndex) {
            onCraftMatrixChanged(fInventory);
        }
        return itemStack;
    }

    @OnlyIn(Dist.CLIENT)
    public void setAll(List<ItemStack> itemList) {
        super.setAll(itemList);
        lockInput();
    }
}
