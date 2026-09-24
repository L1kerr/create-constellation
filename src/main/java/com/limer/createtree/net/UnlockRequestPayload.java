package com.limer.createtree.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.limer.createtree.CreateTreeMod;

public record UnlockRequestPayload(ResourceLocation item) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<UnlockRequestPayload> TYPE =
		new CustomPacketPayload.Type<>(CreateTreeMod.id("unlock_request"));

	public static final StreamCodec<RegistryFriendlyByteBuf, UnlockRequestPayload> STREAM_CODEC =
		StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, UnlockRequestPayload::item,
			UnlockRequestPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
