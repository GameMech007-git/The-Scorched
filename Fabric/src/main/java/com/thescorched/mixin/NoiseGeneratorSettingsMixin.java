package com.thescorched.mixin;

import com.thescorched.worldgen.ModBiomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.levelgen.VerticalAnchor;

@Mixin(NoiseGeneratorSettings.class)
public abstract class NoiseGeneratorSettingsMixin {

    @Unique
    private SurfaceRules.RuleSource thescorched$patchedSurfaceRule;

    @Inject(
            method = "surfaceRule",
            at = @At("RETURN"),
            cancellable = true
    )
    private void thescorched$addScorchedSurfaceRule(
            CallbackInfoReturnable<SurfaceRules.RuleSource> cir
    ) {
        if (this.thescorched$patchedSurfaceRule != null) {
            cir.setReturnValue(this.thescorched$patchedSurfaceRule);
            return;
        }

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
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, topLayer),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, underLayer),
                        SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, blackstone),
                        SurfaceRules.ifTrue(SurfaceRules.VERY_DEEP_UNDER_FLOOR, tuff),
                        SurfaceRules.ifTrue(SurfaceRules.ON_CEILING, basalt),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_CEILING, blackstone)
                )
        );

        this.thescorched$patchedSurfaceRule = SurfaceRules.sequence(scorchedSurface, original);
        cir.setReturnValue(this.thescorched$patchedSurfaceRule);
    }
}