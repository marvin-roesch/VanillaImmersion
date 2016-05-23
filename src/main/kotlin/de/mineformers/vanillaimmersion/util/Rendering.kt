package de.mineformers.vanillaimmersion.util

import net.minecraft.client.Minecraft
import net.minecraft.util.Timer
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
             * Holds a reference to the Minecraft timer, required since we need access to partial ticks in places where
             * you usually don't have it.
             */
            private val TIMER_FIELD by lazy {
                ReflectionHelper.findField(Minecraft::class.java, "field_71428_T", "timer")
            }

            init {
                TIMER_FIELD.isAccessible = true
            }

            /**
             * Property holding the current partial ticks.
             */
            override val partialTicks: Float
                get() = (TIMER_FIELD.get(Minecraft.getMinecraft()) as Timer).renderPartialTicks
        }

        class ServerProxy : RenderingProxy {
            /**
             * Property holding the current partial ticks.
             */
            override val partialTicks: Float
                get() = 1f
        }
    }

    /**
     * Property holding the current partial ticks.
     */
    val partialTicks: Float
        get
}