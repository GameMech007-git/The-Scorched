package com.thescorched.client;

import com.thescorched.client.render.CinderlingRenderer;
import com.thescorched.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.thescorched.client.ScorchedBiomeParticles;

public class TheScorchedClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ScorchedBiomeParticles.register();
        EntityRendererRegistry.register(ModEntities.CINDERLING, CinderlingRenderer::new);
    }
}