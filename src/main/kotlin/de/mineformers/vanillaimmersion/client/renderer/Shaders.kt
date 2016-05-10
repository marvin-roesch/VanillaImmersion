package de.mineformers.vanillaimmersion.client.renderer

import net.minecraft.util.ResourceLocation

/**
 * ${JDOC}
 */
object Shaders {
    final val ALPHA by lazy {
        Shader(null, ResourceLocation("vimmersion", "alpha"))
    }
    final val EMBERS by lazy {
        Shader(null, ResourceLocation("vimmersion", "embers"))
    }

    internal fun init() {
        ALPHA.init()
        EMBERS.init()
    }
}