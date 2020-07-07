package se.puggan.factory.container;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.ImageButton;

public class EnabledButton extends ImageButton {
    public boolean enabled;
    private final Runnable onFunction;
    private final Runnable offFunction;

    public EnabledButton(int xIn, int yIn, boolean enabled, Runnable onPress, Runnable offPress) {
        super(
                xIn, yIn,
                20, 18,
                177, 0,
                19,
                FactoryScreen.GUI_MAP, (button) -> {
                }
        );
        this.enabled = enabled;
        onFunction = onPress;
        offFunction = offPress;
    }

    @Override
    // public void onPress() { #MCP
    public void func_230930_b_() {
        if(enabled) {
            enabled = false;
            offFunction.run();
            return;
        }
        enabled = true;
        onFunction.run();
    }

    @Override
    //public void renderButton(int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) { #MCP
    public void func_230431_b_(MatrixStack p_230431_1_, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().bindTexture(FactoryScreen.GUI_MAP);
        /*
         * Green button: 177, 0, 20, 18
         * Green pressed button: 177, 19, 20, 18
         * Red button: 198, 0, 20, 18
         * Red pressed button: 198, 19, 20, 18
         */
        RenderSystem.disableDepthTest();
        float textureX = enabled ? 177 : 198;
        //float textureY = isHovered() ? 19 : 0; #MCP
        float textureY = func_230449_g_() ? 19 : 0;

        // blit(x, y, textureX, textureY, width, height, 256, 256); #MCP
        func_238463_a_(p_230431_1_, field_230690_l_, field_230691_m_, textureX, textureY, field_230688_j_, field_230689_k_, 256, 256);
        RenderSystem.enableDepthTest();
    }
}
