package com.thescorched.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DryVegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ScorchedShrubBlock extends DryVegetationBlock {

    public ScorchedShrubBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.MAGMA_BLOCK);
    }
}