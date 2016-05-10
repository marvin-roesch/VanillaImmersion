package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.tileentity.AnvilLogic
import de.mineformers.vanillaimmersion.tileentity.AnvilLogic.Companion.Slot
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.FIXED
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.item.ItemBlock
import org.lwjgl.opengl.GL11

/**
 * ${JDOC}
 */
class AnvilRenderer : TileEntitySpecialRenderer<AnvilLogic>() {
    override fun renderTileEntityAt(te: AnvilLogic?,
                                    x: Double, y: Double, z: Double,
                                    partialTicks: Float, destroyStage: Int) {
        if (te == null)
            return
        pushMatrix()
        color(1f, 1f, 1f, 1f)
        val light = te.world.getCombinedLight(te.pos.add(0, 1, 0), 0)
        val bX = light % 65536
        val bY = light / 65536
        translate(x + 0.5, y + 1.0, z + 0.5)
        rotate(180f - te.facing.horizontalAngle, 0f, 1f, 0f)
        Minecraft.getMinecraft().textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        enableRescaleNormal()
        RenderHelper.enableStandardItemLighting()
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bX.toFloat(), bY.toFloat())

        renderItem(te, Slot.INPUT_OBJECT, 0.2, 0.2)
        renderItem(te, Slot.INPUT_MATERIAL, -0.2, 0.4)
        enableBlend()
        tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
        Shaders.ALPHA.activate()
        Shaders.ALPHA.setUniformFloat("alpha", 0.5f)
        renderItem(te, Slot.OUTPUT, 0.0, -0.25)
        Shaders.ALPHA.deactivate()
        disableBlend()

        RenderHelper.disableStandardItemLighting()
        disableRescaleNormal()
        rotate(te.facing.horizontalAngle - 180, 0f, 1f, 0f)
        val vec = te.facing.rotateY().directionVec
        translate(vec.x * 0.32, vec.y * 0.32, vec.z * 0.32)
        scale(0.00625f, -0.00625f, 0.00625f)
        rotate(180f, 0f, 1f, 0f)
        val font = Minecraft.getMinecraft().fontRendererObj
        if (te.currentName != null) {
            font.drawString("Item Name:", -font.getStringWidth("Item Name:") / 2, 25 - font.FONT_HEIGHT - 2, 0xFFFFFF)
            font.drawString(te.currentName, -font.getStringWidth(te.currentName) / 2, 25, 0xFFFFFF)
        }
        popMatrix()
    }

    private fun renderItem(te: AnvilLogic, slot: Slot, x: Double, z: Double) {
        pushMatrix()
        translate(x, 0.03125 / 2, z)
        rotate(-90f, 0f, 1f, 0f)
        val stack = te[slot]
        if (stack?.item is ItemBlock) {
            translate(0.0, 0.171875, 0.0)
            scale(2f, 2f, 2f)
        } else {
            rotate(90f, 1f, 0f, 0f)
            rotate(180f, 0f, 1f, 0f)
        }
        scale(0.375, 0.375, 0.375)
        Minecraft.getMinecraft().renderItem.renderItem(stack, FIXED)
        popMatrix()
    }
}