package com.thescorched.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ScorchedVolcanicVentFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MAX_SAFE_RADIUS = 15;

    public ScorchedVolcanicVentFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        Integer originSurfaceY = findSurfaceY(level, origin.getX(), origin.getZ(), origin.getY());
        if (originSurfaceY == null) {
            return false;
        }

        BlockPos originSurface = new BlockPos(origin.getX(), originSurfaceY, origin.getZ());
        if (!isScorchedGround(level.getBlockState(originSurface))) {
            return false;
        }

        int radius = 5 + random.nextInt(15); // 5-19
        int height = 4 + random.nextInt(6);  // 4-9

        // Per-volcano personality — drives all the shape variation below.
        int    craterRadius   = 1 + random.nextInt(2);          // 1–2  (tight vs wide bowl)
        int    rimRadius      = 3 + random.nextInt(3);          // 3–5  (how far the rim extends)
        double slopePower     = 0.65 + random.nextDouble() * 0.6; // 0.65–1.25 (steep vs gentle slope)
        float  roughChance    = 0.06F + random.nextFloat() * 0.18F; // 6–24 % roughness
        float  basaltBias     = 0.15F + random.nextFloat() * 0.35F; // basalt share 15–50 %
        float  tuffBias       = 0.05F + random.nextFloat() * 0.20F; // tuff share  5–25 %

        buildVolcanoCone(level, random, originSurface, radius, height,
                craterRadius, rimRadius, slopePower, roughChance, basaltBias, tuffBias);
        buildCrater(level, random, originSurface, height, craterRadius, rimRadius);

        return true;
    }

    // ---------------------------------------------------------------
    // Cone
    // ---------------------------------------------------------------

    private void buildVolcanoCone(WorldGenLevel level, RandomSource random,
                                  BlockPos originSurface, int radius, int height,
                                  int craterRadius, int rimRadius,
                                  double slopePower, float roughChance,
                                  float basaltBias, float tuffBias) {
        int baseY = originSurface.getY();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos columnBase = originSurface.offset(dx, 0, dz);
                if (!isNearOrigin(originSurface, columnBase)) continue;

                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > radius) continue;

                Integer surfaceY = findSurfaceY(
                        level,
                        originSurface.getX() + dx,
                        originSurface.getZ() + dz,
                        originSurface.getY()
                );
                if (surfaceY == null) continue;

                int targetY;

                if (distance <= craterRadius) {
                    // Flat crater floor.
                    targetY = baseY + height - 2;
                } else if (distance <= rimRadius) {
                    // Raised rim ring.
                    targetY = baseY + height;
                } else {
                    // Cone slope — slopePower controls the silhouette:
                    // < 1.0 → concave (broad shield), > 1.0 → convex (steep stratovolcano).
                    double slopeProgress = (distance - rimRadius) / (double)(radius - rimRadius);
                    slopeProgress = Math.max(0.0, Math.min(1.0, slopeProgress));
                    double falloff = Math.pow(slopeProgress, slopePower);
                    targetY = baseY + height - 1 - (int) Math.floor(falloff * height);
                }

                // Feather the outermost ring gently into terrain.
                if (distance > radius - 1.0) {
                    targetY = Math.min(targetY, baseY + 1);
                }

                // Surface roughness — intensity varies per volcano.
                if (distance > rimRadius && random.nextFloat() < roughChance) {
                    targetY += random.nextBoolean() ? 1 : -1;
                }
                if (distance > rimRadius + 1 && distance < radius - 1 && random.nextFloat() < roughChance * 0.6F) {
                    targetY += random.nextBoolean() ? 1 : -1;
                }

                if (targetY <= surfaceY) continue;

                for (int y = surfaceY + 1; y <= targetY; y++) {
                    BlockPos placePos = new BlockPos(
                            originSurface.getX() + dx,
                            y,
                            originSurface.getZ() + dz
                    );
                    if (!isNearOrigin(originSurface, placePos)) continue;
                    if (canReplace(level.getBlockState(placePos))) {
                        setBlock(level, placePos, randomRockState(random, basaltBias, tuffBias));
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Crater
    // ---------------------------------------------------------------

    private void buildCrater(WorldGenLevel level, RandomSource random,
                             BlockPos originSurface, int height,
                             int craterRadius, int rimRadius) {
        int baseY        = originSurface.getY();
        int craterFloorY = baseY + height - 1;
        int rimY         = baseY + height;

        // --- Crater floor: magma bowl with lava pool ---
        for (int dx = -craterRadius; dx <= craterRadius; dx++) {
            for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > craterRadius + 0.25) continue;

                BlockPos floorPos = new BlockPos(
                        originSurface.getX() + dx,
                        craterFloorY,
                        originSurface.getZ() + dz
                );
                if (!isNearOrigin(originSurface, floorPos)) continue;

                setBlock(level, floorPos,
                        distance <= 1.0 ? Blocks.LAVA.defaultBlockState()
                                : Blocks.MAGMA_BLOCK.defaultBlockState());

                // Clear above so the bowl is open.
                BlockPos airPos = floorPos.above();
                if (isNearOrigin(originSurface, airPos)) {
                    setBlock(level, airPos, Blocks.AIR.defaultBlockState());
                }
            }
        }

        // --- Fill lava to rim level so it is ready to spill ---
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (Math.sqrt(dx * dx + dz * dz) > 1.5) continue;
                BlockPos lavaTop = new BlockPos(
                        originSurface.getX() + dx,
                        rimY,
                        originSurface.getZ() + dz
                );
                if (isNearOrigin(originSurface, lavaTop)) {
                    setBlock(level, lavaTop, Blocks.LAVA.defaultBlockState());
                }
            }
        }

        // --- Rim ring ---
        for (int dx = -(rimRadius + 1); dx <= rimRadius + 1; dx++) {
            for (int dz = -(rimRadius + 1); dz <= rimRadius + 1; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < craterRadius + 0.5 || distance > rimRadius + 0.3) continue;
                if (random.nextFloat() < 0.12F) continue;

                BlockPos rimPos = new BlockPos(
                        originSurface.getX() + dx,
                        rimY,
                        originSurface.getZ() + dz
                );
                if (isNearOrigin(originSurface, rimPos) && canReplace(level.getBlockState(rimPos))) {
                    setBlock(level, rimPos, randomRockState(random, 0.25F, 0.13F));
                }
            }
        }

        // --- Magma cracks near rim floor ---
        for (int i = 0; i < 4; i++) {
            int dx = -4 + random.nextInt(9);
            int dz = -4 + random.nextInt(9);
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance < 3.0 || distance > 4.6) continue;

            BlockPos crackPos = new BlockPos(
                    originSurface.getX() + dx,
                    craterFloorY,
                    originSurface.getZ() + dz
            );
            if (isNearOrigin(originSurface, crackPos) && canReplace(level.getBlockState(crackPos))) {
                setBlock(level, crackPos, Blocks.MAGMA_BLOCK.defaultBlockState());
            }
        }

        // --- Breach: ~50 % of craters flow, the rest stay sealed ---
        if (random.nextFloat() < 0.5F) {
            int[][] cardinals = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            int[] dir = cardinals[random.nextInt(4)];

            // 3-wide notch, cleared TWO blocks deep (rimY and rimY-1)
            // so lava has a full opening to escape through the wall.
            for (int side = -1; side <= 1; side++) {
                int ox = dir[1] * side;
                int oz = dir[0] * side;

                for (int step = 2; step <= 4; step++) {
                    int bx = dir[0] * step + ox;
                    int bz = dir[1] * step + oz;

                    for (int dy = 0; dy >= -1; dy--) {   // clear rimY and rimY-1
                        BlockPos notchPos = new BlockPos(
                                originSurface.getX() + bx,
                                rimY + dy,
                                originSurface.getZ() + bz
                        );
                        if (isNearOrigin(originSurface, notchPos)) {
                            setBlock(level, notchPos, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Integer findSurfaceY(WorldGenLevel level, int x, int z, int nearY) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int top    = Math.min(level.getMaxY() - 2, nearY + 10);
        int bottom = Math.max(level.getMinY() + 2, nearY - 14);

        for (int y = top; y >= bottom; y--) {
            cursor.set(x, y, z);
            BlockState state = level.getBlockState(cursor);
            BlockState above = level.getBlockState(cursor.above());
            if (isScorchedGround(state) && above.isAir()) {
                return y;
            }
        }
        return null;
    }

    private BlockState randomRockState(RandomSource random, float basaltBias, float tuffBias) {
        float roll = random.nextFloat();
        if (roll < basaltBias)              return Blocks.BASALT.defaultBlockState();
        if (roll < basaltBias + tuffBias)   return Blocks.TUFF.defaultBlockState();
        return Blocks.BLACKSTONE.defaultBlockState();
    }

    private boolean isNearOrigin(BlockPos origin, BlockPos pos) {
        return Math.abs(pos.getX() - origin.getX()) <= MAX_SAFE_RADIUS
                && Math.abs(pos.getZ() - origin.getZ()) <= MAX_SAFE_RADIUS;
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