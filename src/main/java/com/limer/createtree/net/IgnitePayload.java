package com.limer.createtree.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.limer.createtree.CreateTreeMod;

public record IgnitePayload() implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<IgnitePayload> TYPE =
		new CustomPacketPayload.Type<>(CreateTreeMod.id("ignite"));

	public static final StreamCodec<RegistryFriendlyByteBuf, IgnitePayload> STREAM_CODEC =
		StreamCodec.unit(new IgnitePayload());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
