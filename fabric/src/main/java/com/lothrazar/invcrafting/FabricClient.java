package com.lothrazar.invcrafting;

import com.lothrazar.invcrafting.inventory.FabricInventoryHooks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class FabricClient implements ClientModInitializer {

  @Override
  public void onInitializeClient() {
    ClientTickEvents.END_CLIENT_TICK.register(client -> {
      if (client.player != null) {
        FabricInventoryHooks.updatePlayer(client.player);
      }
    });
  }
}
