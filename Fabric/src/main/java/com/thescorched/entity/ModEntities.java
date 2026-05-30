package com.thescorched.entity;

import com.thescorched.TheScorched;
import com.thescorched.entity.custom.CinderlingEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

public class ModEntities {

    public static final Identifier CINDERLING_ID = Identifier.fromNamespaceAndPath(
            TheScorched.MOD_ID,
            "cinderling"
    );

    public static final ResourceKey<EntityType<?>> CINDERLING_KEY = ResourceKey.create(
            Registries.ENTITY_TYPE,
            CINDERLING_ID
    );

    public static final EntityType<CinderlingEntity> CINDERLING = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CINDERLING_ID,
            EntityType.Builder.of(CinderlingEntity::new, MobCategory.MONSTER)
                    .sized(0.7f, 0.45f)
                    .build(CINDERLING_KEY)
    );

    public static void register() {
        TheScorched.LOGGER.info("Registering entities for {}", TheScorched.MOD_ID);
    }

    public static AttributeSupplier.Builder createCinderlingAttributes() {
        return CinderlingEntity.createAttributes();
    }
}