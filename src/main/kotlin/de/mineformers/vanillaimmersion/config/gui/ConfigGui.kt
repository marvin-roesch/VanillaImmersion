package de.mineformers.vanillaimmersion.config.gui

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.config.ConfigEntry.Category.Companion.fromAnyRef
import de.mineformers.vanillaimmersion.config.ConfigStorage
import de.mineformers.vanillaimmersion.config.Configuration
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.config.GuiConfig
import net.minecraftforge.fml.client.config.IConfigElement
import net.minecraftforge.fml.client.event.ConfigChangedEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Vanilla Immersion's Configuration interface.
 */
class ConfigGui(parent: GuiScreen) :
    GuiConfig(parent, buildElements(), VanillaImmersion.MODID, false, false, VanillaImmersion.MOD_NAME) {

    /**
     * Holds onto the individual config entries and their values.
     */
    companion object : ConfigStorage {
        private val configValues = mutableMapOf<String, Any?>()

        init {
            MinecraftForge.EVENT_BUS.register(this)
        }

        override fun get(path: String) =
            configValues.getOrPut(path) {
                Configuration.getAnyRef(path)
            }

        override fun set(path: String, value: Any?) {
            configValues[path] = value
        }

        /**
         * Builds a list of elements for the configuration interface.
         */
        private fun buildElements(): List<IConfigElement> {
            configValues.clear()
            return Configuration.TEMPLATE.elements.map {
                ConfigElement(it.name, it, this)
            }
        }

        @SubscribeEvent
        fun onChanged(event: ConfigChangedEvent.OnConfigChangedEvent) {
            // When our config was changed, save it
            if (event.modID == VanillaImmersion.MODID) {
                for ((path, value) in configValues)
                    Configuration.update(
                        Configuration.withValue(path, fromAnyRef(value, Configuration.TEMPLATE[path]!!.comment)))
                Configuration.save()
            }
        }
    }
}