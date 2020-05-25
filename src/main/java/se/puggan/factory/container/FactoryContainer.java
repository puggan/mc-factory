package se.puggan.factory.container;

import com.google.common.collect.Lists;
import net.minecraft.client.util.RecipeBookCategories;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.IRecipeHelperPopulator;
import net.minecraft.inventory.container.RecipeBookContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IntReferenceHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import se.puggan.factory.Factory;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.container.slot.*;
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
    private final int resultSlotIndex = 9;
    private final int outputSlotIndex = 19;
    private final int enabledSlotIndex = 20;

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
            fInventory.stateOpen(true);
            enabled = fInventory.getState(FactoryBlock.enabledProperty);
        }
        slotInventory();
        loading = false;
        onCraftMatrixChanged(cInvetory);
    }

    public FactoryContainer(int windowId, PlayerInventory playerInventory, PacketBuffer extraData) {
        this(windowId, playerInventory, new FactoryEntity());
    }

    public void setWorldAndPos(World world, BlockPos pos) {
        fInventory.setWorldAndPos(world, pos);
    }

    private void slotInventory() {
        // Receipt Inventory, 0-8
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                addSlot(new ReceiptSlot(cInvetory, 3 * y + x, 79 + 18 * x, 16 + 18 * y));
            }
        }
        // Receipt output, 9
        addSlot(new LockedSlot(craftingResultInventory, 0, 151, 16, enabled));

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

        addSlot(new LockedSlot(new SlotInvetory(enabled ? craftingResultInventory.getStackInSlot(0).copy() : ItemStack.EMPTY), 0, 0, 0, false));

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
    public void fillStackedContents(RecipeItemHelper itemHelperIn) {
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
        fInventory.modeCrafting = true;
        boolean matches = recipeIn.matches(cInvetory, pInventory.player.world);
        fInventory.modeCrafting = false;
        return matches;
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

    public boolean activate() {
        if (enabled) return true;
        if (fInventory.getRecipeUsed() == null) {
            Factory.LOGGER.debug("Tried to activate with no Recipet loaded");
            return false;
        }
        if (screen != null) {
            screen.enable();
        }
        for (int i = 10; i < 20; i++) {
            Slot s = inventorySlots.get(i);
            if (s instanceof ItemSlot) {
                ((ItemSlot) s).lockItem(inventorySlots.get(i - 10));
            }
        }

        // TODO send to server, right now this function is only run on the client side
        Factory.LOGGER.info("activate() @ " + (pInventory.player instanceof ServerPlayerEntity ? "Server" : "Client") + "/" + (pInventory.player.world.isRemote ? "Remote" : "Local"));
        enabled = true;
        inventorySlots.get(enabledSlotIndex).putStack(craftingResultInventory.getStackInSlot(0).copy());
        fInventory.stateEnabled(true);
        detectAndSendChanges();
        return true;
    }

    public boolean deactivate() {
        if (!enabled) return true;
        for (int i = 10; i < 20; i++) {
            Slot slot = inventorySlots.get(i);
            if (slot instanceof ItemSlot) {
                if (slot.getHasStack()) {
                    // TODO test what happends if inventory is full, should we drop them on the ground?
                    pInventory.addItemStackToInventory(slot.getStack());
                    // InventoryHelper.dropInventoryItems
                }
                ((ItemSlot) slot).enabled = false;
            }
        }
        if (screen != null) {
            screen.disable();
        }
        // TODO send to server, right now this function is only run on the client side
        Factory.LOGGER.info("deactivate() @ " + (pInventory.player instanceof ServerPlayerEntity ? "Server" : "Client") + "/" + (pInventory.player.world.isRemote ? "Remote" : "Local"));
        enabled = false;
        inventorySlots.get(enabledSlotIndex).putStack(ItemStack.EMPTY);
        fInventory.stateEnabled(false);
        detectAndSendChanges();
        return true;
    }

    @Override
    public void onCraftMatrixChanged(IInventory inventoryIn) {
        if (loading) {
            return;
        }
        super.onCraftMatrixChanged(inventoryIn);

        ICraftingRecipe icraftingrecipe = fInventory.getRecipeUsed();
        if (icraftingrecipe != null && icraftingrecipe.matches(cInvetory, pInventory.player.world)) {
//            Factory.LOGGER.debug("onCraftMatrixChanged(): Use current Recipe");
            return;
        }

        Optional<ICraftingRecipe> optional = pInventory.player.world.getRecipeManager().getRecipe(IRecipeType.CRAFTING, cInvetory, pInventory.player.world);
        if (!optional.isPresent()) {
            if (icraftingrecipe != null) {
                fInventory.setRecipeUsed(null);
                Factory.LOGGER.debug("onCraftMatrixChanged(): no Recipe");
            }
            deactivate();
            setReciptResult(ItemStack.EMPTY);
            return;
        }

        icraftingrecipe = optional.get();
        Factory.LOGGER.debug("onCraftMatrixChanged(): Recipe " + icraftingrecipe.getId());
        fInventory.setRecipeUsed(icraftingrecipe);
        setReciptResult(icraftingrecipe.getCraftingResult(cInvetory));
    }

    private void setReciptResult(ItemStack stack) {
        Slot slot = inventorySlots.get(resultSlotIndex);
        if (slot instanceof LockedSlot) {
            ((LockedSlot) slot).enabled = !stack.isEmpty();
        }
        slot.putStack(stack);
        ((ItemSlot) inventorySlots.get(outputSlotIndex)).lockItem(stack);
        detectAndSendChanges();
        /*
        if(pInventory.player instanceof ServerPlayerEntity) {
            Factory.LOGGER.debug("setReciptResult(): stack " + stack.getCount() + "x " + stack.getItem().getRegistryName().toString());
            ((ServerPlayerEntity)pInventory.player).connection.sendPacket(
                    new SSetSlotPacket(windowId, resultSlotIndex, stack)
            );
        }
        */
    }

    @Override
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);
        //fInventory.stateEnabled(enabled);
        if(pInventory.player instanceof ServerPlayerEntity) {
            fInventory.stateEnabled(inventorySlots.get(enabledSlotIndex).getHasStack());
        }
        fInventory.stateOpen(false);
    }
}
