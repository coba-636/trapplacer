package com.trapplacer.item;

import java.util.function.Consumer;

import com.trapplacer.Config;
import com.trapplacer.ModContent;
import com.trapplacer.PreviewTicker;
import com.trapplacer.TrapPlacing;
import com.trapplacer.trap.TrapType;
import com.trapplacer.trap.Traps;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The Trap Placer wand. Right-click the ground to build the selected trap there,
 * pointing the way you're looking; sneak + right-click to pick the next trap.
 */
public class TrapPlacerItem extends Item {
	public TrapPlacerItem(Properties properties) {
		super(properties);
	}

	public static TrapType selected(ItemStack stack) {
		String id = stack.get(ModContent.SELECTED_TRAP);
		return id == null ? Traps.ALL.getFirst() : Traps.byId(id).orElse(Traps.ALL.getFirst());
	}

	public static void select(ItemStack stack, TrapType trap) {
		stack.set(ModContent.SELECTED_TRAP, trap.id());
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack stack = player.getItemInHand(hand);

		if (player.isShiftKeyDown()) {
			TrapType next = Traps.next(selected(stack));
			select(stack, next);
			player.displayClientMessage(Component.translatable("message.trapplacer.selected", next.name().copy().withStyle(ChatFormatting.GOLD)), true);
			PreviewTicker.hold(serverPlayer);
			level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
			return InteractionResult.SUCCESS;
		}

		HitResult hit = player.pick(Config.maxReach, 1.0F, false);
		PreviewTicker.hold(serverPlayer);
		if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
			player.displayClientMessage(Component.translatable("message.trapplacer.no_target"), true);
			return InteractionResult.FAIL;
		}

		TrapType trap = selected(stack);
		Component error = TrapPlacing.place(serverLevel, serverPlayer, player.getUUID(), trap, blockHit.getBlockPos(), player.getDirection(), false);
		if (error != null) {
			player.displayClientMessage(error.copy().withStyle(ChatFormatting.RED), true);
			return InteractionResult.FAIL;
		}
		player.displayClientMessage(Component.translatable("message.trapplacer.building", trap.name()), true);
		return InteractionResult.SUCCESS;
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable("item.trapplacer.trap_placer.with_trap", super.getName(stack), selected(stack).name());
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		TrapType trap = selected(stack);
		tooltip.accept(trap.description().copy().withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("tooltip.trapplacer.trigger", trap.trigger()).withStyle(ChatFormatting.DARK_AQUA));
		tooltip.accept(Component.translatable("tooltip.trapplacer.controls").withStyle(ChatFormatting.DARK_GRAY));
	}
}
