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
        // Build icons for the beacon
        val icons: Map<Int, BeaconIcon> =
            if (te.state == null) {
                // If not editing, just use the current effects
                buildIconMap(te.primaryEffect, te.secondaryEffect) ?: return
            } else if (te.state!!.stage > 2) {
                // If editing, but in last stage, display result
                buildIconMap(te.state!!.primary, te.state!!.secondary) ?: return
            } else {
                // If editing, decide whether to display barrier (= no selection) or active selection
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
        // Override the depth function to allow correct overlaying of secondary effect icons
        depthFunc(GL11.GL_LESS)
        tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                             SourceFactor.ONE, DestFactor.ZERO)
        disableLighting()
        disableAlpha()
        color(1f, 1f, 1f, 1f)
        // Activate shader to assign infinite depth to transparent pixels
        Shaders.TRANSPARENT_DEPTH.activate()
        renderQuads(icons)
        Shaders.TRANSPARENT_DEPTH.deactivate()
        disableBlend()
        enableAlpha()
        enableLighting()
        // Reset changes to the depth function
        depthFunc(GL11.GL_LEQUAL)
        popMatrix()
    }

    /**
     * Generates a map from side to beacon icon, based on the passed effects.
     */
    private fun buildIconMap(primary: Potion?, secondary: Potion?): Map<Int, BeaconIcon>? {
        if (primary == null)
            return null
        // Basic UV calculation, ripped from Vanilla
        var uMin = (primary.statusIconIndex % 8 * 18) / 256.0
        var vMin = (198 + primary.statusIconIndex / 8 * 18) / 256.0
        var uMax = uMin + 18 / 256f
        var vMax = vMin + 18 / 256f
        // Make the icons pulsate in a 90 tick time frame
        val t = Minecraft.getMinecraft().theWorld.totalWorldTime % 90.0
        val alpha = pulsate(t, 0.1, 0.3, 90.0)
        val primaryIcon = BeaconIcon(GuiContainer.INVENTORY_BACKGROUND,
                                     listOf(uMin, vMin, uMax, vMax),
                                     false,
                                     alpha)
        if (secondary != null) {
            // If the secondary effect is just the level 2 effect, display only that
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

    /**
     * Calculates the current value of a pulsating motion with a given duration.
     */
    fun pulsate(time: Double, start: Double, change: Double, duration: Double) =
        (start + change * abs(sin(2 * PI * time * (1 / (duration * 2))))).toFloat()

    /**
     * Renders beacon icons on every side using the provided information.
     */
    fun renderQuads(uvs: Map<Int, BeaconIcon>) {
        val tessellator = Tessellator.getInstance()
        val buffer = tessellator.buffer
        // Draw something for all four sides
        for (i in 0..3) {
            if (i !in uvs.keys)
                continue
            val (texture, localUVs, level2, alpha) = uvs[i]!!
            pushMatrix()
            // Rotate according to the current side
            rotate(i * 90f, 0f, 1f, 0f)
            color(1f, 1f, 1f, alpha)
            // Draw the level 2 overlay first, to fill the depth buffer accordingly
            if (level2) {
                // The level 2 overlay requires that transparent pixels have infinite depth
                Shaders.TRANSPARENT_DEPTH.setUniformBool("overrideDepth", true)
                Minecraft.getMinecraft().renderEngine.bindTexture(POTION_LEVEL_TEXTURE)
                buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
                buffer.pos(-.25, .75, .25 + 0.0626).tex(.0, .0).endVertex()
                buffer.pos(-.25, .25, .25 + 0.0626).tex(.0, 1.0).endVertex()
                buffer.pos(.25, .25, .25 + 0.0626).tex(1.0, 1.0).endVertex()
                buffer.pos(.25, .75, .25 + 0.0626).tex(1.0, .0).endVertex()
                tessellator.draw()
            }
            // The actual icon does not need infinite depth for transparent pixels
            Shaders.TRANSPARENT_DEPTH.setUniformBool("overrideDepth", false)
            Minecraft.getMinecraft().renderEngine.bindTexture(texture)
            buffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX)
            buffer.pos(-.25, .75, .25 + 0.0626).tex(localUVs[0], localUVs[1]).endVertex()
            buffer.pos(-.25, .25, .25 + 0.0626).tex(localUVs[0], localUVs[3]).endVertex()
            buffer.pos(.25, .25, .25 + 0.0626).tex(localUVs[2], localUVs[3]).endVertex()
            buffer.pos(.25, .75, .25 + 0.0626).tex(localUVs[2], localUVs[1]).endVertex()
            tessellator.draw()
            popMatrix()
        }
    }

    /**
     * Wraps around the information about beacon icons.
     */
    data class BeaconIcon(val texture: ResourceLocation, val uvs: List<Double>,
                          val level2: Boolean = false, val alpha: Float = 0.7f)
}