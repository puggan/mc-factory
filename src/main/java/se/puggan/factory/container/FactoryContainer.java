package se.puggan.factory.container;

import com.google.common.collect.Lists;
import net.minecraft.client.util.RecipeBookCategories;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IRecipeHelperPopulator;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.RecipeBookContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ICraftingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeItemHelper;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import se.puggan.factory.Factory;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.container.slot.*;
import se.puggan.factory.network.FactoryNetwork;
import se.puggan.factory.network.SetRecipeUsedMessage;
import se.puggan.factory.network.StateEnabledMessage;
import se.puggan.factory.util.RegistryHandler;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class FactoryContainer extends RecipeBookContainer<CraftingInventory> implements IRecipeHelperPopulator {
    private FactoryScreen screen;
    private boolean enabled;
    private CraftingInventory cInvetory;
    private SlotInvetory craftingResultInventory;
    private CraftingInventory inputInvetory;
    private SlotInvetory outputStackInventory;
    private PlayerInventory pInventory;
    private FactoryEntity fInventory;
    private boolean loading;
    public final int resultSlotIndex = 9;
    public final int outputSlotIndex = 19;
    private boolean clientSide;

    public FactoryContainer(int windowId, PlayerInventory playerInventory, IInventory inventory) {
        super(RegistryHandler.FACTORY_CONTAINER.get(), windowId);
        loading = true;
        pInventory = playerInventory;
        cInvetory = new SyncedCraftingInventory(this, 3, 3, inventory, 0);
        craftingResultInventory = new SyncedSlotInventory(inventory, 9);
        inputInvetory = new SyncedCraftingInventory(this, 3, 3, inventory, 10);
        outputStackInventory = new SyncedSlotInventory(inventory, 19);
        if (inventory instanceof FactoryEntity) {
            fInventory = (FactoryEntity) inventory;
            World world = fInventory.getWorld();
            if(world == null) {
                fInventory.setWorldAndPos(world = FactoryBlock.lastWorld, FactoryBlock.lastBlockPosition);
            }
            clientSide = world.isRemote;
            if(!clientSide) {
                fInventory.stateOpen(true);
            }
            enabled = fInventory.getState(FactoryBlock.enabledProperty);
            fInventory.setContainer(this);
        } else {
            clientSide = !(pInventory.player instanceof ServerPlayerEntity);
        }
        slotInventory();
        loading = false;
        if(enabled) {
            activate(false);
        } else {
            deactivate(false);
        }
        onCraftMatrixChanged(cInvetory);
    }

    public FactoryContainer(int windowId, PlayerInventory playerInventory, PacketBuffer extraData) {
        this(windowId, playerInventory, new FactoryEntity());
    }

    private void slotInventory() {
        // Receipt Inventory, 0-8
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new ReceiptSlot(cInvetory, 3 * y + x, 79 + 18 * x, 16 + 18 * y, enabled));
            }
        }
        // Receipt output, 9
        addSlot(new HiddenSlot(craftingResultInventory, 0, 151, 16));

        // InBox, 10-18
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                int index = 3 * y + x;
                ItemSlot slot = new ItemSlot(inputInvetory, index, 7 + 18 * x, 16 + 18 * y, enabled);
                addSlot(slot);
                if (enabled) {
                    slot.lockItem(inventorySlots.get(index));
                }
            }
        }
        // Outbox, 19
        ItemSlot outSlot = new ItemSlot(outputStackInventory, 0, 151, 52, false);
        addSlot(outSlot);
        if (enabled) {
            outSlot.lockItem(craftingResultInventory.getStackInSlot(0));
        }

        // Player Hotbar, 0-8
        for (int x = 0; x < 9; ++x) {
            addSlot(new PlayerSlot(pInventory, x, 8 + x * 18, 143));
        }

        // Player Inventory, 9-35
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                addSlot(new PlayerSlot(pInventory, 9 + 9 * y + x, 8 + 18 * x, 85 + 18 * y));
            }
        }
    }

    @Override
    public boolean canInteractWith(@Nonnull PlayerEntity player) {
        return true;
    }

    public void setScreen(FactoryScreen factoryScreen) {
        screen = factoryScreen;
    }

    @Override
    public void fillStackedContents(@Nonnull RecipeItemHelper itemHelperIn) {
        cInvetory.fillStackedContents(itemHelperIn);
    }

    @Override
    public void clear() {
        cInvetory.clear();
        craftingResultInventory.clear();
        inputInvetory.clear();
        outputStackInventory.clear();
    }

    @Override
    public boolean matches(IRecipe<? super CraftingInventory> recipeIn) {
        return recipeIn.matches(cInvetory, pInventory.player.world);
    }

    @Override
    public int getOutputSlot() {
        // 0-8 is in, 9 is out
        return 9;
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
        Slot rOut = inventorySlots.get(resultSlotIndex);
        if(rOut.getStack().isEmpty()) {
            Factory.LOGGER.warn(enabled ? "lockInput(true) with NO Receipt" : "lockInput(false) with NO Receipt");
        } else {
            Factory.LOGGER.warn(enabled ? "lockInput(true) with Receipt" : "lockInput(false) with Receipt");
        }
        for (int i = 0; i < 10; i++) {
            Slot slot = inventorySlots.get(i);
            if (!(slot instanceof ReceiptSlot)) {
                continue;
            }

            ReceiptSlot rSlot = (ReceiptSlot) slot;
            if(rSlot.locked != enabled) {
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
                if(itemSlot.enabled) {
                    itemSlot.lockItem((Item) null);
                }
                continue;
            }

            ItemStack stack = inventorySlots.get(i - 10).getStack();
            if (stack.isEmpty()) {
                if(itemSlot.enabled) {
                    itemSlot.lockItem((Item) null);
                }
                continue;
            }

            itemSlot.lockItem(stack);
        }
    }

    public boolean activate() {return activate(true);}
    public boolean activate(boolean send) {
        //<editor-fold desc="ServerSide">
        if(pInventory.player.world != null && !pInventory.player.world.isRemote) {
            enabled = true;
            lockInput();
            return true;
        }
        //</editor-fold>

        //<editor-fold desc="Client Side">
        Slot rOut = inventorySlots.get(resultSlotIndex);
        if(rOut.getStack().isEmpty()) {
            deactivate(send);
        }

        if (screen != null) {
            screen.enable();
        }

        if (enabled) {
            return true;
        }
        enabled = true;
        lockInput();
        fInventory.stateEnabled(true);
        if(send) {
            FactoryNetwork.CHANNEL.sendToServer(new StateEnabledMessage(fInventory.getPos(), true));
        }
        return true;
        //</editor-fold>
    }

    public boolean deactivate() {return deactivate(true);}
    public boolean deactivate(boolean send) {
        //<editor-fold desc="Server Side">
        if(pInventory.player.world != null && !pInventory.player.world.isRemote) {
            boolean stuffToDrop = false;
            for (int i = resultSlotIndex + 1; i <= outputSlotIndex; i++) {
                Slot slot = inventorySlots.get(i);
                if (slot instanceof ItemSlot) {
                    ItemStack stack = slot.getStack();
                    if (!stack.isEmpty()) {
                        pInventory.addItemStackToInventory(stack);
                    }
                    if (!stack.isEmpty()) {
                        if(i == outputSlotIndex) {
                            InventoryHelper.dropInventoryItems(pInventory.player.world, fInventory.getPos(), outputStackInventory);
                        } else {
                            stuffToDrop = true;
                        }
                    }
                    ((ItemSlot) slot).enabled = false;
                }
            }

            if(stuffToDrop) {
                InventoryHelper.dropInventoryItems(pInventory.player.world, fInventory.getPos(), inputInvetory);
            }

            enabled = false;
            lockInput();
            detectAndSendChanges();
            return true;
        }
        //</editor-fold>

        //<editor-fold desc="Client Side">
        if (screen != null) {
            screen.disable();
        }
        if (!enabled) return true;
        for (int i = 10; i < 20; i++) {
            Slot slot = inventorySlots.get(i);
            if (slot instanceof ItemSlot) {
                ((ItemSlot) slot).enabled = false;
            }
        }

        enabled = false;
        lockInput();
        fInventory.stateEnabled(false);
        if(send) {
            FactoryNetwork.CHANNEL.sendToServer(new StateEnabledMessage(fInventory.getPos(), false));
        }
        return true;
        //</editor-fold>
    }

    @Override
    public void onCraftMatrixChanged(@Nonnull IInventory inventoryIn) {
        if (loading) {
            return;
        }
        super.onCraftMatrixChanged(inventoryIn);
        if(clientSide) {
            return;
        }

        //<editor-fold desc="ServerSide">
        ICraftingRecipe recipe = fInventory.getRecipeUsed();
        if (recipe != null && recipe.matches(cInvetory, pInventory.player.world)) {
            return;
        }

        Optional<ICraftingRecipe> optional = pInventory.player.world.getRecipeManager().getRecipe(IRecipeType.CRAFTING, cInvetory, pInventory.player.world);
        if (!optional.isPresent()) {
            if (recipe != null) {
                fInventory.setRecipeUsed(null);
            }
            deactivate(true);
            setReciptResult(ItemStack.EMPTY);
            return;
        }

        recipe = optional.get();
        fInventory.setRecipeUsed(recipe);
        if(pInventory.player instanceof ServerPlayerEntity) {
            (new SetRecipeUsedMessage(fInventory.getPos(), recipe)).sendToPlayer((ServerPlayerEntity) pInventory.player);
        }
        setReciptResult(recipe.getCraftingResult(cInvetory));
        //</editor-fold>
    }

    private void setReciptResult(ItemStack stack) {
        Slot slot = inventorySlots.get(resultSlotIndex);
        if (slot instanceof LockedSlot) {
            ((LockedSlot) slot).enabled = !stack.isEmpty();
        }
        slot.putStack(stack);
        ((ItemSlot) inventorySlots.get(outputSlotIndex)).lockItem(stack);
        detectAndSendChanges();
    }

    @Override
    public void onContainerClosed(@Nonnull PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);
        if(clientSide) {
            return;
        }
        fInventory.stateOpen(false);
    }

    public void updateEnabled(boolean send) {
        if(fInventory == null) return;
        boolean enabledState = fInventory.getState(FactoryBlock.enabledProperty);
        if(enabledState && !enabled) {
            activate(send);
        } else if(enabled && !enabledState) {
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
        int from = outputSlotIndex + 1;
        int to = outputSlotIndex + 9 * 4;
        boolean reverse = true;

        ItemStack slotStack = slot.getStack();
        ItemStack newStack = slotStack.copy();

        // Transfer from player (when clicking in the player invetory)
        if(slot instanceof PlayerSlot) {
            // To recipt (when disabled)
            from = 0;
            to =  resultSlotIndex-1;
            reverse = false;

            if(enabled) {
                // To input (when enabled)
                from = resultSlotIndex+1;
                to = outputSlotIndex-1;
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
        return newStack;
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickType, PlayerEntity player) {
        updateEnabled(true);
        return super.slotClick(slotId, dragType, clickType, player);
    }
}
