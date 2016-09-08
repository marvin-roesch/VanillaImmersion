package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.util.SlotHighlights
import de.mineformers.vanillaimmersion.util.rotateY
import de.mineformers.vanillaimmersion.util.x
import de.mineformers.vanillaimmersion.util.z
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.RenderGlobal
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.math.RayTraceResult
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
import org.lwjgl.opengl.GL11

/**
 * Renders slot highlights for tile entities
 */
object SlotHighlightRenderer {
    @SubscribeEvent
    fun onRenderHighlight(event: DrawBlockHighlightEvent) {
        val player = event.player
        val world = player.worldObj
        if (event.target.typeOfHit != RayTraceResult.Type.BLOCK)
            return
        val pos = event.target.blockPos
        val tile = world.getTileEntity(pos)
        if (tile !is SlotHighlights || !tile.hasCapability(ITEM_HANDLER_CAPABILITY, null))
            return
        val itemHandler = tile.getCapability(ITEM_HANDLER_CAPABILITY, null)
        fun slotFilter(slot: Int) = itemHandler.getStackInSlot(slot) == null
        pushMatrix()
        enableBlend()
        glLineWidth(2.0f)
        depthMask(false)

        val partialTicks = event.partialTicks
        if (world.worldBorder.contains(pos)) {
            val cX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks
            val cY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks
            val cZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
            translate(-cX, -cY, -cZ)

            // Draw highlight boxes
            disableTexture2D()
            color(0.0f, 0.0f, 0.0f, 0.4f)
            tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                                 SourceFactor.ONE, DestFactor.ZERO)

            for ((box, icon, rotation) in tile.highlights.filterKeys(::slotFilter).values) {
                val rotated = box.rotateY(rotation).offset(pos)
                RenderGlobal.drawSelectionBoundingBox(rotated.expandXyz(0.002), 0f, 0f, 0f, 0.4f)
            }

            // Draw highlight icons
            enableTexture2D()
            color(1f, 1f, 1f, 0.4f)
            tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE,
                                 SourceFactor.ONE, DestFactor.ZERO)
            for ((originalBox, icon, rotation, baseUVs) in tile.highlights.filterKeys(::slotFilter).values) {
                if (icon == null)
                    continue
                val box = originalBox.rotateY(rotation).offset(pos)
                val uvs = baseUVs.map {
                    it.addVector(-.5, -.5, -.5)
                        .rotateY(rotation)
                        .addVector(.5, .5, .5)
                }

                Minecraft.getMinecraft().renderEngine.bindTexture(icon)
                val tess = Tessellator.getInstance()
                val renderer = tess.buffer
                Shaders.SLOT_HIGHLIGHT.activate()
                renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
                renderer.pos(box.minX, box.minY + 0.0001, box.maxZ).tex(uvs[0].x, uvs[0].z).endVertex()
                renderer.pos(box.maxX, box.minY + 0.0001, box.maxZ).tex(uvs[1].x, uvs[1].z).endVertex()
                renderer.pos(box.maxX, box.minY + 0.0001, box.minZ).tex(uvs[2].x, uvs[2].z).endVertex()
                renderer.pos(box.minX, box.minY + 0.0001, box.minZ).tex(uvs[3].x, uvs[3].z).endVertex()
                tess.draw()
                Shaders.SLOT_HIGHLIGHT.deactivate()
            }
        }

        depthMask(true)
        disableBlend()
        popMatrix()
        tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                             SourceFactor.ONE, DestFactor.ZERO)
    }
}