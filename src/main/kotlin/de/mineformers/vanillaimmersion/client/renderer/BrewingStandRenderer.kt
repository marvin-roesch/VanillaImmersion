package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.block.BrewingStand.Companion.BOTTLE1_AABB
import de.mineformers.vanillaimmersion.block.BrewingStand.Companion.BOTTLE2_AABB
import de.mineformers.vanillaimmersion.block.BrewingStand.Companion.BOTTLE3_AABB
import de.mineformers.vanillaimmersion.block.BrewingStand.Companion.BOWL_AABB
import de.mineformers.vanillaimmersion.tileentity.BrewingStandLogic
import de.mineformers.vanillaimmersion.tileentity.BrewingStandLogic.Companion.Slot
import de.mineformers.vanillaimmersion.util.Rays
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.item.ItemBlock
import net.minecraft.util.math.RayTraceResult
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Renders the items held by a brewing stand.
 */
class BrewingStandRenderer : TileEntitySpecialRenderer<BrewingStandLogic>() {
    object Highlight {
        @SubscribeEvent
        fun onRenderHighlight(event: DrawBlockHighlightEvent) {
            // Straight copy out of Vanilla, only checks whether a sub-box was hit
            val player = event.player
            val world = player.worldObj
            if (event.target.typeOfHit != RayTraceResult.Type.BLOCK ||
                world.getBlockState(event.target.blockPos).block !== VanillaImmersion.Blocks.BREWING_STAND)
                return
            val pos = event.target.blockPos
            val boxes = listOf(
                BOTTLE1_AABB.offset(pos), BOTTLE2_AABB.offset(pos), BOTTLE3_AABB.offset(pos), BOWL_AABB.offset(pos)
            )
            val hit = Rays.rayTraceBoxes(player, boxes)
            if (hit >= 0) {
                enableBlend()
                tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                                     SourceFactor.ONE, DestFactor.ZERO)
                color(0.0f, 0.0f, 0.0f, 0.4f)
                glLineWidth(2.0f)
                disableTexture2D()
                depthMask(false)

                val partialTicks = event.partialTicks
                if (world.worldBorder.contains(pos)) {
                    val cX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks
                    val cY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks
                    val cZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
                    RenderGlobal.drawSelectionBoundingBox(boxes[hit].expandXyz(0.002).offset(-cX, -cY, -cZ),
                                                          0f, 0f, 0f, 0.4f)
                }

                depthMask(true)
                enableTexture2D()
                disableBlend()
                event.isCanceled = true
            }
        }
    }

    override fun renderTileEntityAt(te: BrewingStandLogic, x: Double, y: Double, z: Double,
                                    partialTicks: Float, destroyStage: Int) {
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
        if (stack?.item is ItemBlock) {
            scale(2f, 2f, 2f)
        }
        // Again, magic numbers
        scale(0.5, 0.5, 0.5)
        Minecraft.getMinecraft().renderItem.renderItem(stack, ItemCameraTransforms.TransformType.FIXED)
        popMatrix()
    }
}