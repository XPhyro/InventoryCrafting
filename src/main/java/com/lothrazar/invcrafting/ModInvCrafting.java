package com.lothrazar.invcrafting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ModInvCrafting.MODID)
public class ModInvCrafting {

  public static final Logger LOGGER = LogManager.getLogger();
  public static final String MODID = ModConstants.MODID;

  public ModInvCrafting(IEventBus modEventBus) {
    // The shared mixins expand the original inventory menu at construction.
  }
}
