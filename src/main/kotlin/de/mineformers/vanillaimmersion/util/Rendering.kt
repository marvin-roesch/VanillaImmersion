package de.mineformers.vanillaimmersion.util

import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.util.Timer
import net.minecraft.util.math.Vec3d
import net.minecraftforge.fml.common.SidedProxy
import net.minecraftforge.fml.relauncher.ReflectionHelper

/**
 * Utilities dealing with rendering.
 * Uses proxies to provide sensitive server side defaults.
 */
object Rendering : RenderingProxy by RenderingProxy.proxy

internal interface RenderingProxy {
    companion object {
        @SidedProxy
        internal lateinit var proxy: RenderingProxy

        class ClientProxy : RenderingProxy {
            /**
             * Property holding the current partial ticks.
             */
            override val partialTicks: Float = Minecraft.getMinecraft().renderPartialTicks

            override fun getEyePosition(entity: Entity, partialTicks: Float): Vec3d =
                entity.getPositionEyes(partialTicks)
        }

        class ServerProxy : RenderingProxy {
            /**
             * Property holding the current partial ticks.
             */
            override val partialTicks: Float
                get() = 1f

            override fun getEyePosition(entity: Entity, partialTicks: Float): Vec3d {
                return entity.positionVector + Vec3d(.0, entity.eyeHeight.toDouble(), .0)
            }
        }
    }

    /**
     * Property holding the current partial ticks.
     */
    val partialTicks: Float
        get

    /**
     * Gets the eye position of an entity in a safe manner.
     */
    fun getEyePosition(entity: Entity, partialTicks: Float): Vec3d
}