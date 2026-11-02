package com.lothrazar.invcrafting.inventory;

import com.lothrazar.invcrafting.mixin.PlayerInventoryAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.GameType;

public final class FabricInventoryHooks {

  private FabricInventoryHooks() {}

  public static void updatePlayer(Player player) {
    if (!(player.getInventory() instanceof InventoryPlayerCrafting)) {
      InventoryPlayerCrafting inventory = new InventoryPlayerCrafting(player);
      for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
        inventory.setItem(i, player.getInventory().getItem(i));
      }
      inventory.setSelectedSlot(player.getInventory().getSelectedSlot());
      PlayerInventoryAccessor accessor = (PlayerInventoryAccessor) player;
      accessor.invcrafting$setInventory(inventory);
      installMenu(player, accessor, player.isCreative() ? GameType.CREATIVE : GameType.SURVIVAL);
      player.containerMenu = accessor.invcrafting$getInventoryMenu();
    }
    else {
      installMenu(player, (PlayerInventoryAccessor) player, player.isCreative() ? GameType.CREATIVE : GameType.SURVIVAL);
    }
  }

  private static void installMenu(Player player, PlayerInventoryAccessor accessor, GameType gameMode) {
    if (!(player.getInventory() instanceof InventoryPlayerCrafting inventory)) {
      return;
    }

    boolean creative = gameMode == GameType.CREATIVE;
    boolean hasCustomMenu = accessor.invcrafting$getInventoryMenu() instanceof ContainerPlayerCrafting;
    boolean localWorld = !player.level().isClientSide();
    if (creative && hasCustomMenu) {
      InventoryMenu vanillaMenu = new InventoryMenu(inventory, localWorld, player);
      accessor.invcrafting$setInventoryMenu(vanillaMenu);
      if (player.containerMenu == null || player.containerMenu instanceof ContainerPlayerCrafting) {
        player.containerMenu = vanillaMenu;
      }
    }
    else if (!creative && !hasCustomMenu) {
      ContainerPlayerCrafting customMenu = new ContainerPlayerCrafting(inventory, localWorld, player);
      accessor.invcrafting$setInventoryMenu(customMenu);
      if (player.containerMenu == null || player.containerMenu.getClass() == InventoryMenu.class) {
        player.containerMenu = customMenu;
      }
    }
  }
}
