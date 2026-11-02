package com.lothrazar.invcrafting.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Player.class)
public interface PlayerInventoryAccessor {

  @Mutable
  @Accessor("inventory")
  void invcrafting$setInventory(Inventory inventory);

  @Accessor("inventoryMenu")
  InventoryMenu invcrafting$getInventoryMenu();

  @Mutable
  @Accessor("inventoryMenu")
  void invcrafting$setInventoryMenu(InventoryMenu inventoryMenu);
}
