package com.limer.createtree.client;

import com.limer.createtree.CreateTreeMod;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = CreateTreeMod.MODID, value = Dist.CLIENT)
public class ClientSetup {

	public static final KeyMapping OPEN_TREE = new KeyMapping(
		"key.createtree.open_tree",
		KeyConflictContext.IN_GAME,
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_I,
		"key.categories.createtree");

	@SubscribeEvent
	public static void onRegisterKeys(final RegisterKeyMappingsEvent event) {
		event.register(OPEN_TREE);
	}
}
