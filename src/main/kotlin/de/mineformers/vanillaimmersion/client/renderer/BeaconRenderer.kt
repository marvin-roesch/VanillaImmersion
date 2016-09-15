package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.tileentity.BeaconLogic
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.tileentity.TileEntityBeacon
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.GL_QUADS
import java.lang.Math.*

/**
 * Renders effect icons for the beacon.
 */
class BeaconRenderer : TileEntityBeaconRenderer() {
    override fun renderTileEntityAt(te: TileEntityBeacon?, x: Double, y: Double, z: Double,
                                    partialTicks: Float, destroyStage: Int) {
        super.renderTileEntityAt(te, x, y, z, partialTicks, destroyStage)
        if (te !is BeaconLogic || te.levels <= 0) {
            return
        }
        pushMatrix()
        translate(x, y, z)
        translate(.5, .0, .5)
        val uvs: Map<Int, List<Double>>
        if (te.state == null) {
            val primary = te.primaryEffect ?: return
            val secondary = te.secondaryEffect
            Minecraft.getMinecraft().renderEngine.bindTexture(GuiContainer.INVENTORY_BACKGROUND)
            var uMin = (primary.statusIconIndex % 8 * 18) / 256.0
            var vMin = (198 + primary.statusIconIndex / 8 * 18) / 256.0
            var uMax = uMin + 18 / 256f
            var vMax = vMin + 18 / 256f
            val primaryUVs = listOf(uMin, vMin, uMax, vMax)
            if (secondary != null) {
                uMin = (secondary.statusIconIndex % 8 * 18) / 256.0
                vMin = (198 + secondary.statusIconIndex / 8 * 18) / 256.0
                uMax = uMin + 18 / 256f
                vMax = vMin + 18 / 256f
                val secondaryUVs = listOf(uMin, vMin, uMax, vMax)
                uvs = mapOf(0 to primaryUVs, 1 to secondaryUVs, 2 to primaryUVs, 3 to secondaryUVs)
            } else {
                uvs = mapOf(0 to primaryUVs, 1 to primaryUVs, 2 to primaryUVs, 3 to primaryUVs)
            }
        } else {
            val state = te.state!!
            Minecraft.getMinecraft().renderEngine.bindTexture(ResourceLocation("textures/items/barrier.png"))
            val barrierUVs = listOf(.0, .0, 1.0, 1.0)
            val active = if (state.stage <= 1) state.primary else state.secondary
            if (active == null) {
                uvs = mapOf(0 to barrierUVs, 1 to barrierUVs, 2 to barrierUVs, 3 to barrierUVs)
            } else {
                val uMin = (active.statusIconIndex % 8 * 18) / 256.0
                val vMin = (198 + active.statusIconIndex / 8 * 18) / 256.0
                val uMax = uMin + 18 / 256f
                val vMax = vMin + 18 / 256f
                val activeUVs = listOf(uMin, vMin, uMax, vMax)
                uvs = mapOf(0 to activeUVs, 1 to activeUVs, 2 to activeUVs, 3 to activeUVs)
            }
        }
        enableBlend()
        tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                             SourceFactor.ONE, DestFactor.ZERO)
        depthMask(false)
        disableLighting()
        disableAlpha()
        val t = te.world.totalWorldTime % 90.0
        color(1f, 1f, 1f, pulsate(t, 0.1, 0.3, 90.0))
        renderQuads(uvs)
        disableBlend()
        depthMask(true)
        enableAlpha()
        enableLighting()
        popMatrix()
        tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                             SourceFactor.ONE, DestFactor.ZERO)
    }

    fun pulsate(time: Double, start: Double, change: Double, duration: Double) =
        abs(start + change * sin(2 * PI * time * (1 / (duration * 2)))).toFloat()

    fun renderQuads(uvs: Map<Int, List<Double>>) {
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        for (i in 0..3) {
            if (i !in uvs.keys)
                continue
            val localUVs = uvs[i]!!
            pushMatrix()
            rotate(i * 90f, 0f, 1f, 0f)
            buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
            buffer.pos(-.25, .75, .25 + 0.06255).tex(localUVs[0], localUVs[1]).endVertex()
            buffer.pos(-.25, .25, .25 + 0.06255).tex(localUVs[0], localUVs[3]).endVertex()
            buffer.pos(.25, .25, .25 + 0.06255).tex(localUVs[2], localUVs[3]).endVertex()
            buffer.pos(.25, .75, .25 + 0.06255).tex(localUVs[2], localUVs[1]).endVertex()
            tessellator.draw()
            popMatrix()
        }
    }
}