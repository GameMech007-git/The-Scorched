package com.thescorched.block;

import com.thescorched.TheScorched;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    public static final Block SCORCHED_SHRUB = registerScorchedShrub("scorched_shrub");

    private static Block registerScorchedShrub(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(TheScorched.MOD_ID, name);

        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        Block block = new ScorchedShrubBlock(
                BlockBehaviour.Properties.of()
                        .setId(blockKey)
                        .noCollision()
                        .instabreak()
                        .sound(SoundType.GRASS)
        );

        Registry.register(
                BuiltInRegistries.BLOCK,
                id,
                block
        );

        Registry.register(
                BuiltInRegistries.ITEM,
                id,
                new BlockItem(
                        block,
                        new Item.Properties()
                                .setId(itemKey)
                )
        );

        return block;
    }

    public static void registerModBlocks() {
        TheScorched.LOGGER.info("Registering blocks for " + TheScorched.MOD_ID);
    }
}