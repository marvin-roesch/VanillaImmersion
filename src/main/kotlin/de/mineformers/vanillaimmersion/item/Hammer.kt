package de.mineformers.vanillaimmersion.item

import de.mineformers.vanillaimmersion.VanillaImmersion.MODID
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemTool
import net.minecraft.util.ResourceLocation

/**
 * The Hammer serves as tool for editing objects on the anvil.
 */
open class Hammer : ItemTool(1f, -2.4f, ToolMaterial.IRON, emptySet()) {
    init {
        creativeTab = CreativeTabs.TOOLS
        unlocalizedName = "$MODID.hammer"
        registryName = ResourceLocation(MODID, "hammer")
    }
}