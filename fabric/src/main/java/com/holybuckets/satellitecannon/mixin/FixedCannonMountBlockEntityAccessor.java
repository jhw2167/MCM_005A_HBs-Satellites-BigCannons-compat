package com.holybuckets.satellitecannon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rbasamoyai.createbigcannons.cannon_control.fixed_cannon_mount.FixedCannonMountBlockEntity;

@Mixin(FixedCannonMountBlockEntity.class)
public interface FixedCannonMountBlockEntityAccessor {

    @Accessor("cannonYaw")
    float getCannonYaw();

    @Accessor("cannonYaw")
    void setCannonYaw(float value);

    @Accessor("cannonPitch")
    float getCannonPitch();

    @Accessor("cannonPitch")
    void setCannonPitch(float value);
}
