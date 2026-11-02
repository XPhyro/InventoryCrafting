package com.lothrazar.invcrafting.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class CraftingCompatibility {

  private CraftingCompatibility() {}

  static void dropCraftingRemainder(Player player, ItemStack stack) {
    player.drop(stack, false);
  }
}
