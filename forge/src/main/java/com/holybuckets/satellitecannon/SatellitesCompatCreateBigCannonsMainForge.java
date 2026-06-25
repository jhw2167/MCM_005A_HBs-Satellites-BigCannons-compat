package com.holybuckets.satellitecannon;

import net.blay09.mods.balm.api.Balm;
import net.minecraftforge.fml.common.Mod;

@Mod( Constants.MOD_ID)
public class SatellitesCompatCreateBigCannonsMainForge {

    public SatellitesCompatCreateBigCannonsMainForge() {
        super();
        Balm.initialize(Constants.MOD_ID, CommonClass::init);
    }


}
