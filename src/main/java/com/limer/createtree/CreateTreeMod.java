package com.limer.createtree;

import com.limer.createtree.config.SkillTreeLoader;
import com.limer.createtree.data.ModAttachments;
import com.limer.createtree.event.CraftEvents;
import com.limer.createtree.event.SyncEvents;
import com.limer.createtree.net.ModNetwork;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

@Mod(CreateTreeMod.MODID)
public class CreateTreeMod {

	public static final String MODID = "createtree";
	public static final Logger LOGGER = LogUtils.getLogger();

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}

	public CreateTreeMod(IEventBus modBus) {

		ModAttachments.REGISTER.register(modBus);
		modBus.addListener(ModNetwork::register);

		IEventBus forgeBus = NeoForge.EVENT_BUS;
		forgeBus.addListener(this::onAddReloadListener);
		forgeBus.register(CraftEvents.class);
		forgeBus.register(SyncEvents.class);
		forgeBus.register(com.limer.createtree.event.ServerTickEvents.class);

		LOGGER.info("Create Progression Tree initializing");
	}

	private void onAddReloadListener(AddReloadListenerEvent event) {
		event.addListener(new SkillTreeLoader());
	}
}
