package com.limer.createtree.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.limer.createtree.CreateTreeMod;

public record ProgressSyncPayload(List<ResourceLocation> unlocked, int exp, int points, int expPerPoint,
								  boolean sunIgnited, ResourceLocation contractItem, int contractTarget,
								  int contractProgress, int contractReward)
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
				int exp = buf.readVarInt();
				int points = buf.readVarInt();
				int epp = buf.readVarInt();
				boolean ignited = buf.readBoolean();
				boolean hasContract = buf.readBoolean();
				ResourceLocation item = null;
				int target = 0, prog = 0, reward = 0;
				if (hasContract) {
					item = ResourceLocation.STREAM_CODEC.decode(buf);
					target = buf.readVarInt();
					prog = buf.readVarInt();
					reward = buf.readVarInt();
				}
				return new ProgressSyncPayload(list, exp, points, epp, ignited, item, target, prog, reward);
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buf, ProgressSyncPayload payload) {
				buf.writeVarInt(payload.unlocked().size());
				for (ResourceLocation rl : payload.unlocked())
					ResourceLocation.STREAM_CODEC.encode(buf, rl);
				buf.writeVarInt(payload.exp());
				buf.writeVarInt(payload.points());
				buf.writeVarInt(payload.expPerPoint());
				buf.writeBoolean(payload.sunIgnited());
				buf.writeBoolean(payload.contractItem() != null);
				if (payload.contractItem() != null) {
					ResourceLocation.STREAM_CODEC.encode(buf, payload.contractItem());
					buf.writeVarInt(payload.contractTarget());
					buf.writeVarInt(payload.contractProgress());
					buf.writeVarInt(payload.contractReward());
				}
			}
		};

	public ProgressSyncPayload(com.limer.createtree.data.PlayerProgress progress) {
		this(new ArrayList<>(progress.unlocked()), progress.exp(), progress.points(),
			com.limer.createtree.config.SkillTrees.get().expPerPoint(), progress.sunIgnited(),
			progress.contract().isEmpty() ? null : progress.contract().item(),
			progress.contract().target(), progress.contract().progress(), progress.contract().reward());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
