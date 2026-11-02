package com.lothrazar.invcrafting.mixin;

import com.lothrazar.invcrafting.inventory.GuiInventoryCrafting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public abstract class MinecraftScreenMixin {

  @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
  private Screen invcrafting$replaceInventoryScreen(Screen screen) {
    Minecraft minecraft = Minecraft.getInstance();
    if (screen != null && screen.getClass() == InventoryScreen.class && minecraft.player != null && !minecraft.player.isCreative()) {
      return new GuiInventoryCrafting(minecraft.player);
    }
    return screen;
  }
}
