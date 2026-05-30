package com.thescorched.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.thescorched.client.model.CinderlingModel;
import com.thescorched.entity.custom.CinderlingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class CinderlingRenderer extends GeoEntityRenderer<CinderlingEntity, LivingEntityRenderState> {

    public CinderlingRenderer(EntityRendererProvider.Context context) {
        super(context, new CinderlingModel());
        this.withScale(1.0f);
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}