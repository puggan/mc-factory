package se.puggan.factory.container;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.ImageButton;
import net.minecraft.util.ResourceLocation;

public class EnabledButton extends ImageButton /*implements Button.IPressable*/ {
    public boolean enabled;
    private static final ResourceLocation OFF_TEXTURE = new ResourceLocation("factory:textures/gui/off_button.png");
    private static final ResourceLocation ON_TEXTURE = new ResourceLocation("factory:textures/gui/on_button.png");
    private final Runnable onFunction;
    private final Runnable offFunction;

    public EnabledButton(boolean enabled, Runnable onPress, Runnable offPress) {
        super(0, 0, 18, 18, 0, 0, 18, enabled ? ON_TEXTURE : OFF_TEXTURE, (button) -> {});
        this.enabled = enabled;
        onFunction = onPress;
        offFunction = offPress;
    }

    @Override
    public void onPress() {
        if(enabled) {
            enabled = false;
            offFunction.run();
            return;
        }
        enabled = true;
        onFunction.run();
    }

    @Override
    public void renderButton(int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().bindTexture(enabled ? ON_TEXTURE : OFF_TEXTURE);
        RenderSystem.disableDepthTest();
        blit(x, y, (float)0, (float)0, width, height, 256, 256);
        RenderSystem.enableDepthTest();
    }
}
