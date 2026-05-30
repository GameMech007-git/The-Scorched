package com.thescorched;

import com.thescorched.block.ModBlocks;
import com.thescorched.entity.ModEntities;
import com.thescorched.worldgen.ModFeatures;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TheScorched implements ModInitializer {

	public static final String MOD_ID = "thescorched";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.registerModBlocks();
		ModFeatures.registerModFeatures();

		ModEntities.register();

		FabricDefaultAttributeRegistry.register(
				ModEntities.CINDERLING,
				ModEntities.createCinderlingAttributes()
		);

		LOGGER.info("The Scorched initialized");
	}
}