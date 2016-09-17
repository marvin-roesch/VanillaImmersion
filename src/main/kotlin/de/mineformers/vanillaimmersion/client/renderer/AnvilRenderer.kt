package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.block.Anvil
import de.mineformers.vanillaimmersion.client.gui.AnvilTextGui
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic.Companion.Slot
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.FIXED
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.resources.I18n
import net.minecraft.item.ItemBlock
import org.apache.commons.lang3.StringUtils

/**
 * Renders the items on top of an anvil.
 */
open class AnvilRenderer : TileEntitySpecialRenderer<AnvilLogic>() {
    // TODO: Maybe switch to FastTESR?
    override fun renderTileEntityAt(te: AnvilLogic, x: Double, y: Double, z: Double,
                                    partialTicks: Float, destroyStage: Int) {
        if (te.blockState.block !is Anvil)
            return
        pushMatrix()
        color(1f, 1f, 1f, 1f)

        // Use the brightness of the block above the anvil for lighting
        val light = te.world.getCombinedLight(te.pos.add(0, 1, 0), 0)
        val bX = light % 65536
        val bY = light / 65536
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bX.toFloat(), bY.toFloat())

        // Translate to the anvil's center and rotate according to its orientation
        translate(x + 0.5, y + 1.0, z + 0.5)
        rotate(180f - te.facing.horizontalAngle, 0f, 1f, 0f)

        enableRescaleNormal()
        RenderHelper.enableStandardItemLighting()
        Minecraft.getMinecraft().textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)

        // Render both inputs
        // TODO: Achieve this with baked models
        renderItem(te, Slot.INPUT_OBJECT, .0, .33)
        renderItem(te, Slot.INPUT_MATERIAL, .0, -.03)

        // Render the hammer, if present
        val hammer = te[Slot.HAMMER]
        if (hammer != null) {
            pushMatrix()
            // Magic numbers, but this appears to be the perfect offset
            translate(.0, .05, -.32)
            rotate(-90f, 0f, 1f, 0f)
            rotate(85f, 1f, 0f, 0f)
            rotate(6f, 0f, 0f, 1f)
            scale(0.5f, 0.5f, 0.5f)
            Minecraft.getMinecraft().renderItem.renderItem(hammer, FIXED)
            popMatrix()
        }

        RenderHelper.disableStandardItemLighting()
        disableRescaleNormal()

        // Render the text on the front of the anvil
        translate(0f, -0.05f, 0f)
        rotate(90f, 0f, 1f, 0f)
        translate(0f, 0f, 0.32f)
        scale(0.00625f, -0.00625f, 0.00625f)
        val font = Minecraft.getMinecraft().fontRendererObj
        val activeScreen = Minecraft.getMinecraft().currentScreen

        if (StringUtils.isNotEmpty(te.itemName)) {
            val label = I18n.format("vimmersion.anvil.itemName")
            font.drawString(label, -font.getStringWidth(label) / 2, 25 - font.FONT_HEIGHT - 2, 0xFFFFFF)
            if (activeScreen is AnvilTextGui && activeScreen.anvil == te) {
                activeScreen.nameField.xPosition = -font.getStringWidth(te.itemName) / 2
                activeScreen.nameField.yPosition = 25
                activeScreen.nameField.drawTextBox()
            } else {
                font.drawString(te.itemName, -font.getStringWidth(te.itemName) / 2, 25, 0xFFFFFF)
            }
        } else if (activeScreen is AnvilTextGui && activeScreen.anvil == te) {
            val label = I18n.format("vimmersion.anvil.itemName")
            font.drawString(label, -font.getStringWidth(label) / 2, 25 - font.FONT_HEIGHT - 2, 0xFFFFFF)
            activeScreen.nameField.xPosition = -font.getStringWidth(activeScreen.nameField.text) / 2
            activeScreen.nameField.yPosition = 25
            activeScreen.nameField.drawTextBox()
        }

        popMatrix()
    }

    /**
     * Renders an item on top of the anvil.
     */
    private fun renderItem(te: AnvilLogic, slot: Slot, x: Double, z: Double) {
        pushMatrix()
        // Magic numbers, but this appears to be the perfect offset
        translate(x, 0.015, z)
        rotate(-90f, 0f, 1f, 0f)
        val stack = te[slot]
        // Most blocks use a block model which requires special treatment
        if (stack?.item is ItemBlock) {
            translate(0.0, 0.135, 0.0)
            scale(2f, 2f, 2f)
        } else {
            // Rotate items to lie down flat on the anvil
            rotate(90f, 1f, 0f, 0f)
            rotate(180f, 0f, 1f, 0f)
        }
        scale(0.3, 0.3, 0.3)
        Minecraft.getMinecraft().renderItem.renderItem(stack, FIXED)
        popMatrix()
    }
}