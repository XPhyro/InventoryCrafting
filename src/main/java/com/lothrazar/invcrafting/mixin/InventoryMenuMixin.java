package com.lothrazar.invcrafting.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.lothrazar.invcrafting.inventory.ExpandedCraftingSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Expands the existing menu without changing any vanilla inventory slot IDs. */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractCraftingMenu {

  @Shadow @Final private Player owner;

  @Unique private List<Slot> invcrafting$inputs;

  protected InventoryMenuMixin(MenuType<?> type, int id, int width, int height) {
    super(type, id, width, height);
  }

  @ModifyArgs(method = "<init>", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/inventory/AbstractCraftingMenu;<init>(Lnet/minecraft/world/inventory/MenuType;III)V"))
  private static void invcrafting$expandGrid(Args args) {
    // Initialise the actual inherited fields: recipe placement and menu cleanup
    // access these directly, rather than going through the public grid getters.
    args.set(2, 3);
    args.set(3, 3);
  }

  @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/inventory/InventoryMenu;addResultSlot(Lnet/minecraft/world/entity/player/Player;II)Lnet/minecraft/world/inventory/Slot;"), index = 2)
  private int invcrafting$resultY(int originalY) {
    return 24;
  }

  @Redirect(method = "<init>", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/inventory/InventoryMenu;addCraftingGridSlots(II)V"))
  private void invcrafting$addOriginalInputs(InventoryMenu menu, int left, int top) {
    invcrafting$inputs = new ArrayList<>(Collections.nCopies(9, null));
    // Keep the original 2x2 inputs at menu slots 1..4. Armour, storage,
    // hotbar, and off-hand therefore remain at their vanilla IDs 5..45.
    for (int gridIndex : new int[] { 0, 1, 3, 4 }) {
      invcrafting$addInput(gridIndex);
    }
  }

  @Inject(method = "<init>", at = @At("TAIL"))
  private void invcrafting$appendInputs(Inventory inventory, boolean active, Player player, CallbackInfo ci) {
    for (int gridIndex : new int[] { 2, 5, 6, 7, 8 }) {
      invcrafting$addInput(gridIndex);
    }
    invcrafting$inputs = List.copyOf(invcrafting$inputs);
  }

  @Unique
  private void invcrafting$addInput(int gridIndex) {
    Slot slot = this.addSlot(new ExpandedCraftingSlot(this.craftSlots, gridIndex, this.owner));
    invcrafting$inputs.set(gridIndex, slot);
  }

  @Inject(method = "getInputGridSlots", at = @At("HEAD"), cancellable = true)
  private void invcrafting$getInputs(CallbackInfoReturnable<List<Slot>> cir) {
    // Recipe books and transfer mods require row-major order, not menu order.
    cir.setReturnValue(invcrafting$inputs);
  }

  @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
  private void invcrafting$quickMoveExtraInput(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
    Slot slot = this.slots.get(slotIndex);
    if (slotIndex < InventoryMenu.CRAFT_SLOT_END || slot.container != this.craftSlots) {
      return;
    }

    // Vanilla handles slots 0..45. The appended inputs must take the same path
    // as its original inputs, including when the ingredient is equippable.
    cir.setReturnValue(ItemStack.EMPTY);
    if (!slot.hasItem() || !slot.isActive() || !slot.mayPickup(player)) {
      return;
    }
    ItemStack stack = slot.getItem();
    ItemStack original = stack.copy();
    if (!this.moveItemStackTo(stack, InventoryMenu.INV_SLOT_START, InventoryMenu.USE_ROW_SLOT_END, false)) {
      return;
    }
    if (stack.isEmpty()) {
      slot.setByPlayer(ItemStack.EMPTY, original);
    }
    else {
      slot.setChanged();
    }
    if (stack.getCount() != original.getCount()) {
      slot.onTake(player, stack);
      cir.setReturnValue(original);
    }
  }
}
