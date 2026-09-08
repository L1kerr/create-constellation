package com.limer.createtree.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.limer.createtree.CreateTreeMod;

/**
 * Server -> client: player's progression state (unlocks, exp, points).
 */
public record ProgressSyncPayload(List<ResourceLocation> unlocked, int exp, int points, int expPerPoint)
	implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<ProgressSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(CreateTreeMod.id("progress_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, ProgressSyncPayload> STREAM_CODEC =
		new StreamCodec<>() {
			@Override
			public ProgressSyncPayload decode(RegistryFriendlyByteBuf buf) {
				int count = buf.readVarInt();
				List<ResourceLocation> list = new ArrayList<>(count);
				for (int i = 0; i < count; i++)
					list.add(ResourceLocation.STREAM_CODEC.decode(buf));
				return new ProgressSyncPayload(list, buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buf, ProgressSyncPayload payload) {
				buf.writeVarInt(payload.unlocked().size());
				for (ResourceLocation rl : payload.unlocked())
					ResourceLocation.STREAM_CODEC.encode(buf, rl);
				buf.writeVarInt(payload.exp());
				buf.writeVarInt(payload.points());
				buf.writeVarInt(payload.expPerPoint());
			}
		};

	public ProgressSyncPayload(com.limer.createtree.data.PlayerProgress progress) {
		this(new ArrayList<>(progress.unlocked()), progress.exp(), progress.points(),
			com.limer.createtree.config.SkillTrees.get().expPerPoint());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
