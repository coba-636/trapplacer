package com.trapplacer.command;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.trapplacer.ModContent;
import com.trapplacer.TrapPlacing;
import com.trapplacer.build.BuildManager;
import com.trapplacer.build.UndoHistory;
import com.trapplacer.item.TrapPlacerItem;
import com.trapplacer.trap.TrapType;
import com.trapplacer.trap.Traps;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** {@code /trap list|info|select|undo|give|place}. */
public final class TrapCommand {
	private static final DynamicCommandExceptionType UNKNOWN_TRAP = new DynamicCommandExceptionType(
		id -> Component.translatable("command.trapplacer.unknown", id));
	private static final DynamicCommandExceptionType BAD_FACING = new DynamicCommandExceptionType(
		value -> Component.translatable("command.trapplacer.bad_facing", value));
	private static final SuggestionProvider<CommandSourceStack> TRAP_IDS = (context, builder) ->
		SharedSuggestionProvider.suggest(Traps.ALL.stream().map(TrapType::id), builder);
	private static final SuggestionProvider<CommandSourceStack> FACINGS = (context, builder) ->
		SharedSuggestionProvider.suggest(new String[] {"north", "east", "south", "west"}, builder);

	private TrapCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("trap")
			.then(Commands.literal("list").executes(TrapCommand::list))
			.then(Commands.literal("info")
				.then(Commands.argument("trap", StringArgumentType.word()).suggests(TRAP_IDS)
					.executes(context -> info(context, trap(context)))))
			.then(Commands.literal("select")
				.then(Commands.argument("trap", StringArgumentType.word()).suggests(TRAP_IDS)
					.executes(context -> select(context, trap(context)))))
			.then(Commands.literal("undo").executes(TrapCommand::undo))
			.then(Commands.literal("give")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(context -> give(context, java.util.List.of(context.getSource().getPlayerOrException())))
				.then(Commands.argument("targets", EntityArgument.players())
					.executes(context -> give(context, EntityArgument.getPlayers(context, "targets")))))
			.then(Commands.literal("place")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("trap", StringArgumentType.word()).suggests(TRAP_IDS)
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(context -> place(context, defaultFacing(context.getSource())))
						.then(Commands.argument("facing", StringArgumentType.word()).suggests(FACINGS)
							.executes(context -> place(context, facing(context))))))));
	}

	private static TrapType trap(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		String id = StringArgumentType.getString(context, "trap");
		return Traps.byId(id).orElseThrow(() -> UNKNOWN_TRAP.create(id));
	}

	private static Direction facing(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		String value = StringArgumentType.getString(context, "facing");
		Direction direction = Direction.byName(value);
		if (direction == null || !direction.getAxis().isHorizontal()) {
			throw BAD_FACING.create(value);
		}
		return direction;
	}

	private static Direction defaultFacing(CommandSourceStack source) {
		Entity entity = source.getEntity();
		return entity != null ? entity.getDirection() : Direction.NORTH;
	}

	private static UUID ownerOf(CommandSourceStack source) {
		Entity entity = source.getEntity();
		return entity != null ? entity.getUUID() : Util.NIL_UUID;
	}

	private static int list(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		source.sendSuccess(() -> Component.translatable("command.trapplacer.list.header").withStyle(ChatFormatting.GOLD), false);
		for (TrapType trap : Traps.ALL) {
			source.sendSuccess(() -> Component.literal(" - ").append(trap.name().copy().withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(" (" + trap.id() + "): ").withStyle(ChatFormatting.DARK_GRAY))
				.append(trap.description()), false);
		}
		return Traps.ALL.size();
	}

	private static int info(CommandContext<CommandSourceStack> context, TrapType trap) {
		CommandSourceStack source = context.getSource();
		source.sendSuccess(() -> trap.name().copy().withStyle(ChatFormatting.GOLD), false);
		source.sendSuccess(trap::description, false);
		source.sendSuccess(() -> Component.translatable("tooltip.trapplacer.trigger", trap.trigger()).withStyle(ChatFormatting.DARK_AQUA), false);

		MutableComponent cost = Component.empty();
		boolean first = true;
		for (Map.Entry<Item, Integer> entry : trap.cost().entrySet()) {
			if (!first) {
				cost.append(", ");
			}
			cost.append(entry.getValue() + "x ").append(entry.getKey().getName());
			first = false;
		}
		source.sendSuccess(() -> Component.translatable("command.trapplacer.info.cost", cost).withStyle(ChatFormatting.GRAY), false);
		return 1;
	}

	private static int select(CommandContext<CommandSourceStack> context, TrapType trap) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ItemStack stack = player.getMainHandItem().is(ModContent.TRAP_PLACER) ? player.getMainHandItem() : player.getOffhandItem();
		if (!stack.is(ModContent.TRAP_PLACER)) {
			context.getSource().sendFailure(Component.translatable("command.trapplacer.no_wand"));
			return 0;
		}
		TrapPlacerItem.select(stack, trap);
		context.getSource().sendSuccess(() -> Component.translatable("message.trapplacer.selected", trap.name()), false);
		return 1;
	}

	private static int undo(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		UUID owner = ownerOf(source);
		BuildManager.finishNow(owner);
		UndoHistory.Entry entry = BuildManager.undoHistory().undo(source.getServer(), owner);
		if (entry == null) {
			source.sendFailure(Component.translatable("command.trapplacer.undo.none"));
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("command.trapplacer.undo.done", entry.trap().name()), true);
		return 1;
	}

	private static int give(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			ItemStack stack = new ItemStack(ModContent.TRAP_PLACER);
			if (!player.getInventory().add(stack)) {
				player.drop(stack, false);
			}
		}
		context.getSource().sendSuccess(() -> Component.translatable("command.trapplacer.give", players.size()), true);
		return players.size();
	}

	private static int place(CommandContext<CommandSourceStack> context, Direction facing) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		TrapType trap = trap(context);
		BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
		Component error = TrapPlacing.place(source.getLevel(), source.getPlayer(), ownerOf(source), trap, pos, facing, true);
		if (error != null) {
			source.sendFailure(error);
			return 0;
		}
		source.sendSuccess(() -> Component.translatable("command.trapplacer.placed", trap.name(), pos.getX(), pos.getY(), pos.getZ(), facing.getName()), true);
		return 1;
	}
}
