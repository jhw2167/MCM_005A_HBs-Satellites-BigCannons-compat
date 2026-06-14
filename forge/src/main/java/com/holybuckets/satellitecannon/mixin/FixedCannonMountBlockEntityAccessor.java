package com.holybuckets.satellitecannon.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import rbasamoyai.createbigcannons.cannon_control.fixed_cannon_mount.FixedCannonMountBlockEntity;

/**
 * Mixin accessor that widens FixedCannonMountBlockEntity#cannonYaw and #cannonPitch.
 *
 * Why this and not an access transformer (accesstransformer.cfg)?
 * ForgeGradle's `accessTransformer` configuration is only applied to Minecraft/Forge
 * classes during compilation, not to mod-dependency jars (Create Big Cannons in this case).
 * The AT would succeed at runtime via the classloader but the source still fails to compile
 * with "cannonYaw has private access". A Mixin Accessor is processed by the Mixin annotation
 * processor and generates a synthetic interface contract that the compiler accepts, while
 * Mixin itself rewrites the bytecode at load time on both dev and prod classpaths.
 */
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
