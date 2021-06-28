package se.puggan.factory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.container.FactoryContainer;
import se.puggan.factory.container.FactoryEntity;
import se.puggan.factory.container.FactoryScreen;
import se.puggan.factory.network.StateEnabledMessage;

public class Factory implements ModInitializer, DedicatedServerModInitializer, ClientModInitializer {
    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "factory";
    public static final Identifier factory_id = new Identifier(MOD_ID, MOD_ID);

    public static ScreenHandlerType<FactoryContainer> containerType;
    public static BlockEntityType<FactoryEntity> blockEntityType;

    public Factory() {
    }

    public void onInitialize() {
        FactoryBlock block = new FactoryBlock();
        Registry.register(Registry.BLOCK, factory_id, block);

        Item.Settings itemSetting = new Item.Settings().group(ItemGroup.REDSTONE);
        Registry.register(Registry.ITEM, factory_id, new BlockItem(block, itemSetting));

        blockEntityType = Registry.register(Registry.BLOCK_ENTITY_TYPE, factory_id, BlockEntityType.Builder.create(FactoryEntity::new, block).build(null));

        containerType = ScreenHandlerRegistry.registerExtended(factory_id, FactoryContainer::new);

        ServerSidePacketRegistry.INSTANCE.register(factory_id, StateEnabledMessage::receiveFromClient);
    }

    @Override
    public void onInitializeClient() {
        ScreenRegistry.register(containerType, FactoryScreen::new);
    }

    @Override
    public void onInitializeServer() {
    }
}
