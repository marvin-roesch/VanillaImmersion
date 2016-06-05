package de.mineformers.vanillaimmersion.config.gui

import net.minecraft.client.Minecraft
import net.minecraftforge.fml.client.IModGuiFactory

/**
 * Factory for Vanilla Immersion's configuration interface.
 */
class GuiFactory : IModGuiFactory {
    override fun initialize(minecraftInstance: Minecraft?) = Unit

    override fun mainConfigGuiClass() = ConfigGui::class.java

    override fun runtimeGuiCategories() = null

    override fun getHandlerFor(element: IModGuiFactory.RuntimeOptionCategoryElement?) = null
}