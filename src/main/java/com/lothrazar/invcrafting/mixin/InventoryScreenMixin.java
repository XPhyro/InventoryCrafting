package com.lothrazar.invcrafting.mixin;

import com.lothrazar.invcrafting.ModConstants;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Changes the background without replacing the screen or its modded widgets. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

  @Unique private static final Identifier INVCRAFTING_BACKGROUND = Identifier.fromNamespaceAndPath(
      ModConstants.MODID, "textures/gui/inventorycraft.png");

  @Redirect(method = "extractBackground", at = @At(value = "FIELD",
      target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;INVENTORY_LOCATION:Lnet/minecraft/resources/Identifier;"))
  private Identifier invcrafting$background() {
    return INVCRAFTING_BACKGROUND;
  }

  @ModifyArg(method = "extractLabels", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V"), index = 1)
  private Component invcrafting$hideOverlappingTitle(Component original) {
    return Component.empty();
  }
}
