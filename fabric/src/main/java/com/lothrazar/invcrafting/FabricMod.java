package com.lothrazar.invcrafting;

import com.lothrazar.invcrafting.inventory.FabricInventoryHooks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class FabricMod implements ModInitializer {

  @Override
  public void onInitialize() {
    ServerPlayerEvents.JOIN.register(FabricInventoryHooks::updatePlayer);
    ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> FabricInventoryHooks.updatePlayer(newPlayer));
    ServerTickEvents.END_SERVER_TICK.register(server -> {
      for (ServerLevel level : server.getAllLevels()) {
        for (ServerPlayer player : level.players()) {
          FabricInventoryHooks.updatePlayer(player);
        }
      }
    });
  }
}
