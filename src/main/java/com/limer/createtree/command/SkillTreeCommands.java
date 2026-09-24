package com.limer.createtree.command;

import java.util.Collection;
import java.util.List;

import com.limer.createtree.config.SkillEntry;
import com.limer.createtree.config.SkillTrees;
import com.limer.createtree.data.PlayerProgress;
import com.limer.createtree.data.SkillApi;
import com.limer.createtree.net.ModNetwork;
import com.limer.createtree.net.ProgressSyncPayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class SkillTreeCommands {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("cr")
			.requires(src -> src.hasPermission(2))
			.then(Commands.literal("points")
				.then(Commands.literal("add")
					.then(Commands.argument("targets", EntityArgument.players())
						.then(Commands.argument("amount", IntegerArgumentType.integer(1))
							.executes(ctx -> changePoints(ctx, 1)))))
				.then(Commands.literal("remove")
					.then(Commands.argument("targets", EntityArgument.players())
						.then(Commands.argument("amount", IntegerArgumentType.integer(1))
							.executes(ctx -> changePoints(ctx, -1)))))
				.then(Commands.literal("set")
					.then(Commands.argument("targets", EntityArgument.players())
						.then(Commands.argument("amount", IntegerArgumentType.integer(0))
							.executes(SkillTreeCommands::setPoints)))))
			.then(Commands.literal("reset")
				.then(Commands.argument("targets", EntityArgument.players())
					.executes(SkillTreeCommands::reset)))
			.then(Commands.literal("hardreset")
				.then(Commands.argument("targets", EntityArgument.players())
					.executes(SkillTreeCommands::hardReset)))
			.then(Commands.literal("unlock")
				.then(Commands.argument("targets", EntityArgument.players())
					.then(Commands.argument("item", ResourceLocationArgument.id())
						.suggests((ctx, builder) -> {
							treeItems().forEach(id -> builder.suggest(id.toString()));
							return builder.buildFuture();
						})
						.executes(SkillTreeCommands::unlock))))
			.then(Commands.literal("lock")
				.then(Commands.argument("targets", EntityArgument.players())
					.then(Commands.argument("item", ResourceLocationArgument.id())
						.suggests((ctx, builder) -> {
							treeItems().forEach(id -> builder.suggest(id.toString()));
							return builder.buildFuture();
						})
						.executes(SkillTreeCommands::lock))))
			.then(Commands.literal("sync")
				.then(Commands.argument("targets", EntityArgument.players())
					.executes(SkillTreeCommands::sync)))
			.then(Commands.literal("contract")
				.then(Commands.literal("set")
					.then(Commands.argument("targets", EntityArgument.players())
						.then(Commands.argument("item", ResourceLocationArgument.id())
							.suggests((ctx, builder) -> {
								treeItems().forEach(id -> builder.suggest(id.toString()));
								return builder.buildFuture();
							})
							.then(Commands.argument("target", IntegerArgumentType.integer(1))
								.then(Commands.argument("reward", IntegerArgumentType.integer(1))
									.executes(SkillTreeCommands::setContract))))))
				.then(Commands.literal("clear")
					.then(Commands.argument("targets", EntityArgument.players())
						.executes(SkillTreeCommands::clearContract))))
			.then(Commands.literal("event")
				.then(Commands.literal("start")
					.then(Commands.argument("type", com.mojang.brigadier.arguments.StringArgumentType.word())
						.suggests((ctx, builder) -> {
							builder.suggest("golden_planet");
							builder.suggest("discount_night");
							builder.suggest("comet");
							return builder.buildFuture();
						})
						.then(Commands.argument("branch", com.mojang.brigadier.arguments.StringArgumentType.word())
							.suggests((ctx, builder) -> {
								SkillTrees.get().all().stream().map(SkillEntry::branch).distinct()
									.forEach(builder::suggest);
								return builder.buildFuture();
							})
							.executes(ctx -> startEvent(ctx,
								com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "type"),
								com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "branch"))))
						.executes(ctx -> startEvent(ctx,
							com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "type"), null))))
				.then(Commands.literal("stop")
					.executes(SkillTreeCommands::stopEvent)))
			.executes(SkillTreeCommands::help));
	}

	private static int changePoints(CommandContext<CommandSourceStack> ctx, int sign) {
		int amount = IntegerArgumentType.getInteger(ctx, "amount");
		return forEachTarget(ctx, (source, player, progress) -> {
			progress.addPoints(amount * sign);
			syncOne(player);
			source.sendSuccess(() -> Component.translatable("commands.createtree.points.changed",
				signed(amount * sign), player.getDisplayName()), true);
		});
	}

	private static int setPoints(CommandContext<CommandSourceStack> ctx) {
		int amount = IntegerArgumentType.getInteger(ctx, "amount");
		return forEachTarget(ctx, (source, player, progress) -> {
			progress.setPoints(amount);
			syncOne(player);
			source.sendSuccess(() -> Component.translatable("commands.createtree.points.set",
				amount, player.getDisplayName()), true);
		});
	}

	private static int reset(CommandContext<CommandSourceStack> ctx) {
		return forEachTarget(ctx, (source, player, progress) -> {
			progress.softReset(SkillTrees.get().startingPoints());
			syncOne(player);
			source.sendSuccess(() -> Component.translatable("commands.createtree.reset",
				player.getDisplayName()), true);
		});
	}

	private static int hardReset(CommandContext<CommandSourceStack> ctx) {
		return forEachTarget(ctx, (source, player, progress) -> {
			progress.hardReset();
			syncOne(player);
			source.sendSuccess(() -> Component.translatable("commands.createtree.hardreset",
				player.getDisplayName()), true);
		});
	}

	private static int unlock(CommandContext<CommandSourceStack> ctx) {
		ResourceLocation item = ResourceLocationArgument.getId(ctx, "item");
		if (SkillTrees.get().entry(item) == null) {
			ctx.getSource().sendFailure(Component.translatable("commands.createtree.unknown_item", item));
			return 0;
		}
		return forEachTarget(ctx, (source, player, progress) -> {

			progress.forceUnlockAll(SkillTrees.get().groupAndSelf(item));
			syncOne(player);
			source.sendSuccess(() -> Component.translatable("commands.createtree.unlocked",
				item.toString(), player.getDisplayName()), true);
		});
	}

	private static int lock(CommandContext<CommandSourceStack> ctx) {
		ResourceLocation item = ResourceLocationArgument.getId(ctx, "item");
		return forEachTarget(ctx, (source, player, progress) -> {
			progress.forceLock(item);
			syncOne(player);
			source.sendSuccess(() -> Component.translatable("commands.createtree.locked",
				item.toString(), player.getDisplayName()), true);
		});
	}

	private static int sync(CommandContext<CommandSourceStack> ctx) {
		return forEachTarget(ctx, (source, player, progress) -> {
			ModNetwork.sendTreeTo(player);
			syncOne(player);
			source.sendSuccess(() -> Component.translatable("commands.createtree.synced",
				player.getDisplayName()), true);
		});
	}

	private static int setContract(CommandContext<CommandSourceStack> ctx) {
		ResourceLocation item = ResourceLocationArgument.getId(ctx, "item");
		if (SkillTrees.get().entry(item) == null) {
			ctx.getSource().sendFailure(Component.translatable("commands.createtree.unknown_item", item));
			return 0;
		}
		int target = IntegerArgumentType.getInteger(ctx, "target");
		int reward = IntegerArgumentType.getInteger(ctx, "reward");
		return forEachTarget(ctx, (source, player, progress) -> {
			progress.contract().set(item, target, reward);
			syncOne(player);
			source.sendSuccess(() -> Component.translatable("commands.createtree.contract_set",
				target, item.toString(), reward, player.getDisplayName()), true);
		});
	}

	private static int clearContract(CommandContext<CommandSourceStack> ctx) {
		return forEachTarget(ctx, (source, player, progress) -> {
			progress.contract().clear();
			syncOne(player);
			source.sendSuccess(() -> Component.translatable("commands.createtree.contract_cleared",
				player.getDisplayName()), true);
		});
	}

	private static int help(CommandContext<CommandSourceStack> ctx) {
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.createtree.help"), false);
		return 1;
	}

	private static int startEvent(CommandContext<CommandSourceStack> ctx, String type, String branch) {
		var server = ctx.getSource().getServer();
		if (com.limer.createtree.event.WorldEvents.adminStart(server, type, branch)) {
			ctx.getSource().sendSuccess(() -> Component.translatable("commands.createtree.event.started"), true);
			return 1;
		}
		ctx.getSource().sendFailure(Component.translatable("commands.createtree.event.failed"));
		return 0;
	}

	private static int stopEvent(CommandContext<CommandSourceStack> ctx) {
		var server = ctx.getSource().getServer();
		if (com.limer.createtree.event.WorldEvents.adminStop(server)) {
			ctx.getSource().sendSuccess(() -> Component.translatable("commands.createtree.event.stopped"), true);
			return 1;
		}
		ctx.getSource().sendFailure(Component.translatable("commands.createtree.event.failed"));
		return 0;
	}

	private interface TargetAction {
		void apply(CommandSourceStack source, ServerPlayer player, PlayerProgress progress);
	}

	private static int forEachTarget(CommandContext<CommandSourceStack> ctx, TargetAction action) {
		try {
			Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
			int n = 0;
			for (ServerPlayer player : targets) {
				action.apply(ctx.getSource(), player, SkillApi.progress(player));
				n++;
			}
			return n;
		} catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
			ctx.getSource().sendFailure(Component.literal(e.getMessage()));
			return 0;
		}
	}

	private static String signed(int v) {
		return (v > 0 ? "+" : "") + v;
	}

	private static void syncOne(ServerPlayer player) {
		ModNetwork.sendToPlayer(player, new ProgressSyncPayload(SkillApi.progress(player)));
	}

	public static List<ResourceLocation> treeItems() {
		return SkillTrees.get().all().stream().map(SkillEntry::item).toList();
	}
}
