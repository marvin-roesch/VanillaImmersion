package de.mineformers.vanillaimmersion.util

import de.mineformers.vanillaimmersion.client.renderer.Shaders
import de.mineformers.vanillaimmersion.util.SelectionBox.Companion.RenderOptions
import de.mineformers.vanillaimmersion.util.SelectionBox.Companion.SlotOptions
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.RenderGlobal.drawSelectionBoundingBox
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
import org.lwjgl.opengl.GL11
import java.util.*

/**
 * TileEntities inheriting from this interface will have a preview/highlight drawn for each slot if it is not occupied.
 */
interface SubSelections {
    val boxes: List<SelectionBox>

    /**
     * If this method returns true, the Vanilla selection bounding box will not be drawn if a selection box is hovered.
     */
    fun cancelsVanillaSelectionRendering(): Boolean = false

    /**
     * Called whenever a selection box is right clicked.
     * The provided side and hit vector are relative to the unrotated box.
     *
     * @return `true` if the further processing of the click should be prevented, `false` otherwise
     */
    fun onRightClickBox(box: SelectionBox,
                        player: EntityPlayer, hand: EnumHand, stack: ItemStack,
                        side: EnumFacing, hitVec: Vec3d) = false

    /**
     * Called whenever a selection box is left clicked.
     * The provided side and hit vector are relative to the unrotated box.
     *
     * @return `true` if the further processing of the click should be prevented, `false` otherwise
     */
    fun onLeftClickBox(box: SelectionBox,
                       player: EntityPlayer, hand: EnumHand, stack: ItemStack,
                       side: EnumFacing, hitVec: Vec3d) = false

    /**
     * Called whenever the block but no selection box was right clicked.
     *
     * @return `true` if the further processing of the click should be prevented, `false` otherwise
     */
    fun onRightClickBlock(player: EntityPlayer, hand: EnumHand, stack: ItemStack,
                          side: EnumFacing, hitVec: Vec3d) = false

    /**
     * Called whenever the block but no selection box was left clicked.
     *
     * @return `true` if the further processing of the click should be prevented, `false` otherwise
     */
    fun onLeftClickBlock(player: EntityPlayer, hand: EnumHand, stack: ItemStack,
                         side: EnumFacing, hitVec: Vec3d) = false
}

/**
 * Builds a selection box.
 * Should not be used directly but rather in conjunction with Kotlin's builder notation.
 */
class SelectionBoxBuilder(var bounds: AxisAlignedBB) {
    var rotation: Rotation = Rotation.NONE
    var rendering: RenderOptions? = null
    var slot: SlotOptions? = null
    var rightClicks: Boolean = true
    var leftClicks: Boolean = true

    /**
     * Adds render options to the built selection box.
     * The options maybe modified before getting added.
     */
    fun renderOptions(init: RenderOptionsBuilder.() -> Unit = {}) {
        val builder = RenderOptionsBuilder()
        builder.init()
        rendering = builder.build()
    }

    /**
     * Adds slot options to the built selection box.
     * The options maybe modified before getting added.
     */
    fun slot(id: Int, init: SlotOptionsBuilder.() -> Unit = {}) {
        val builder = SlotOptionsBuilder(id)
        builder.init()
        slot = builder.build()
    }

    /**
     * Bakes the builder's settings into a concrete, immutable object.
     */
    fun build() = SelectionBox(bounds, rotation, rendering, slot, rightClicks, leftClicks)
}

/**
 * Builds render options for a selection box.
 * Should not be used directly but rather in conjunction with Kotlin's builder notation.
 */
class RenderOptionsBuilder {
    var icon: ResourceLocation? = null
    var uvs: List<Vec3d> = listOf(Vec3d(.0, .0, .0),
                                  Vec3d(.0, .0, 1.0),
                                  Vec3d(1.0, .0, 1.0),
                                  Vec3d(1.0, .0, .0))
    var hoveredOnly: Boolean = false
    var hoverColor: Vec3d? = null

    /**
     * Bakes the builder's settings into a concrete, immutable object.
     */
    fun build() = RenderOptions(icon, uvs, hoveredOnly, hoverColor)
}

/**
 * Builds slot options for a selection box.
 * Should not be used directly but rather in conjunction with Kotlin's builder notation.
 */
class SlotOptionsBuilder(var id: Int) {
    var renderFilled: Boolean = false

    /**
     * Bakes the builder's settings into a concrete, immutable object.
     */
    fun build() = SlotOptions(id, renderFilled)
}

/**
 * Builds a selection box.
 */
fun selectionBox(bounds: AxisAlignedBB, init: SelectionBoxBuilder.() -> Unit = {}): SelectionBox {
    val builder = SelectionBoxBuilder(bounds)
    builder.init()
    return builder.build()
}

/**
 * Starts building render options for a selection box.
 */
fun renderOptions(init: RenderOptionsBuilder.() -> Unit = {}): RenderOptions {
    val builder = RenderOptionsBuilder()
    builder.init()
    return builder.build()
}

/**
 * Starts building slot options for a selection box.
 */
fun slotOptions(id: Int, init: SlotOptionsBuilder.() -> Unit = {}): SlotOptions {
    val builder = SlotOptionsBuilder(id)
    builder.init()
    return builder.build()
}

/**
 * Contains information about a sub selection box.
 * The stored bounding box is relative to the block's position.
 */
data class SelectionBox(val bounds: AxisAlignedBB,
                        val rotation: Rotation = Rotation.NONE,
                        val rendering: RenderOptions? = null,
                        val slot: SlotOptions? = null,
                        val rightClicks: Boolean = true,
                        val leftClicks: Boolean = true) {
    companion object {
        /**
         * Represents rendering options for a selection box.
         * If not specified for a given box, it will not be rendered.
         */
        data class RenderOptions(val icon: ResourceLocation? = null,
                                 val uvs: List<Vec3d> = listOf(Vec3d(.0, .0, .0),
                                                               Vec3d(.0, .0, 1.0),
                                                               Vec3d(1.0, .0, 1.0),
                                                               Vec3d(1.0, .0, .0)),
                                 val hoveredOnly: Boolean = false,
                                 val hoverColor: Vec3d? = null)

        /**
         * Represents slot options for a selection box.
         * If not specified for a given box, it will not be linked to a slot.
         */
        data class SlotOptions(val id: Int, val renderFilled: Boolean = false)
    }

    /**
     * Changes the selection boxes rotation and returns a modified instance.
     */
    fun withRotation(rotation: Rotation) = this.copy(rotation = rotation)
}

/**
 * Handles interaction with selection boxes.
 */
object SubSelectionHandler {
    /**
     * Performs a ray trace on a list of selection boxes.
     */
    fun rayTrace(player: EntityPlayer, pos: BlockPos, boxes: List<SelectionBox>): SelectionBox? {
        val result = Rays.rayTraceBoxes(player, boxes.map { it.bounds.rotateY(it.rotation).offset(pos) })
        if (result == -1)
            return null
        else
            return boxes[result]
    }

    /**
     * Performs a ray trace on all selection boxes within a TileEntity and gets its facing.
     */
    private fun getBoxHit(player: EntityPlayer, pos: BlockPos, tile: SubSelections,
                          filter: (SelectionBox) -> Boolean): Pair<SelectionBox, RayTraceResult>? {
        val hovered = rayTrace(player, pos, tile.boxes.filter(filter)) ?: return null
        val ray = Rays.rayTraceBox(player, hovered.bounds.rotateY(hovered.rotation).offset(pos))!!
        return hovered to ray
    }

    @SubscribeEvent
    fun onRightClick(event: PlayerInteractEvent.RightClickBlock) {
        val world = event.world
        val pos = event.pos
        val tile = world.getTileEntity(pos)
        val player = event.entityPlayer
        val hand = event.hand
        val stack = event.itemStack
        val side = event.face!!
        if (tile !is SubSelections)
            return
        // Perform a ray trace on all selection boxes and forward the call to the block if there was no result
        val (box, ray) = getBoxHit(player, pos, tile, { it.rightClicks }) ?:
                         if (tile.onRightClickBlock(player, hand, stack, side, event.hitVec)) {
                             event.isCanceled = true
                             return
                         } else {
                             return
                         }
        // Invert the rotation applied to the box
        val face = box.rotation.inverse.rotate(ray.sideHit)
        val hitVec = ((ray.hitVec - pos) - Vec3d(.5, .5, .5)).rotateY(box.rotation.inverse) +
                     Vec3d(.5, .5, .5) - box.bounds.min
        if (tile.onRightClickBox(box, player, hand, stack, face, hitVec))
            event.isCanceled = true
    }

    @SubscribeEvent
    fun onLeftClick(event: PlayerInteractEvent.LeftClickBlock) {
        val world = event.world
        val pos = event.pos
        val tile = world.getTileEntity(pos)
        val player = event.entityPlayer
        val hand = event.hand
        val stack = event.itemStack
        val side = event.face!!
        if (tile !is SubSelections)
            return
        // Perform a ray trace on all selection boxes and forward the call to the block if there was no result
        val (box, ray) = getBoxHit(player, pos, tile, { it.rightClicks }) ?:
                         if (tile.onLeftClickBlock(player, hand, stack, side, event.hitVec)) {
                             event.isCanceled = true
                             return
                         } else {
                             return
                         }
        // Invert the rotation applied to the box
        val face = box.rotation.inverse.rotate(ray.sideHit)
        val hitVec = ((ray.hitVec - pos) - Vec3d(.5, .5, .5)).rotateY(box.rotation.inverse) +
                     Vec3d(.5, .5, .5) - box.bounds.min
        if (tile.onLeftClickBox(box, player, hand, stack, face, hitVec))
            event.isCanceled = true
    }
}

/**
 * Renders sub selections for tile entities
 */
object SubSelectionRenderer {
    @SubscribeEvent
    fun onRenderHighlight(event: DrawBlockHighlightEvent) {
        // Mostly ripped from Vanilla
        val player = event.player
        val world = player.world
        if (event.target.typeOfHit != RayTraceResult.Type.BLOCK)
            return
        val pos = event.target.blockPos
        val tile = world.getTileEntity(pos)
        if (tile !is SubSelections)
            return
        val itemHandler = tile.getCapability(ITEM_HANDLER_CAPABILITY, null)
        val hovered = SubSelectionHandler.rayTrace(player, pos, tile.boxes)

        // Determines whether a given selection box should be rendered
        // Decides based on render and slot options
        fun renderFilter(box: SelectionBox) =
            box.rendering != null &&
            !(box.rendering.hoveredOnly && hovered != box) &&
            (box.slot == null ||
             (tile.hasCapability(ITEM_HANDLER_CAPABILITY, null) &&
              (box.slot.renderFilled ||
               itemHandler!!.getStackInSlot(box.slot.id).isEmpty)))

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
            tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                                 SourceFactor.ONE, DestFactor.ZERO)

            for (box in tile.boxes.filter(::renderFilter)) {
                val hoverColor = box.rendering?.hoverColor ?: Vec3d.ZERO
                val color = if (hovered == box) hoverColor else Vec3d.ZERO
                color(color.x.toFloat(), color.y.toFloat(), color.z.toFloat(), 0.4f)
                val rotated = box.bounds.rotateY(box.rotation).offset(pos)
                drawSelectionBoundingBox(rotated.grow(0.002),
                                         color.x.toFloat(), color.y.toFloat(), color.z.toFloat(), 0.4f)
            }

            // Draw highlight icons
            enableTexture2D()
            color(1f, 1f, 1f, 0.4f)
            tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE,
                                 SourceFactor.ONE, DestFactor.ZERO)
            for ((originalBox, rotation, rendering, slot) in tile.boxes.filter(::renderFilter)) {
                if (rendering!!.icon == null)
                    continue
                val box = originalBox.rotateY(rotation).offset(pos)
                val uvs = mutableListOf(*rendering.uvs.toTypedArray())
                // UV rotation can't be performed on the texture coordinates themselves but rather their order
                Collections.rotate(uvs, when (rotation) {
                    Rotation.COUNTERCLOCKWISE_90 -> 1
                    Rotation.CLOCKWISE_90 -> -1
                    Rotation.CLOCKWISE_180 -> 2
                    else -> 0
                })

                Minecraft.getMinecraft().renderEngine.bindTexture(rendering.icon)
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

        if (hovered != null && tile.cancelsVanillaSelectionRendering())
            event.isCanceled = true
    }
}