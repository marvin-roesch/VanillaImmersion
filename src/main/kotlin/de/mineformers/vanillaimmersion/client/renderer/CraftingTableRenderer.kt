package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic.Companion.Slot
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.FIXED
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.item.ItemBlock

/**
 * ${JDOC}
 */
class CraftingTableRenderer : TileEntitySpecialRenderer<CraftingTableLogic>() {
    override fun renderTileEntityAt(te: CraftingTableLogic?,
                                    x: Double, y: Double, z: Double,
                                    partialTicks: Float, destroyStage: Int) {
        if (te == null)
            return
        pushMatrix()
        color(1f, 1f, 1f, 1f)
        translate(x + 0.5, y + 1.0, z + 0.5)
        rotate(180f - te.facing.horizontalAngle, 0f, 1f, 0f)
        translate(0.0625, 0.0, 0.0)
        Minecraft.getMinecraft().textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        enableRescaleNormal()
        RenderHelper.enableStandardItemLighting()
        val light = te.world.getCombinedLight(te.pos.add(0, 1, 0), 0)
        val bX = light % 65536
        val bY = light / 65536
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bX.toFloat(), bY.toFloat())

        renderItem(te, Slot.IN_TOP_LEFT, 0.1875, 0.1875)
        renderItem(te, Slot.IN_TOP, 0.0, 0.1875)
        renderItem(te, Slot.IN_TOP_RIGHT, -0.1875, 0.1875)

        renderItem(te, Slot.IN_LEFT, 0.1875, 0.0)
        renderItem(te, Slot.IN_MIDDLE, 0.0, 0.0)
        renderItem(te, Slot.IN_RIGHT, -0.1875, 0.0)

        renderItem(te, Slot.IN_BOTTOM_LEFT, 0.1875, -0.1875)
        renderItem(te, Slot.IN_BOTTOM, 0.0, -0.1875)
        renderItem(te, Slot.IN_BOTTOM_RIGHT, -0.1875, -0.1875)

        renderItem(te, Slot.OUTPUT, -0.1875 * 2, 0.0)

        RenderHelper.disableStandardItemLighting()
        disableRescaleNormal()
        popMatrix()
    }

    private fun renderItem(te: CraftingTableLogic, slot: Slot, x: Double, z: Double) {
        pushMatrix()
        translate(x, 0.0703125, z)
        val stack = te[slot]
        if (stack?.item is ItemBlock) {
            scale(2f, 2f, 2f)
        } else {

        }
        scale(0.140625, 0.140625, 0.140625)
        Minecraft.getMinecraft().renderItem.renderItem(stack, FIXED)
        popMatrix()
    }
}