package com.thescorched.mixin;

import com.thescorched.worldgen.ModBiomes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OverworldBiomeBuilder.class)
public abstract class OverworldBiomeBuilderMixin {

    /*
     * Main Badlands-style hook.
     * Vanilla uses pickBadlandsBiome when the climate is hot enough.
     * Replacing this makes The Scorched generate in Badlands-like regions.
     */
    @Inject(
            method = "pickBadlandsBiome",
            at = @At("HEAD"),
            cancellable = true
    )
    private void thescorched$pickScorchedBadlandsBiome(
            int humidityIndex,
            Climate.Parameter weirdness,
            CallbackInfoReturnable<ResourceKey<Biome>> cir
    ) {
        cir.setReturnValue(ModBiomes.THESCORCHED);
    }

    /*
     * Hot plateau regions.
     * This makes the biome wider like Badlands plateaus instead of only mountain peaks.
     */
    @Inject(
            method = "pickPlateauBiome",
            at = @At("HEAD"),
            cancellable = true
    )
    private void thescorched$pickScorchedPlateauBiome(
            int temperatureIndex,
            int humidityIndex,
            Climate.Parameter weirdness,
            CallbackInfoReturnable<ResourceKey<Biome>> cir
    ) {
        if (temperatureIndex == 4) {
            cir.setReturnValue(ModBiomes.THESCORCHED);
        }
    }

    /*
     * Hot peak regions.
     * Keeps volcanic mountains inside the larger scorched region.
     */
    @Inject(
            method = "pickPeakBiome",
            at = @At("HEAD"),
            cancellable = true
    )
    private void thescorched$pickScorchedPeakBiome(
            int temperatureIndex,
            int humidityIndex,
            Climate.Parameter weirdness,
            CallbackInfoReturnable<ResourceKey<Biome>> cir
    ) {
        if (temperatureIndex == 4) {
            cir.setReturnValue(ModBiomes.THESCORCHED);
        }
    }

    /*
     * Hot middle branches.
     * Vanilla sends temperatureIndex == 4 into badlands/desert-like choices here.
     */
    @Inject(
            method = "pickMiddleBiomeOrBadlandsIfHot",
            at = @At("HEAD"),
            cancellable = true
    )
    private void thescorched$pickScorchedMiddleOrBadlandsBiome(
            int temperatureIndex,
            int humidityIndex,
            Climate.Parameter weirdness,
            CallbackInfoReturnable<ResourceKey<Biome>> cir
    ) {
        if (temperatureIndex == 4) {
            cir.setReturnValue(ModBiomes.THESCORCHED);
        }
    }

    /*
     * Same hot branch, used in mountain/high slices.
     */
    @Inject(
            method = "pickMiddleBiomeOrBadlandsIfHotOrSlopeIfCold",
            at = @At("HEAD"),
            cancellable = true
    )
    private void thescorched$pickScorchedMiddleOrBadlandsOrSlopeBiome(
            int temperatureIndex,
            int humidityIndex,
            Climate.Parameter weirdness,
            CallbackInfoReturnable<ResourceKey<Biome>> cir
    ) {
        if (temperatureIndex == 4) {
            cir.setReturnValue(ModBiomes.THESCORCHED);
        }
    }
}