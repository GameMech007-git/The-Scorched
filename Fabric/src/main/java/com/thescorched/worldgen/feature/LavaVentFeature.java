package com.thescorched.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class LavaVentFeature extends Feature<NoneFeatureConfiguration> {

    public LavaVentFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        BlockPos center = findSurface(level, origin);

        if (center == null) {
            return false;
        }

        int outerRadius = 8; // around 16x16
        int lavaRadius = 3;
        int magmaRadius = 6;

        // Prevent vents from spawning on cliff edges / tiny floating areas
        if (!hasEnoughGround(level, center, outerRadius)) {
            return false;
        }

        for (int x = -outerRadius; x <= outerRadius; x++) {
            for (int z = -outerRadius; z <= outerRadius; z++) {
                double distance = Math.sqrt(x * x + z * z);

                // Uneven natural edge
                double edgeNoise = random.nextDouble() * 1.4 - 0.7;

                if (distance > outerRadius + edgeNoise) {
                    continue;
                }

                BlockPos surfacePos = center.offset(x, 0, z);
                BlockPos floorPos = surfacePos.below();

                // If this exact part has no support, skip it
                if (!hasSolidSupport(level, floorPos)) {
                    continue;
                }

                // Center lava pool
                if (distance <= lavaRadius + edgeNoise * 0.3) {
                    level.setBlock(floorPos, Blocks.LAVA.defaultBlockState(), 3);
                    level.setBlock(surfacePos, Blocks.AIR.defaultBlockState(), 3);

                    // Deeper lava pockets
                    if (random.nextFloat() < 0.45f && hasSolidSupport(level, floorPos.below())) {
                        level.setBlock(floorPos.below(), Blocks.LAVA.defaultBlockState(), 3);
                    }
                }

                // Hot cracked inner ring
                else if (distance <= magmaRadius + edgeNoise * 0.4) {
                    float roll = random.nextFloat();

                    if (roll < 0.58f) {
                        level.setBlock(floorPos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                    } else if (roll < 0.78f) {
                        level.setBlock(floorPos, Blocks.BASALT.defaultBlockState(), 3);
                    } else if (roll < 0.94f) {
                        level.setBlock(floorPos, Blocks.BLACKSTONE.defaultBlockState(), 3);
                    } else {
                        level.setBlock(floorPos, Blocks.LAVA.defaultBlockState(), 3);
                    }

                    level.setBlock(surfacePos, Blocks.AIR.defaultBlockState(), 3);
                }

                // Outer rocky burnt rim
                else {
                    float roll = random.nextFloat();

                    if (roll < 0.42f) {
                        level.setBlock(floorPos, Blocks.BLACKSTONE.defaultBlockState(), 3);
                    } else if (roll < 0.72f) {
                        level.setBlock(floorPos, Blocks.BASALT.defaultBlockState(), 3);
                    } else if (roll < 0.90f) {
                        level.setBlock(floorPos, Blocks.TUFF.defaultBlockState(), 3);
                    } else {
                        level.setBlock(floorPos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                    }

                    level.setBlock(surfacePos, Blocks.AIR.defaultBlockState(), 3);

                    // Raised rocky chunks around the edge
                    if (distance > magmaRadius && random.nextFloat() < 0.22f) {
                        BlockPos rockPos = surfacePos;

                        if (random.nextBoolean()) {
                            level.setBlock(rockPos, Blocks.BASALT.defaultBlockState(), 3);
                        } else {
                            level.setBlock(rockPos, Blocks.BLACKSTONE.defaultBlockState(), 3);
                        }

                        if (random.nextFloat() < 0.30f) {
                            level.setBlock(rockPos.above(), Blocks.BASALT.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        addLavaCracks(level, center, random, outerRadius, magmaRadius);

        return true;
    }

    private void addLavaCracks(
            WorldGenLevel level,
            BlockPos center,
            RandomSource random,
            int outerRadius,
            int magmaRadius
    ) {
        int crackCount = 3 + random.nextInt(4); // 3 to 6 cracks

        for (int i = 0; i < crackCount; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int length = 5 + random.nextInt(5); // 5 to 9 blocks

            for (int step = 3; step < length && step < outerRadius; step++) {
                int x = (int) Math.round(Math.cos(angle) * step);
                int z = (int) Math.round(Math.sin(angle) * step);

                BlockPos crackFloorPos = center.offset(x, -1, z);
                BlockPos crackAirPos = crackFloorPos.above();

                if (!hasSolidSupport(level, crackFloorPos)) {
                    continue;
                }

                if (random.nextFloat() < 0.45f && step < magmaRadius) {
                    level.setBlock(crackFloorPos, Blocks.LAVA.defaultBlockState(), 3);
                } else {
                    level.setBlock(crackFloorPos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
                }

                level.setBlock(crackAirPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private boolean hasEnoughGround(WorldGenLevel level, BlockPos center, int radius) {
        int checked = 0;
        int solid = 0;

        for (int x = -radius; x <= radius; x += 2) {
            for (int z = -radius; z <= radius; z += 2) {
                double distance = Math.sqrt(x * x + z * z);

                if (distance > radius) {
                    continue;
                }

                checked++;

                BlockPos floorPos = center.offset(x, -1, z);

                if (hasSolidSupport(level, floorPos)) {
                    solid++;
                }
            }
        }

        // Require at least 80% supported ground
        return checked > 0 && solid >= checked * 0.80;
    }

    private boolean hasSolidSupport(WorldGenLevel level, BlockPos floorPos) {
        return !level.isEmptyBlock(floorPos)
                && !level.isEmptyBlock(floorPos.below())
                && level.getBlockState(floorPos).getFluidState().isEmpty();
    }

    private BlockPos findSurface(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos mutable = origin.mutable();

        for (int y = level.getMaxY() - 1; y > level.getMinY(); y--) {
            mutable.set(origin.getX(), y, origin.getZ());

            if (!level.isEmptyBlock(mutable) && level.isEmptyBlock(mutable.above())) {
                return mutable.above().immutable();
            }
        }

        return null;
    }
}