package com.lothrazar.invcrafting.inventory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;

/** A crafting input that the creative inventory's slot wrappers must not display. */
public final class ExpandedCraftingSlot extends Slot {

  private final Player owner;

  public ExpandedCraftingSlot(CraftingContainer grid, int gridIndex, Player owner) {
    super(grid, gridIndex, 82 + gridIndex % 3 * 18, 8 + gridIndex / 3 * 18);
    this.owner = owner;
  }

  @Override
  public boolean isActive() {
    return !owner.isCreative();
  }
}
