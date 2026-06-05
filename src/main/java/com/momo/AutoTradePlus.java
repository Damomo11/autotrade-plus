package com.momo;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoTradePlus implements ModInitializer {
	public static final String MOD_ID = "autotrade-plus";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("autotrade-plus initialized");
	}
}
