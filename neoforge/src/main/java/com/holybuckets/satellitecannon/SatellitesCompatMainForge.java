package com.holybuckets.satellitecannon;

import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.satellitecannon.core.RemoteCannonWeapon;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.neoforge.NeoForgeLoadContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class SatellitesCompatMainForge {

    public SatellitesCompatMainForge(IEventBus modEventBus) {
        // NeoForge entry point delegating Balm init through the mod event bus
        final var context = new NeoForgeLoadContext(modEventBus);
        Balm.initialize(Constants.MOD_ID, context, CommonClass::init);
    }
}
