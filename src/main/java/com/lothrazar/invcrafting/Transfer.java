package com.lothrazar.invcrafting;

import java.util.List;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public class Transfer implements IRecipeTransferInfo<InventoryMenu, RecipeHolder<CraftingRecipe>> {

  @Override
  public boolean canHandle(InventoryMenu container, RecipeHolder<CraftingRecipe> recipe) {
    return container.getGridWidth() == 3 && container.getGridHeight() == 3
        && container.getInputGridSlots().size() == 9
        && container.getInputGridSlots().stream().allMatch(Slot::isActive);
  }

  @Override
  public Class<InventoryMenu> getContainerClass() {
    return InventoryMenu.class;
  }

  @Override
  public Optional<MenuType<InventoryMenu>> getMenuType() {
    return Optional.empty();
  }

  @Override
  public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
    return RecipeTypes.CRAFTING;
  }

  @Override
  public List<Slot> getInventorySlots(InventoryMenu container, RecipeHolder<CraftingRecipe> recipe) {
    // Only main inventory and hotbar are ingredient sources, never equipment
    // or crafting inputs appended by this mod or another mod.
    return List.copyOf(container.slots.subList(InventoryMenu.INV_SLOT_START, InventoryMenu.USE_ROW_SLOT_END));
  }

  @Override
  public List<Slot> getRecipeSlots(InventoryMenu container, RecipeHolder<CraftingRecipe> recipe) {
    // The expanded grid is deliberately non-contiguous in menu-slot order.
    return container.getInputGridSlots();
  }
}
