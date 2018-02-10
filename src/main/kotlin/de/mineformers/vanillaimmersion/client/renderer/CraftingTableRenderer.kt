package de.mineformers.vanillaimmersion.client.renderer

import de.mineformers.vanillaimmersion.block.CraftingDrawer
import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic.Companion.Slot
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.BlockRendererDispatcher
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.GlStateManager.color
import net.minecraft.client.renderer.GlStateManager.depthMask
import net.minecraft.client.renderer.GlStateManager.disableFog
import net.minecraft.client.renderer.GlStateManager.disableLighting
import net.minecraft.client.renderer.GlStateManager.disableRescaleNormal
import net.minecraft.client.renderer.GlStateManager.enableDepth
import net.minecraft.client.renderer.GlStateManager.enableFog
import net.minecraft.client.renderer.GlStateManager.enableRescaleNormal
import net.minecraft.client.renderer.GlStateManager.loadIdentity
import net.minecraft.client.renderer.GlStateManager.matrixMode
import net.minecraft.client.renderer.GlStateManager.ortho
import net.minecraft.client.renderer.GlStateManager.popMatrix
import net.minecraft.client.renderer.GlStateManager.pushMatrix
import net.minecraft.client.renderer.GlStateManager.rotate
import net.minecraft.client.renderer.GlStateManager.scale
import net.minecraft.client.renderer.GlStateManager.translate
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.FIXED
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.shader.Framebuffer
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.item.ItemBlock
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.client.model.animation.Animation
import net.minecraftforge.common.model.animation.CapabilityAnimation
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.common.property.Properties
import net.minecraftforge.fml.common.Loader
import org.lwjgl.opengl.GL11.GL_MODELVIEW
import org.lwjgl.opengl.GL11.GL_PROJECTION


/**
 * Renders the items on top of a crafting table.
 */
open class CraftingTableRenderer : TileEntitySpecialRenderer<CraftingTableLogic>() {

    protected val framebuffer by lazy { Framebuffer(110, 94, false) }
    protected var blockRenderer: BlockRendererDispatcher? = null

    override fun render(te: CraftingTableLogic, x: Double, y: Double, z: Double,
                        partialTicks: Float, destroyStage: Int, partialAlpha: Float) {
        if (te.blockState.block !is CraftingTable)
            return

        pushMatrix()
        color(1f, 1f, 1f, 1f)

        // Use the brightness of the block directly above the table for lighting.
        val light = te.world.getCombinedLight(te.pos.add(0, 1, 0), 0)
        val bX = light % 65536
        val bY = light / 65536
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bX.toFloat(), bY.toFloat())

        // Translate to the table's center and rotate according to its orientation
        translate(x + 0.5, y + 0.875, z + 0.5)
        rotate(180f - te.facing.horizontalAngle, 0f, 1f, 0f)
        // The slots aren't completely centered due to the output slot, translate 1 "pixel" to the left therefore
        translate(0.0625, 0.0, 0.0)

        Minecraft.getMinecraft().textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        enableRescaleNormal()
        RenderHelper.enableStandardItemLighting()

        // Render each slot of the table individually
        // TODO: Move this to a custom IBakedModel
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

        val font = Minecraft.getMinecraft().fontRenderer
        if (Loader.isModLoaded("jei")) {
            pushMatrix()
            translate(0f, 0.001f, 0f)
            scale(0.025f, -0.025f, 0.025f)
            rotate(180f, 0f, 1f, 0f)
            rotate(90f, 1f, 0f, 0f)
            font.drawString("?", -font.getStringWidth("?") / 2 + 17, 5, 0xFFFFFF)
            popMatrix()
        }

        if (te.drawerChanging || te.drawerOpened) {
            renderBookFramebuffer()
            renderBook(te)
        }

        popMatrix()
    }

    private fun renderBookFramebuffer() {
        framebuffer.bindFramebuffer(true)

        disableFog()
        matrixMode(GL_PROJECTION)
        pushMatrix()
        loadIdentity()
        ortho(0.0, 110.0, 94.0, 0.0, 1000.0, 3000.0)
        matrixMode(GL_MODELVIEW)
        pushMatrix()
        loadIdentity()
        translate(0.0f, 0.0f, -2000.0f)
        depthMask(true)
        color(1f, 1f, 1f, 1f)
        disableLighting()
        RenderHelper.disableStandardItemLighting()

        drawRecipeBook()

        matrixMode(GL_PROJECTION)
        popMatrix()
        matrixMode(GL_MODELVIEW)
        popMatrix()
        enableFog()

        framebuffer.unbindFramebuffer()

        Minecraft.getMinecraft().framebuffer.bindFramebuffer(true)
    }

    private fun drawRecipeBook() {
        enableDepth()
        RenderHelper.enableGUIStandardItemLighting()
        val itemRenderer = Minecraft.getMinecraft().renderItem
        itemRenderer.renderItemAndEffectIntoGUI(CreativeTabs.MISC.iconItemStack, 3, 5)
        itemRenderer.renderItemAndEffectIntoGUI(CreativeTabs.FOOD.iconItemStack, 14, 5)
        RenderHelper.disableStandardItemLighting()
    }

    private fun renderBook(te: CraftingTableLogic) {
        val width = CraftingDrawer.sampleWidth(te)
        pushMatrix()
        translate(0.25 + 0.03125 + width, -0.1249, 0.1875)
        rotate(90f, 1f, 0f, 0f)
        scale(-.004, -.004, 1.0)
        color(1f, 1f, 1f, 1f)
        framebuffer.bindFramebufferTexture()

        val tessellator = Tessellator.getInstance()
        val bufferbuilder = tessellator.buffer
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX)
        bufferbuilder.pos(.0, 94.0, .0).tex(.0, .0).endVertex()
        bufferbuilder.pos(110.0, 94.0, 0.0).tex(1.0, .0).endVertex()
        bufferbuilder.pos(110.0, .0, 0.0).tex(1.0, 1.0).endVertex()
        bufferbuilder.pos(.0, .0, 0.0).tex(.0, 1.0).endVertex()
        tessellator.draw()
        popMatrix()
    }

    override fun renderTileEntityFast(te: CraftingTableLogic, x: Double, y: Double, z: Double, partialTicks: Float, destroyStage: Int, partial: Float, renderer: BufferBuilder) {
        if (!te.hasCapability(CapabilityAnimation.ANIMATION_CAPABILITY, null)) {
            return
        }
        if (blockRenderer == null) blockRenderer = Minecraft.getMinecraft().blockRendererDispatcher
        val pos = te.pos
        val world = MinecraftForgeClient.getRegionRenderCache(te.world, pos)
        var state = world.getBlockState(pos)
        state = state.getActualState(world, pos)
        if (state.propertyKeys.contains(Properties.StaticProperty)) {
            state = state.withProperty(Properties.StaticProperty, false)
        }
        if (state is IExtendedBlockState) {
            var exState = state
            if (exState.unlistedNames.contains(Properties.AnimationProperty)) {
                val time = Animation.getWorldTime(getWorld(), partialTicks)
                val capability = te.getCapability(CapabilityAnimation.ANIMATION_CAPABILITY, null)
                if (capability != null) {
                    val pair = capability.apply(time)

                    val model = blockRenderer!!.blockModelShapes.getModelForState(exState.clean)
                    exState = exState.withProperty(Properties.AnimationProperty, pair.left)

                    renderer.setTranslation(x - pos.x, y - pos.y, z - pos.z)

                    blockRenderer!!.blockModelRenderer.renderModel(world, model, exState, pos, renderer, false)
                }
            }
        }
    }

    /**
     * Renders an item on top of the crafting table.
     */
    private fun renderItem(te: CraftingTableLogic, slot: Slot, x: Double, z: Double) {
        pushMatrix()
        // Magic numbers, but this appears to be the perfect offset
        translate(x, 0.01, z)
        val stack = te[slot]
        // Most blocks use a block model which requires special treatment
        if (stack.item is ItemBlock) {
            translate(0.0, 0.06328125, 0.0)
            scale(2f, 2f, 2f)
        } else {
            // Rotate items to lie down flat on the anvil
            rotate(90f, 1f, 0f, 0f)
            rotate(180f, 0f, 1f, 0f)
        }
        // Again, magic numbers
        scale(0.140625, 0.140625, 0.140625)
        Minecraft.getMinecraft().renderItem.renderItem(stack, FIXED)
        popMatrix()
    }

    override fun isGlobalRenderer(te: CraftingTableLogic?) = true
}