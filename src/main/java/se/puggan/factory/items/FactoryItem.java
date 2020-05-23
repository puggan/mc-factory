package se.puggan.factory.items;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import se.puggan.factory.util.RegistryHandler;

public class FactoryItem extends BlockItem {
    public FactoryItem() {
        super(RegistryHandler.FACTORY_BLOCK.get(), new Item.Properties().group(ItemGroup.REDSTONE));
    }
}
