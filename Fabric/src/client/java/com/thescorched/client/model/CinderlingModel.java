package com.thescorched.client.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.thescorched.TheScorched;
import com.thescorched.entity.custom.CinderlingEntity;
import net.minecraft.resources.Identifier;

public class CinderlingModel extends DefaultedEntityGeoModel<CinderlingEntity> {

    public CinderlingModel() {
        super(Identifier.fromNamespaceAndPath(TheScorched.MOD_ID, "cinderling"));
    }
}