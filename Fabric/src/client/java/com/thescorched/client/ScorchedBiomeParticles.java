package com.thescorched.client;

import com.thescorched.worldgen.ModBiomes;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.core.particles.ParticleTypes;

public class ScorchedBiomeParticles {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) {
                return;
            }

            Holder<Biome> biome = client.level.getBiome(client.player.blockPosition());

            if (!biome.is(ModBiomes.THESCORCHED)) {
                return;
            }

            RandomSource random = client.player.getRandom();

            // Increase this for more ash
            for (int i = 0; i < 2; i++) {
                double x = client.player.getX() + (random.nextDouble() - 0.5) * 18.0;
                double y = client.player.getY() + random.nextDouble() * 6.0;
                double z = client.player.getZ() + (random.nextDouble() - 0.5) * 18.0;

                client.level.addParticle(
                        ParticleTypes.WHITE_ASH,
                        x,
                        y,
                        z,
                        0.0,
                        -0.01,
                        0.0
                );
            }
        });
    }
}