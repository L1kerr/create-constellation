package com.limer.createtree.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.limer.createtree.CreateTreeMod;

public record EventSyncPayload(int eventType, int secondsLeft, String branch,
							   List<ResourceLocation> discountItems, int[] discountPrices)
	implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<EventSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(CreateTreeMod.id("event_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, EventSyncPayload> STREAM_CODEC =
		new StreamCodec<>() {
			@Override
			public EventSyncPayload decode(RegistryFriendlyByteBuf buf) {
				int eventType = buf.readVarInt();
				int seconds = buf.readVarInt();
				String branch = buf.readUtf();
				int n = buf.readVarInt();
				List<ResourceLocation> items = new ArrayList<>(n);
				int[] prices = new int[n];
				for (int i = 0; i < n; i++) {
					items.add(ResourceLocation.STREAM_CODEC.decode(buf));
					prices[i] = buf.readVarInt();
				}
				return new EventSyncPayload(eventType, seconds, branch, items, prices);
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buf, EventSyncPayload p) {
				buf.writeVarInt(p.eventType());
				buf.writeVarInt(p.secondsLeft());
				buf.writeUtf(p.branch());
				buf.writeVarInt(p.discountItems().size());
				for (int i = 0; i < p.discountItems().size(); i++) {
					ResourceLocation.STREAM_CODEC.encode(buf, p.discountItems().get(i));
					buf.writeVarInt(p.discountPrices()[i]);
				}
			}
		};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
