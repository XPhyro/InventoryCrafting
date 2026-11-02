package com.lothrazar.invcrafting;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.Identifier;

@JeiPlugin
public class JEIPlugin implements IModPlugin {

  private static final Identifier ID = Identifier.fromNamespaceAndPath(ModConstants.MODID, "player");

  @Override
  public void registerRecipeTransferHandlers(IRecipeTransferRegistration registry) {
    // JEI registers its vanilla plugin first; this replaces the 2x2 handler
    // for the same InventoryMenu/Crafting pair with the expanded slot mapping.
    registry.addRecipeTransferHandler(new Transfer());
  }

  @Override
  public Identifier getPluginUid() {
    return ID;
  }
}
