package com.holybuckets.satellitecannon.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rbasamoyai.createbigcannons.munitions.big_cannon.he_shell.HEShellProjectile;

@Mixin(Entity.class)
public class HEShellProjectileMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void logTickData(CallbackInfo ci) {
        if( !((Object)this instanceof HEShellProjectile)) {
            return;
        }
        HEShellProjectile self = (HEShellProjectile)(Object)this;
        Vec3 pos = self.position();
        Vec3 deltaMovement = self.getDeltaMovement();

        System.out.printf("Tick: pos=(%.6f, %.6f, %.6f) velocity=(%.6f, %.6f, %.6f)%n",
            pos.x, pos.y, pos.z,
            deltaMovement.x, deltaMovement.y, deltaMovement.z);
    }
}