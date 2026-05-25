package com.thescorched.worldgen;

import com.thescorched.TheScorched;
import com.thescorched.worldgen.feature.ScorchedVolcanicVentFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class ModFeatures {

    public static final Feature<NoneFeatureConfiguration> SCORCHED_VOLCANIC_VENT =
            Registry.register(
                    BuiltInRegistries.FEATURE,
                    Identifier.fromNamespaceAndPath(TheScorched.MOD_ID, "scorched_volcanic_vent"),
                    new ScorchedVolcanicVentFeature(NoneFeatureConfiguration.CODEC)
            );

    public static void registerModFeatures() {
        TheScorched.LOGGER.info("Registering worldgen features for " + TheScorched.MOD_ID);
    }
}