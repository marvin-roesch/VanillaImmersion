package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.tileentity.BrewingStandLogic
import de.mineformers.vanillaimmersion.tileentity.BrewingStandLogic.Companion.Slot
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.item.ItemBlock

/**
 * Renders the items held by a brewing stand.
 */
class BrewingStandRenderer : TileEntitySpecialRenderer<BrewingStandLogic>() {
    override fun renderTileEntityAt(te: BrewingStandLogic, x: Double, y: Double, z: Double,
                                    partialTicks: Float, destroyStage: Int) {
        pushMatrix()
        color(1f, 1f, 1f, 1f)

        // Use the brightness of the block directly above the table for lighting.
        val light = te.world.getCombinedLight(te.pos.add(0, 1, 0), 0)
        val bX = light % 65536
        val bY = light / 65536
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bX.toFloat(), bY.toFloat())

        // Translate to the stand's center and rotate according to its orientation
        translate(x + 0.5, y + 0.5, z + 0.5)

        Minecraft.getMinecraft().textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        enableRescaleNormal()
        RenderHelper.enableStandardItemLighting()

        renderItem(te, Slot.INPUT1, 0.25 + 0.0625 / 4, .0)
        rotate(135f, 0f, 1f, 0f)
        renderItem(te, Slot.INPUT2, 0.25 + 0.0625 / 4, .0)
        rotate(90f, 0f, 1f, 0f)
        renderItem(te, Slot.INPUT3, 0.25 + 0.0625 / 4, .0)

        RenderHelper.disableStandardItemLighting()
        disableRescaleNormal()

        popMatrix()
    }

    /**
     * Renders an item on top of the crafting table.
     */
    private fun renderItem(te: BrewingStandLogic, slot: Slot, x: Double, z: Double) {
        pushMatrix()
        // Magic numbers, but this appears to be the perfect offset
        translate(x, .0, z)
        val stack = te[slot]
        // Most blocks use a block model which requires special treatment
        if (stack?.item is ItemBlock) {
            scale(2f, 2f, 2f)
        }
        // Again, magic numbers
        scale(0.5, 0.5, 0.5)
        Minecraft.getMinecraft().renderItem.renderItem(stack, ItemCameraTransforms.TransformType.FIXED)
        popMatrix()
    }
}