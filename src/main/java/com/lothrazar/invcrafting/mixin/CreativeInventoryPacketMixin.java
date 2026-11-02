package com.lothrazar.invcrafting.mixin;

import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class CreativeInventoryPacketMixin {

  @Shadow public ServerPlayer player;

  @Inject(method = "handleSetCreativeModeSlot", at = @At("TAIL"))
  private void invcrafting$clearExtraCraftingInput(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo ci) {
    // Vanilla has already enforced the game thread. Creative's destroy-all
    // action clears every menu slot, but vanilla accepts only IDs 1..45.
    // Accept only empty stacks for appended inputs, leaving all other packet
    // validation and the handling of other mods' extra slots unchanged.
    if (!this.player.hasInfiniteMaterials() || !packet.itemStack().isEmpty()) {
      return;
    }
    InventoryMenu menu = this.player.inventoryMenu;
    int slotId = packet.slotNum();
    if (slotId <= InventoryMenu.SHIELD_SLOT || slotId >= menu.slots.size()) {
      return;
    }
    Slot slot = menu.getSlot(slotId);
    if (slot.container != menu.getCraftSlots()) {
      return;
    }
    slot.setByPlayer(ItemStack.EMPTY);
    menu.setRemoteSlot(slotId, ItemStack.EMPTY);
    menu.broadcastChanges();
  }
}
