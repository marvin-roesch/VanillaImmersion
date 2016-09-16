package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.tileentity.BeaconLogic
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.init.MobEffects
import net.minecraft.potion.Potion
import net.minecraft.tileentity.TileEntityBeacon
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_QUADS
import java.lang.Math.*

/**
 * Renders effect icons for the beacon.
 */
class BeaconRenderer : TileEntityBeaconRenderer() {
    val POTION_LEVEL_TEXTURE = ResourceLocation(VanillaImmersion.MODID, "textures/icons/potion_level.png")

    override fun renderTileEntityAt(te: TileEntityBeacon?, x: Double, y: Double, z: Double,
                                    partialTicks: Float, destroyStage: Int) {
        super.renderTileEntityAt(te, x, y, z, partialTicks, destroyStage)
        if (te !is BeaconLogic || te.levels <= 0) {
            return
        }
        val icons: Map<Int, BeaconIcon> =
            if (te.state == null) {
                buildIconMap(te.primaryEffect, te.secondaryEffect) ?: return
            } else if (te.state!!.stage > 2) {
                buildIconMap(te.state!!.primary, te.state!!.secondary) ?: return
            } else {
                val state = te.state!!
                val barrierIcon = BeaconIcon(ResourceLocation("textures/items/barrier.png"),
                                             listOf(.0, .0, 1.0, 1.0),
                                             false)
                val active = if (state.stage <= 1) state.primary else state.secondary
                if (active == null) {
                    mapOf(0 to barrierIcon, 1 to barrierIcon, 2 to barrierIcon, 3 to barrierIcon)
                } else {
                    val uMin = (active.statusIconIndex % 8 * 18) / 256.0
                    val vMin = (198 + active.statusIconIndex / 8 * 18) / 256.0
                    val uMax = uMin + 18 / 256f
                    val vMax = vMin + 18 / 256f
                    val icon = BeaconIcon(GuiContainer.INVENTORY_BACKGROUND, listOf(uMin, vMin, uMax, vMax),
                                          (state.stage > 1 && active != MobEffects.REGENERATION))
                    mapOf(0 to icon, 1 to icon, 2 to icon, 3 to icon)
                }
            }
        pushMatrix()
        translate(x, y, z)
        translate(.5, .0, .5)
        enableBlend()
        depthFunc(GL11.GL_LESS)
        tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                             SourceFactor.ONE, DestFactor.ZERO)
        disableLighting()
        disableAlpha()
        color(1f, 1f, 1f, 1f)
        Shaders.TRANSPARENT_DEPTH.activate()
        renderQuads(icons)
        Shaders.TRANSPARENT_DEPTH.deactivate()
        disableBlend()
        enableAlpha()
        enableLighting()
        depthFunc(GL11.GL_LEQUAL)
        popMatrix()
        tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                             SourceFactor.ONE, DestFactor.ZERO)
    }

    private fun buildIconMap(primary: Potion?, secondary: Potion?): Map<Int, BeaconIcon>? {
        if (primary == null)
            return null
        var uMin = (primary.statusIconIndex % 8 * 18) / 256.0
        var vMin = (198 + primary.statusIconIndex / 8 * 18) / 256.0
        var uMax = uMin + 18 / 256f
        var vMax = vMin + 18 / 256f
        val t = Minecraft.getMinecraft().theWorld.totalWorldTime % 90.0
        val alpha = pulsate(t, 0.1, 0.3, 90.0)
        val primaryIcon = BeaconIcon(GuiContainer.INVENTORY_BACKGROUND,
                                     listOf(uMin, vMin, uMax, vMax),
                                     false,
                                     alpha)
        if (secondary != null) {
            if (secondary == primary) {
                val secondaryIcon = primaryIcon.copy(level2 = true)
                return mapOf(0 to secondaryIcon, 1 to secondaryIcon, 2 to secondaryIcon, 3 to secondaryIcon)
            }
            uMin = (secondary.statusIconIndex % 8 * 18) / 256.0
            vMin = (198 + secondary.statusIconIndex / 8 * 18) / 256.0
            uMax = uMin + 18 / 256f
            vMax = vMin + 18 / 256f
            val secondaryIcon = BeaconIcon(GuiContainer.INVENTORY_BACKGROUND,
                                           listOf(uMin, vMin, uMax, vMax),
                                           secondary != MobEffects.REGENERATION,
                                           alpha)
            return mapOf(0 to primaryIcon, 1 to secondaryIcon, 2 to primaryIcon, 3 to secondaryIcon)
        } else {
            return mapOf(0 to primaryIcon, 1 to primaryIcon, 2 to primaryIcon, 3 to primaryIcon)
        }
    }

    fun pulsate(time: Double, start: Double, change: Double, duration: Double) =
        abs(start + change * sin(2 * PI * time * (1 / (duration * 2)))).toFloat()

    fun renderQuads(uvs: Map<Int, BeaconIcon>) {
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        for (i in 0..3) {
            if (i !in uvs.keys)
                continue
            val (texture, localUVs, level2, alpha) = uvs[i]!!
            pushMatrix()
            rotate(i * 90f, 0f, 1f, 0f)
            color(1f, 1f, 1f, alpha)
            if (level2) {
                Shaders.TRANSPARENT_DEPTH.setUniformBool("overrideDepth", true)
                Minecraft.getMinecraft().renderEngine.bindTexture(POTION_LEVEL_TEXTURE)
                buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
                buffer.pos(-.25, .75, .25 + 0.06255).tex(.0, .0).endVertex()
                buffer.pos(-.25, .25, .25 + 0.06255).tex(.0, 1.0).endVertex()
                buffer.pos(.25, .25, .25 + 0.06255).tex(1.0, 1.0).endVertex()
                buffer.pos(.25, .75, .25 + 0.06255).tex(1.0, .0).endVertex()
                tessellator.draw()
            }
            Shaders.TRANSPARENT_DEPTH.setUniformBool("overrideDepth", false)
            Minecraft.getMinecraft().renderEngine.bindTexture(texture)
            buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
            buffer.pos(-.25, .75, .25 + 0.06255).tex(localUVs[0], localUVs[1]).endVertex()
            buffer.pos(-.25, .25, .25 + 0.06255).tex(localUVs[0], localUVs[3]).endVertex()
            buffer.pos(.25, .25, .25 + 0.06255).tex(localUVs[2], localUVs[3]).endVertex()
            buffer.pos(.25, .75, .25 + 0.06255).tex(localUVs[2], localUVs[1]).endVertex()
            tessellator.draw()
            popMatrix()
        }
    }

    data class BeaconIcon(val texture: ResourceLocation, val uvs: List<Double>,
                          val level2: Boolean = false, val alpha: Float = 1f)
}