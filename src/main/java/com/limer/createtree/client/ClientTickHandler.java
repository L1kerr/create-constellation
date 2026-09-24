package com.limer.createtree.client;

import com.limer.createtree.CreateTreeMod;

import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = CreateTreeMod.MODID, value = Dist.CLIENT)
public class ClientTickHandler {

	@SubscribeEvent
	public static void onClientTick(final ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null)
			return;
		while (ClientSetup.OPEN_TREE.consumeClick()) {
			mc.setScreen(new SkillTreeScreen());
		}
	}
}
