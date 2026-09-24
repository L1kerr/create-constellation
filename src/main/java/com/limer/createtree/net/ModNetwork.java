package com.limer.createtree.net;

import com.limer.createtree.data.SkillApi;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {

	private ModNetwork() {
	}

	public static void register(final RegisterPayloadHandlersEvent event) {
		PayloadRegistrar registrar = event.registrar("1");

		registrar.playToClient(ProgressSyncPayload.TYPE, ProgressSyncPayload.STREAM_CODEC,
			(payload, context) -> context.enqueueWork(() -> ClientData.applyProgress(payload)));

		registrar.playToClient(TreeSyncPayload.TYPE, TreeSyncPayload.STREAM_CODEC,
			(payload, context) -> context.enqueueWork(() -> ClientData.applyTree(payload)));

		registrar.playToServer(UnlockRequestPayload.TYPE, UnlockRequestPayload.STREAM_CODEC,
			(payload, context) -> context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player)
					SkillApi.unlock(player, payload.item());
			}));

		registrar.playToClient(EventSyncPayload.TYPE, EventSyncPayload.STREAM_CODEC,
			(payload, context) -> context.enqueueWork(() -> ClientData.applyEvent(payload)));

		registrar.playToServer(IgnitePayload.TYPE, IgnitePayload.STREAM_CODEC,
			(payload, context) -> context.enqueueWork(() -> {
				if (context.player() instanceof ServerPlayer player)
					SkillApi.ignite(player);
			}));

	}

	public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
		PacketDistributor.sendToPlayer(player, payload);
	}

	public static void sendTreeTo(ServerPlayer player) {
		PacketDistributor.sendToPlayer(player, TreeSyncPayload.current());
	}

	public static void sendToServer(CustomPacketPayload payload) {
		PacketDistributor.sendToServer(payload);
	}
}
