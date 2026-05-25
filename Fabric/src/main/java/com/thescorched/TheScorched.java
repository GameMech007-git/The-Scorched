package com.thescorched;

import com.thescorched.block.ModBlocks;
import com.thescorched.worldgen.ModFeatures;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TheScorched implements ModInitializer {

	public static final String MOD_ID = "thescorched";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModBlocks.registerModBlocks();
		ModFeatures.registerModFeatures();

		LOGGER.info("The Scorched initialized");
	}
}