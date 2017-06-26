package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.tileentity.BrewingStandLogic
import de.mineformers.vanillaimmersion.tileentity.BrewingStandLogic.Companion.Slot
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager.color
import net.minecraft.client.renderer.GlStateManager.disableRescaleNormal
import net.minecraft.client.renderer.GlStateManager.enableRescaleNormal
import net.minecraft.client.renderer.GlStateManager.popMatrix
import net.minecraft.client.renderer.GlStateManager.pushMatrix
import net.minecraft.client.renderer.GlStateManager.rotate
import net.minecraft.client.renderer.GlStateManager.scale
import net.minecraft.client.renderer.GlStateManager.translate
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.item.ItemBlock

/**
 * Renders the items held by a brewing stand.
 */
open class BrewingStandRenderer : TileEntitySpecialRenderer<BrewingStandLogic>() {
    override fun render(te: BrewingStandLogic, x: Double, y: Double, z: Double,
                        partialTicks: Float, destroyStage: Int, partialAlpha: Float) {
        pushMatrix()
        color(1f, 1f, 1f, 1f)

        // Use the brightness of the brewing stand for lighting.
        val light = te.world.getCombinedLight(te.pos, 0)
        val bX = light % 65536
        val bY = light / 65536
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bX.toFloat(), bY.toFloat())

        // Translate to the stand's center and rotate according to its orientation
        translate(x + 0.5, y + 0.5, z + 0.5)

        Minecraft.getMinecraft().textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        enableRescaleNormal()
        RenderHelper.enableStandardItemLighting()

        // Render the ingredient
        pushMatrix()
        translate(.0, .5 - 0.0625, .0)
        rotate(90f, 0f, 1f, 0f)
        rotate(-90f, 1f, 0f, 0f)
        scale(0.5, 0.5, 1.0)
        renderItem(te, Slot.INPUT_INGREDIENT, .0, .0)
        popMatrix()

        // Render all input/output bottles
        renderItem(te, Slot.BOTTLE1, 0.25 + 0.0625 / 4, .0)
        rotate(135f, 0f, 1f, 0f)
        renderItem(te, Slot.BOTTLE2, 0.25 + 0.0625 / 4, .0)
        rotate(90f, 0f, 1f, 0f)
        renderItem(te, Slot.BOTTLE3, 0.25 + 0.0625 / 4, .0)

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
        if (stack.item is ItemBlock) {
            scale(2f, 2f, 2f)
        }
        // Again, magic numbers
        scale(0.5, 0.5, 0.5)
        Minecraft.getMinecraft().renderItem.renderItem(stack, ItemCameraTransforms.TransformType.FIXED)
        popMatrix()
    }
}