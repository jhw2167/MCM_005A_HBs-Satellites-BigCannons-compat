package com.holybuckets.satellitecannon;

import com.holybuckets.satellitecannon.client.CommonClassClient;
import com.holybuckets.satellitecannon.client.IBewlrRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SatellitesCompatCreateBigCannonsMainForgeClient {


    public static void clientInitializeForge() {
        CommonClassClient.initClient();
    }

    private static void setBlockEntityRender(Object item, BlockEntityWithoutLevelRenderer renderer) {
        ((IBewlrRenderer) item).setBlockEntityWithoutLevelRenderer(renderer);
    }

}
