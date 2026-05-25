package com.thescorched.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ScorchedVolcanicVentFeature extends Feature<NoneFeatureConfiguration> {

    public ScorchedVolcanicVentFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        // Heightmap should place us on the surface, but make sure the block below is volcanic ground.
        BlockPos ground = origin.below();
        if (!isScorchedGround(level.getBlockState(ground))) {
            return false;
        }

        int radius = 3 + random.nextInt(2); // 3-4
        int height = 2 + random.nextInt(2); // 2-3

        buildMound(level, random, origin, radius, height);
        buildCrater(level, origin, height);

        return true;
    }

    private void buildMound(WorldGenLevel level, RandomSource random, BlockPos origin, int radius, int height) {
        for (int y = 0; y <= height; y++) {
            int layerRadius = Math.max(1, radius - y);

            for (int dx = -layerRadius; dx <= layerRadius; dx++) {
                for (int dz = -layerRadius; dz <= layerRadius; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);

                    if (dist <= layerRadius + random.nextFloat() * 0.35F) {
                        BlockPos pos = origin.offset(dx, y, dz);

                        if (canReplace(level.getBlockState(pos))) {
                            BlockState state = random.nextFloat() < 0.35F
                                    ? Blocks.BASALT.defaultBlockState()
                                    : Blocks.BLACKSTONE.defaultBlockState();

                            setBlock(level, pos, state);
                        }
                    }
                }
            }
        }
    }

    private void buildCrater(WorldGenLevel level, BlockPos origin, int height) {
        BlockPos top = origin.above(height);

        // No lava source yet. Use magma so it looks hot but does not create lava sticks.
        setBlock(level, top, Blocks.MAGMA_BLOCK.defaultBlockState());

        // Small magma ring.
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos ring = top.relative(direction);
            if (canReplace(level.getBlockState(ring))) {
                setBlock(level, ring, Blocks.MAGMA_BLOCK.defaultBlockState());
            }
        }

        // Blackstone/basalt crater lip.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    BlockPos lip = top.offset(dx, 0, dz);
                    if (canReplace(level.getBlockState(lip))) {
                        setBlock(level, lip, Blocks.BLACKSTONE.defaultBlockState());
                    }
                }
            }
        }
    }

    private void carveLavaStream(WorldGenLevel level, RandomSource random, BlockPos origin, int radius) {
        Direction mainDirection = Direction.Plane.HORIZONTAL.getRandomDirection(random);

        BlockPos current = origin.above(2).relative(mainDirection, 2);

        int length = 4 + random.nextInt(5); // 4-8 blocks, safer for worldgen

        for (int i = 0; i < length; i++) {
            current = findNextFlowPos(level, random, current, mainDirection);

            if (current == null || !isNearOrigin(origin, current)) {
                return;
            }

            placeStreamBlock(level, current);

            // Sometimes widen sideways a little.
            if (random.nextFloat() < 0.35F) {
                Direction side = random.nextBoolean()
                        ? mainDirection.getClockWise()
                        : mainDirection.getCounterClockWise();

                BlockPos sidePos = current.relative(side);
                if (isNearOrigin(origin, sidePos) && canReplace(level.getBlockState(sidePos))) {
                    setBlock(level, sidePos, Blocks.MAGMA_BLOCK.defaultBlockState());
                }
            }
        }
    }

    private boolean isNearOrigin(BlockPos origin, BlockPos pos) {
        return Math.abs(pos.getX() - origin.getX()) <= 7
                && Math.abs(pos.getZ() - origin.getZ()) <= 7;
    }

    private BlockPos findNextFlowPos(WorldGenLevel level, RandomSource random, BlockPos current, Direction mainDirection) {
        Direction side = random.nextBoolean()
                ? mainDirection.getClockWise()
                : mainDirection.getCounterClockWise();

        BlockPos[] candidates = new BlockPos[] {
                current.relative(mainDirection).below(),
                current.relative(mainDirection),
                current.relative(mainDirection).above(),
                current.relative(side).below(),
                current.relative(side),
                current.below()
        };

        for (BlockPos candidate : candidates) {
            BlockPos below = candidate.below();

            if ((canReplace(level.getBlockState(candidate)) || level.getBlockState(candidate).is(Blocks.LAVA))
                    && isScorchedGround(level.getBlockState(below))) {
                return candidate;
            }
        }

        return null;
    }

    private void placeStreamBlock(WorldGenLevel level, BlockPos pos) {
        setBlock(level, pos, Blocks.LAVA.defaultBlockState());

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos edge = pos.relative(direction);
            if (canReplace(level.getBlockState(edge))) {
                setBlock(level, edge, Blocks.MAGMA_BLOCK.defaultBlockState());
            }
        }
    }

    private boolean isScorchedGround(BlockState state) {
        return state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.MAGMA_BLOCK);
    }

    private boolean canReplace(BlockState state) {
        return state.isAir()
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.MAGMA_BLOCK);
    }
}