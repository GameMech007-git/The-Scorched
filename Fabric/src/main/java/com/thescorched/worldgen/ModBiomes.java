package com.thescorched.worldgen;

import com.thescorched.TheScorched;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class ModBiomes {

    public static final ResourceKey<Biome> THESCORCHED = ResourceKey.create(
            Registries.BIOME,
            Identifier.fromNamespaceAndPath(TheScorched.MOD_ID, "thescorched")
    );

    private ModBiomes() {
    }
}