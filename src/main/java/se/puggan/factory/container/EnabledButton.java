package se.puggan.factory.container;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.MinecraftClient;

public class EnabledButton extends TexturedButtonWidget {
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
    public void renderButton(MatrixStack p_230431_1_, int p_renderButton_1_, int p_renderButton_2_, float p_renderButton_3_) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        minecraft.getTextureManager().bindTexture(FactoryScreen.GUI_MAP);
        /*
         * Green button: 177, 0, 20, 18
         * Green pressed button: 177, 19, 20, 18
         * Red button: 198, 0, 20, 18
         * Red pressed button: 198, 19, 20, 18
         */
        RenderSystem.disableDepthTest();
        float textureX = enabled ? 177 : 198;
        float textureY = isHovered() ? 19 : 0;

        drawTexture(p_230431_1_, x, y, textureX, textureY, width, height, 256, 256);
        RenderSystem.enableDepthTest();
    }
}
