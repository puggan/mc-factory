package se.puggan.factory.container;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Stack;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.TickableElement;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import se.puggan.factory.container.slot.ItemSlot;
import se.puggan.factory.container.slot.ReceiptSlot;

public class FactoryScreen extends HandledScreen<FactoryContainer> implements TickableElement {
    /**
     * name: x, y, w, h.
     * background: 0, 0, 176, 166
     * slot off: 151, 16, 18, 18
     * slot on: 152, 83, 18, 18
     * Green button: 177, 0, 20, 18
     * Green pressed button: 177, 19, 20, 18
     * Red button: 198, 0, 20, 18
     * Red pressed button: 198, 19, 20, 18
     */
    public static final Identifier GUI_MAP = new Identifier("factory:textures/gui/factory_map.png");
    private static final Identifier RECIPE_BUTTON_TEXTURE = new Identifier("textures/gui/recipe_button.png");
    private final RecipeBookWidget recipeBookGui = new RecipeBookWidget();

    private boolean enabled;
    private final FactoryContainer fContainer;
    private EnabledButton enabledButton;
    private TexturedButtonWidget recipeButton;
    private final int rButtonX = 150;
    private final int rButtonY = 33;
    private final int eButtonX = 60;
    private final int eButtonY = 33;

    public FactoryScreen(FactoryContainer container, PlayerInventory inv, Text name) {
        super(container, inv, name);
        fContainer = container;
        fContainer.addScreen(this);
        enabled = fContainer.isEnabled();
    }

    public void recipeToggle(ButtonWidget button) {
        recipeBookGui.reset(false);
        recipeBookGui.toggleOpen();
        //guiLeft = recipeBookGui.updateScreenPosition(false, width, xSize); #MCP
        x = recipeBookGui.findLeftEdge(false, width, backgroundWidth);
        rePositionButtons();
    }

    public void rePositionButtons() {
        recipeButton.setPos(x + rButtonX, y + rButtonY);
        enabledButton.setPos(x + eButtonX, y + eButtonY);
    }

    public void enable() {
        enabled = true;
        enabledButton.enabled = true;
    }

    public void disable() {
        enabled = false;
        enabledButton.enabled = false;
    }

    @Override
    protected void init() {
        super.init();
        enabledButton = new EnabledButton(
                eButtonX,
                eButtonY,
                enabled,
                fContainer::activate,
                fContainer::deactivate
        );
        addButton(enabledButton);

        if (client == null) {
            throw new RuntimeException("Minecraft is null");
        }

        recipeBookGui.initialize(width, height, client, false, fContainer);
        children.add(recipeBookGui);
        setFocused(recipeBookGui);
        recipeButton = new TexturedButtonWidget(
                rButtonX,
                rButtonY,
                20,
                18,
                0,
                0,
                19,
                RECIPE_BUTTON_TEXTURE,
                this::recipeToggle
        );
        addButton(recipeButton);
        rePositionButtons();
    }

    public void tick() {
        super.tick();
        this.recipeBookGui.update();
    }

    public void render(MatrixStack p_230450_1_, int mouseX, int mouseY, float partialTicks) {
        renderBackground(p_230450_1_);
        recipeBookGui.render(p_230450_1_, mouseX, mouseY, partialTicks);
        super.render(p_230450_1_, mouseX, mouseY, partialTicks);

        boolean ghostItems = false;
        for (int slotIndex = FactoryEntity.resultSlotIndex + 1; slotIndex < FactoryEntity.outputSlotIndex; ++slotIndex) {
            Slot slot = handler.slots.get(slotIndex);
            if (slot.hasStack()) {
                continue;
            }
            if (slot instanceof ItemSlot) {
                ItemSlot iSlot = (ItemSlot) slot;
                if (!iSlot.enabled) {
                    continue;
                }
                ItemStack fakeStack = new ItemStack(iSlot.lockedItem, 1);
                if (!ghostItems) {
                    RenderSystem.pushMatrix();
                    RenderSystem.translatef(this.x, this.y, 0.0F);
                    ghostItems = true;
                }
                RenderSystem.depthFunc(515);
                itemRenderer.renderGuiItemIcon(fakeStack, slot.x, slot.y);
                RenderSystem.depthFunc(516);
                int alpha = (int) (0.7 * 0xff);
                int red = 0x8b;
                int blue = 0x8b;
                int green = 0x8b;
                int color = ((alpha * 0x100 + red) * 0x100 + blue) * 0x100 + green;
                DrawableHelper.fill(p_230450_1_, slot.x, slot.y, slot.x + 15, slot.y + 15, color);
            }
        }
        if (ghostItems) {
            RenderSystem.depthFunc(515);
            RenderSystem.popMatrix();
        }

        recipeBookGui.drawGhostSlots(p_230450_1_, x, y, true, partialTicks);
        drawMouseoverTooltip(p_230450_1_, mouseX, mouseY);
        recipeBookGui.drawTooltip(p_230450_1_, x, y, mouseX, mouseY);

        focusOn(recipeBookGui);
    }

    @Override
    protected void drawBackground(MatrixStack p_230450_1_, float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        if (client == null) {
            return;
        }
        client.getTextureManager().bindTexture(GUI_MAP);

        drawTexture(p_230450_1_, x, y, 0, 0, 0, backgroundWidth, backgroundHeight, 256, 256);
        renderSlotsBackgrounds(p_230450_1_);
    }

    private void renderSlotsBackgrounds(MatrixStack p_230450_1_) {
        List<Slot> normalSlots = new Stack<>();
        List<Slot> disabledSlots = new Stack<>();
        if (enabled) {
            for (int slotIndex = 0; slotIndex <= FactoryEntity.outputSlotIndex; ++slotIndex) {
                Slot slot = handler.slots.get(slotIndex);
                if (slot instanceof ReceiptSlot) {
                    disabledSlots.add(slot);
                } else if (slot instanceof ItemSlot) {
                    if (((ItemSlot) slot).enabled) {
                        normalSlots.add(slot);
                    } else {
                        disabledSlots.add(slot);
                    }
                }
            }
        } else {
            for (int slotIndex = 0; slotIndex <= FactoryEntity.resultSlotIndex; ++slotIndex) {
                normalSlots.add(handler.slots.get(slotIndex));
            }
        }
        RenderSystem.pushMatrix();
        RenderSystem.translatef(x - 1, y - 1, 0.0F);

        if (client == null) {
            return;
        }
        client.getTextureManager().bindTexture(GUI_MAP);
        for (Slot slot : disabledSlots) {
            // slot off: 151, 16, 18, 18
            drawTexture(p_230450_1_, slot.x, slot.y, 151, 16, 18, 18);
        }
        for (Slot slot : normalSlots) {
            // slot on: 152, 83, 18, 18
            drawTexture(p_230450_1_, slot.x, slot.y, 152, 83, 18, 18);
        }
        RenderSystem.popMatrix();
    }
}
