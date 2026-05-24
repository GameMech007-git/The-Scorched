package com.thescorched.mixin;

import com.thescorched.worldgen.ModBiomes;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.levelgen.VerticalAnchor;

@Mixin(SurfaceRuleData.class)
public abstract class SurfaceRuleDataMixin {

    @Inject(
            method = "overworld",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void thescorched$addScorchedSurfaceRule(
            CallbackInfoReturnable<SurfaceRules.RuleSource> cir
    ) {
        SurfaceRules.RuleSource original = cir.getReturnValue();

        SurfaceRules.RuleSource blackstone = SurfaceRules.state(Blocks.BLACKSTONE.defaultBlockState());
        SurfaceRules.RuleSource basalt = SurfaceRules.state(Blocks.BASALT.defaultBlockState());
        SurfaceRules.RuleSource tuff = SurfaceRules.state(Blocks.TUFF.defaultBlockState());
        SurfaceRules.RuleSource magma = SurfaceRules.state(Blocks.MAGMA_BLOCK.defaultBlockState());

        SurfaceRules.ConditionSource highMountainLevel =
                SurfaceRules.yBlockCheck(VerticalAnchor.absolute(105), 0);

        SurfaceRules.RuleSource highMountainMagma = SurfaceRules.ifTrue(
                highMountainLevel,
                SurfaceRules.ifTrue(
                        SurfaceRules.noiseCondition(Noises.PATCH, -0.035, 0.035),
                        magma
                )
        );

        SurfaceRules.RuleSource lowGroundMagma = SurfaceRules.ifTrue(
                SurfaceRules.not(highMountainLevel),
                SurfaceRules.ifTrue(
                        SurfaceRules.noiseCondition(Noises.PATCH, -0.008, 0.008),
                        magma
                )
        );

        SurfaceRules.RuleSource topLayer = SurfaceRules.sequence(
                highMountainMagma,
                lowGroundMagma,
                SurfaceRules.ifTrue(
                        SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, 0.0),
                        basalt
                ),
                blackstone
        );

        SurfaceRules.RuleSource underLayer = SurfaceRules.sequence(
                SurfaceRules.ifTrue(
                        SurfaceRules.noiseCondition(Noises.NETHER_STATE_SELECTOR, 0.0),
                        basalt
                ),
                blackstone
        );

        SurfaceRules.RuleSource scorchedSurface = SurfaceRules.ifTrue(
                SurfaceRules.isBiome(ModBiomes.THESCORCHED),
                SurfaceRules.sequence(
                        // Top exposed terrain
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, topLayer),

                        // Exposed mountain/cliff sides directly under surface
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, underLayer),

                        // Deeper exposed cliff layers
                        SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, blackstone),

                        // Very deep exposed cliff layers
                        SurfaceRules.ifTrue(SurfaceRules.VERY_DEEP_UNDER_FLOOR, tuff),

                        // Undersides / overhangs
                        SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, basalt),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_CEILING, blackstone)
                )
        );
        System.out.println("[THE SCORCHED] SurfaceRuleDataMixin applied");
        cir.setReturnValue(SurfaceRules.sequence(scorchedSurface, original));
    }
}