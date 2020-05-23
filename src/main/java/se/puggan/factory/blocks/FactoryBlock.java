package se.puggan.factory.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public class FactoryBlock extends Block {
    public FactoryBlock() {
        super(Properties.from(Blocks.CRAFTING_TABLE));
    }
}
