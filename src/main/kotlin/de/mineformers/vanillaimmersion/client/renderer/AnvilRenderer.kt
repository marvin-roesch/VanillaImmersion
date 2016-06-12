package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.VanillaImmersion
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
import org.lwjgl.opengl.GL11

/**
 * Renders the items on top of an anvil.
 */
class AnvilRenderer : TileEntitySpecialRenderer<AnvilLogic>() {
    // TODO: Maybe switch to FastTESR?
    override fun renderTileEntityAt(te: AnvilLogic, x: Double, y: Double, z: Double,
                                    partialTicks: Float, destroyStage: Int) {
        if (te.blockState.block !== VanillaImmersion.Blocks.ANVIL)
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
        renderItem(te, Slot.INPUT_OBJECT, 0.2, 0.2)
        renderItem(te, Slot.INPUT_MATERIAL, -0.2, 0.4)

        // Render the output translucently
        enableBlend()
        tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
        Shaders.ALPHA.activate()
        Shaders.ALPHA.setUniformFloat("alpha", 0.5f)
        renderItem(te, Slot.OUTPUT, 0.0, -0.25)
        Shaders.ALPHA.deactivate()
        disableBlend()

        RenderHelper.disableStandardItemLighting()
        disableRescaleNormal()

        // Render the text on the front of the anvil
        translate(0f, -0.05f, 0f)
        rotate(90f, 0f, 1f, 0f)
        translate(0f, 0f, 0.32f)
        scale(0.00625f, -0.00625f, 0.00625f)
        val font = Minecraft.getMinecraft().fontRendererObj
        if (StringUtils.isNotEmpty(te.itemName)) {
            val label = I18n.format("vimmersion.anvil.itemName")
            font.drawString(label, -font.getStringWidth(label) / 2, 25 - font.FONT_HEIGHT - 2, 0xFFFFFF)
            font.drawString(te.itemName, -font.getStringWidth(te.itemName) / 2, 25, 0xFFFFFF)
        }

        popMatrix()
    }

    /**
     * Renders an item on top of the anvil.
     */
    private fun renderItem(te: AnvilLogic, slot: Slot, x: Double, z: Double) {
        pushMatrix()
        // Magic numbers, but this appears to be the perfect offset
        translate(x, 0.015625, z)
        rotate(-90f, 0f, 1f, 0f)
        val stack = te[slot]
        // Most blocks use a block model which requires special treatment
        if (stack?.item is ItemBlock) {
            translate(0.0, 0.171875, 0.0)
            scale(2f, 2f, 2f)
        } else {
            // Rotate items to lie down flat on the anvil
            rotate(90f, 1f, 0f, 0f)
            rotate(180f, 0f, 1f, 0f)
        }
        scale(0.375, 0.375, 0.375)
        Minecraft.getMinecraft().renderItem.renderItem(stack, FIXED)
        popMatrix()
    }
}