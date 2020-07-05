package se.puggan.factory.container;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.recipebook.RecipeBookGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import se.puggan.factory.container.slot.ItemSlot;

public class FactoryScreen extends ContainerScreen<FactoryContainer> {
    private final ResourceLocation GUI_OFF = new ResourceLocation("factory:textures/gui/factory.png");
    private final ResourceLocation GUI_ON = new ResourceLocation("factory:textures/gui/factory_enabled.png");
    private static final ResourceLocation RECIPE_BUTTON_TEXTURE = new ResourceLocation("textures/gui/recipe_button.png");
    private final RecipeBookGui recipeBookGui = new RecipeBookGui();

    private boolean enabled;
    private final FactoryContainer fContainer;
    private EnabledButton enabledButton;
    private ImageButton recipeButton;

    public FactoryScreen(FactoryContainer container, PlayerInventory inv, ITextComponent name) {
        super(container, inv, name);
        fContainer = container;
        fContainer.setScreen(this);
        enabled = fContainer.isEnabled();
    }

    public void recipeToggle(Button button) {
        recipeBookGui.initSearchBar(false);
        recipeBookGui.toggleVisibility();
        guiLeft = recipeBookGui.updateScreenPosition(false, width, xSize);
        rePositionButtons();
    }

    public void rePositionButtons() {
        int xRButton = 151;
        int yRButton = 34;
        int xEButton = 61;
        int yEButton = 34;
        recipeButton.setPosition(guiLeft + xRButton, guiTop + yRButton);
        enabledButton.setPosition(guiLeft + xEButton, guiTop + yEButton);
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
                enabled,
                fContainer::activate,
                fContainer::deactivate
        );
        addButton(enabledButton);

        if (minecraft == null) {
            throw new RuntimeException("Minecraft is null");
        }

        recipeBookGui.init(width, height, minecraft, false, fContainer);
        children.add(recipeBookGui);
        setFocusedDefault(recipeBookGui);
        recipeButton = new ImageButton(
                0,
                0,
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
        this.recipeBookGui.tick();
    }

    public void render(int mouseX, int mouseY, float partialTicks) {
        renderBackground();
        recipeBookGui.render(mouseX, mouseY, partialTicks);
        super.render(mouseX, mouseY, partialTicks);

        if(container instanceof FactoryContainer) {
            FactoryContainer fInvetory = container;
            boolean ghostItems = false;
            for(int slotIndex = fInvetory.resultSlotIndex + 1; slotIndex < fInvetory.outputSlotIndex; ++slotIndex) {
                Slot slot = fInvetory.inventorySlots.get(slotIndex);
                if(slot.getHasStack()) {
                    continue;
                }
                if(slot instanceof ItemSlot) {
                    ItemSlot iSlot = (ItemSlot) slot;
                    if(!iSlot.enabled) {
                        continue;
                    }
                    ItemStack fakeStack = new ItemStack(iSlot.lockedItem, 1);
                    if(!ghostItems) {
                        RenderSystem.pushMatrix();
                        RenderSystem.translatef(this.guiLeft, this.guiTop, 0.0F);
                        //RenderSystem.enableRescaleNormal();
                        ghostItems = true;
                    }
                    RenderSystem.depthFunc(515);
                    itemRenderer.renderItemIntoGUI(fakeStack, slot.xPos, slot.yPos);
                    RenderSystem.depthFunc(516);
                    int alpha = (int) (0.7 * 0xff);
                    int red = 0x8b;
                    int blue = 0x8b;
                    int green = 0x8b;
                    int color = ((alpha * 0x100 + red)*0x100 + blue)*0x100 + green;
                    AbstractGui.fill(slot.xPos, slot.yPos, slot.xPos + 16, slot.yPos+ 16, color);
                }
            }
            if(ghostItems) {
                RenderSystem.depthFunc(515);
                RenderSystem.popMatrix();
            }
        }

        recipeBookGui.renderGhostRecipe(guiLeft, guiTop, true, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
        recipeBookGui.renderTooltip(guiLeft, guiTop, mouseX, mouseY);
        func_212932_b(recipeBookGui);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        minecraft.getTextureManager().bindTexture(enabled ? GUI_ON : GUI_OFF);
        blit(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}
