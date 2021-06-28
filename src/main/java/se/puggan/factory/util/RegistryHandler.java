package se.puggan.factory.util;

import net.minecraft.block.Block;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import se.puggan.factory.Factory;
import se.puggan.factory.blocks.FactoryBlock;
import se.puggan.factory.container.FactoryContainer;
import se.puggan.factory.container.FactoryEntity;
import se.puggan.factory.container.FactoryScreen;
import se.puggan.factory.items.FactoryItem;
import se.puggan.factory.network.FactoryNetwork;

public class RegistryHandler {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Factory.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Factory.MOD_ID);
    public static final DeferredRegister<TileEntityType<?>> TILE_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Factory.MOD_ID);
    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, Factory.MOD_ID);

    public static void init() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        BLOCKS.register(modEventBus);
        TILE_ENTITIES.register(modEventBus);
        CONTAINERS.register(modEventBus);
        FactoryNetwork.init();
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    public static void initServerOnly() {

    }

    @OnlyIn(Dist.CLIENT)
    public static void initClientOnly() {
        ScreenManager.registerFactory(FACTORY_CONTAINER.get(), FactoryScreen::new);
    }

    public static final RegistryObject<Item> FACTORY_ITEM = ITEMS.register("factory", FactoryItem::new);

    public static final RegistryObject<Block> FACTORY_BLOCK = BLOCKS.register("factory", FactoryBlock::new);

    public static final RegistryObject<TileEntityType<FactoryEntity>> FACTORY_ENTITY = TILE_ENTITIES.register(
            "factory",
            () -> TileEntityType.Builder.create(
                    FactoryEntity::new,
                    FACTORY_BLOCK.get()
            ).build(null)
    );

    public static final RegistryObject<ContainerType<FactoryContainer>> FACTORY_CONTAINER = CONTAINERS.register(
            "factory",
            () -> IForgeContainerType.create(FactoryContainer::new)
    );
}
