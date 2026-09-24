package com.limer.createtree.net;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.limer.createtree.CreateTreeMod;
import com.limer.createtree.config.Category;
import com.limer.createtree.config.SkillEntry;
import com.limer.createtree.config.SkillTrees;

public record TreeSyncPayload(int expPerPoint, List<SkillEntry> entries) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<TreeSyncPayload> TYPE =
		new CustomPacketPayload.Type<>(CreateTreeMod.id("tree_sync"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TreeSyncPayload> STREAM_CODEC =
		new StreamCodec<>() {
			@Override
			public TreeSyncPayload decode(RegistryFriendlyByteBuf buf) {
				int expPerPoint = buf.readVarInt();
				int count = buf.readVarInt();
				List<SkillEntry> list = new ArrayList<>(count);
				for (int i = 0; i < count; i++) {
					ResourceLocation item = ResourceLocation.STREAM_CODEC.decode(buf);
					Category category = Category.values()[buf.readVarInt()];
					int cost = buf.readVarInt();
					int exp = buf.readVarInt();
					String branch = buf.readUtf();
					int uCount = buf.readVarInt();
					List<ResourceLocation> unlocks = new ArrayList<>(uCount);
					for (int u = 0; u < uCount; u++)
						unlocks.add(ResourceLocation.STREAM_CODEC.decode(buf));
					list.add(new SkillEntry(item, category, cost, exp, branch, unlocks));
				}
				return new TreeSyncPayload(expPerPoint, list);
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buf, TreeSyncPayload payload) {
				buf.writeVarInt(payload.expPerPoint());
				buf.writeVarInt(payload.entries().size());
				for (SkillEntry e : payload.entries()) {
					ResourceLocation.STREAM_CODEC.encode(buf, e.item());
					buf.writeVarInt(e.category().ordinal());
					buf.writeVarInt(e.cost());
					buf.writeVarInt(e.exp());
					buf.writeUtf(e.branch());
					buf.writeVarInt(e.unlocks().size());
					for (ResourceLocation u : e.unlocks())
						ResourceLocation.STREAM_CODEC.encode(buf, u);
				}
			}
		};

	public static TreeSyncPayload current() {
		var tree = SkillTrees.get();

		return new TreeSyncPayload(tree.expPerPoint(), tree.nodes());
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
