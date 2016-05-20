package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.tileentity.FurnaceLogic
import de.mineformers.vanillaimmersion.tileentity.FurnaceLogic.Companion.Slot.FUEL
import de.mineformers.vanillaimmersion.tileentity.FurnaceLogic.Companion.Slot.INPUT
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.NONE
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer

/**
 * Renders the items inside a furnace
 */
class FurnaceRenderer : TileEntitySpecialRenderer<FurnaceLogic>() {
    override fun renderTileEntityAt(te: FurnaceLogic, x: Double, y: Double, z: Double,
                                    partialTicks: Float, destroyStage: Int) {
        pushMatrix()
        color(1f, 1f, 1f, 1f)

        // Use the lighting of the block directly in front of the furnace
        val light = te.world.getCombinedLight(te.pos.offset(te.facing), 0)
        val bX = light % 65536
        val bY = light / 65536
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bX.toFloat(), bY.toFloat())

        // Translate to the furance's center and rotate according to its orientation
        translate(x + 0.5, y, z + 0.5)
        rotate(180f - te.facing.horizontalAngle, 0f, 1f, 0f)

        Minecraft.getMinecraft().textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        enableRescaleNormal()
        RenderHelper.enableStandardItemLighting()

        // Render the fuel item
        pushMatrix()
        translate(0.0, 0.2, 0.0)
        scale(0.25, 0.25, 0.25)
        // Use the "embers" shader to gradually turn the item into ashes
        // TODO: Make this actually look nice
        Shaders.EMBERS.activate()
        Shaders.EMBERS.setUniformFloat("tint", 0.1f, 0.1f, 0.1f, 0f)
        var t = te.getField(1).toFloat()
        if (t == 0f) {
            t = 200f
        }
        t = te.getField(0) / t
        if (!te.isBurning)
            t = 1f
        Shaders.EMBERS.setUniformFloat("progress", 1f - t)
        Minecraft.getMinecraft().renderItem.renderItem(te[FUEL], NONE)
        Shaders.EMBERS.deactivate()
        popMatrix()

        // Render the input item
        // TODO: Move this to an IBakedModel
        pushMatrix()
        translate(0.0, 0.675, 0.0)
        scale(0.25, 0.25, 0.25)
        Minecraft.getMinecraft().renderItem.renderItem(te[INPUT], NONE)
        popMatrix()

        RenderHelper.disableStandardItemLighting()
        popMatrix()
    }
}