package de.mineformers.vanillaimmersion.client

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.block.CraftingTable
import de.mineformers.vanillaimmersion.client.renderer.Shaders
import de.mineformers.vanillaimmersion.immersion.CraftingHandler.splitDrag
import de.mineformers.vanillaimmersion.network.CraftingDrag
import de.mineformers.vanillaimmersion.tileentity.CraftingTableLogic
import de.mineformers.vanillaimmersion.util.minus
import de.mineformers.vanillaimmersion.util.times
import de.mineformers.vanillaimmersion.util.toBlockPos
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.renderer.RenderHelper.enableStandardItemLighting
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.settings.KeyBinding
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11

/**
 * Handles dragging item stacks across a crafting table.
 * Mimics Vanilla GUI mechanics.
 */
object CraftingDragHandler {
    /**
     * The position currently hovered on the crafting grid.
     */
    private var dragPosition: Pair<Int, Int>? = null
    /**
     * The target of the current drag operation, i.e. the position of the crafting table.
     */
    private var dragTarget: BlockPos? = null
    /**
     * Determines whether dragging is currently in progress.
     */
    private var dragging = false
    /**
     * The stack to be distributed across the crafting grid.
     */
    private var dragStack: ItemStack? = null
    /**
     * A list of slots covered by the current dragging process.
     */
    private val dragSlots = emptyList<Int>().toMutableList()

    @SubscribeEvent
    fun onMouseInput(event: InputEvent.MouseInputEvent?) {
        onKeyboardInput(null)
    }

    /**
     *
     */
    @SubscribeEvent
    fun onKeyboardInput(event: InputEvent.KeyInputEvent?) {
        val key = Minecraft.getMinecraft().gameSettings.keyBindUseItem
        val correctKey =
            if (key.keyCode < 0)
                Mouse.getEventButton() == key.keyCode + 100
            else
                Keyboard.getEventKey() == key.keyCode
        val buttonDown =
            if (key.keyCode < 0)
                Mouse.getEventButtonState()
            else
                Keyboard.getEventKeyState()
        if (Minecraft.getMinecraft().theWorld == null || !correctKey)
            return
        val wasDragging = dragging
        val target = dragTarget
        updateDragTarget()
        val heldItem = Minecraft.getMinecraft().thePlayer.getHeldItem(EnumHand.MAIN_HAND)
        if (!wasDragging && dragTarget != null && buttonDown) {
            startDragging()
            KeyBinding.setKeyBindState(key.keyCode, false)
            while (key.isPressed) {
            }
        }
        if (wasDragging && (dragTarget == null || !buttonDown || dragStack != heldItem)) {
            stopDragging(target!!)
            KeyBinding.setKeyBindState(key.keyCode, false)
            while (key.isPressed) {
            }
        }
    }

    @SubscribeEvent
    fun onRightClick(event: PlayerInteractEvent.RightClickBlock) {
        if(dragging)
            event.isCanceled = true
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (Minecraft.getMinecraft().theWorld == null || event.phase == TickEvent.Phase.START)
            return
        val wasDragging = dragging
        val target = dragTarget
        updateDragTarget()
        val heldItem = Minecraft.getMinecraft().thePlayer.getHeldItem(EnumHand.MAIN_HAND)
        if (wasDragging && (dragTarget == null || !Mouse.isButtonDown(1) || dragStack != heldItem)) {
            stopDragging(target!!)
        }
        if (dragging && dragTarget != null && dragPosition != null && Mouse.isButtonDown(1)) {
            onDrag(dragPosition!!)
        }
    }

    fun startDragging() {
        dragging = true
        dragStack = Minecraft.getMinecraft().thePlayer.getHeldItem(EnumHand.MAIN_HAND)
        dragSlots.clear()
    }

    fun onDrag(pos: Pair<Int, Int>) {
        val (x, y) = pos
        val slot = x + y * 3
        if (!dragSlots.contains(slot))
            dragSlots.add(x + y * 3)
    }

    fun stopDragging(pos: BlockPos) {
        val tile = Minecraft.getMinecraft().theWorld.getTileEntity(pos)
        if (tile is CraftingTableLogic && dragStack != null && dragPosition != null) {
            VanillaImmersion.NETWORK.sendToServer(CraftingDrag.Message(pos, dragSlots))
        }
        dragSlots.clear()
        dragging = false
        dragTarget = null
        dragPosition = null
        dragStack = null
    }

    fun updateDragTarget() {
        val hovered = Minecraft.getMinecraft().objectMouseOver
        if (hovered != null && hovered.typeOfHit == RayTraceResult.Type.BLOCK) {
            val state = Minecraft.getMinecraft().theWorld.getBlockState(hovered.blockPos)
            if (state.block == VanillaImmersion.Blocks.CRAFTING_TABLE) {
                if (dragTarget == null) {
                    dragTarget = hovered.blockPos
                } else if (dragTarget != hovered.blockPos) {
                    dragTarget = null
                    return
                }
                if (hovered.sideHit == EnumFacing.UP) {
                    val facing = state.getValue(CraftingTable.FACING)
                    val angle = -Math.toRadians(180.0 - facing.horizontalAngle).toFloat()
                    val rot =
                        (-16 * ((hovered.hitVec - hovered.blockPos - Vec3d(0.5, 0.0, 0.5))
                                    .rotateYaw(angle) - Vec3d(0.5, 0.0, 0.5))).toBlockPos()
                    val x = rot.x - 3
                    val z = rot.z - 4
                    if (!(0..7).contains(x) || !(0..7).contains(z)) {
                        dragPosition = null
                        return
                    }
                    val (slotX, modX) = Pair(x / 3, x % 3)
                    val (slotZ, modZ) = Pair(z / 3, z % 3)
                    if (modX == 2 || modZ == 3) {
                        dragPosition = null
                        return
                    }
                    dragPosition = slotX.to(slotZ)
                    return
                }
            }
        }
        dragTarget = null
    }

    @SubscribeEvent
    fun onRenderDragStacks(event: RenderWorldLastEvent) {
        if (!dragging || dragSlots.isEmpty()) {
            return
        }
        pushMatrix()
        val camera = Minecraft.getMinecraft().renderViewEntity
        val player = Minecraft.getMinecraft().thePlayer
        val cX = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * event.partialTicks
        val cY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * event.partialTicks
        val cZ = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * event.partialTicks
        translate(-cX, -cY, -cZ)
        translate(dragTarget!!.x + 0.5, dragTarget!!.y.toDouble() + 1.0, dragTarget!!.z + 0.5)
        color(1f, 1f, 1f, 1f)
        val te = Minecraft.getMinecraft().theWorld.getTileEntity(dragTarget) as CraftingTableLogic
        rotate(180f - te.facing.horizontalAngle, 0f, 1f, 0f)
        translate(0.0625, 0.0, 0.0)
        Minecraft.getMinecraft().textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE)
        enableRescaleNormal()
        val light = te.world.getCombinedLight(te.pos.add(0, 1, 0), 0)
        val bX = light % 65536
        val bY = light / 65536
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bX.toFloat(), bY.toFloat())
        enableStandardItemLighting()

        enableBlend()
        tryBlendFuncSeparate(GL11.GL_ONE, GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
        Shaders.ALPHA.activate()
        Shaders.ALPHA.setUniformFloat("alpha", 0.5f)
        for (slot in dragSlots) {
            val (x, y) = Pair(slot % 3 - 1, slot / 3 - 1)
            if (splitDrag(te, player, dragStack, dragSlots, slot) > -1 && te.inventory.getStackInSlot(slot) == null)
                renderDragStack(x * -0.1875, y * -0.1875)
        }
        Shaders.ALPHA.deactivate()
        disableBlend()

        for (slot in dragSlots) {
            val (x, y) = Pair(slot % 3 - 1, slot / 3 - 1)
            val existing = te.inventory.getStackInSlot(slot + 1)?.stackSize ?: 0
            val amount = splitDrag(te, player, dragStack, dragSlots, slot)
            if (amount > -1) {
                pushMatrix()
                translate(x * -0.1875, 0.2, y * -0.1875)
                scale(0.00625f, -0.00625f, 0.00625f)
                rotate(180f, 0f, 1f, 0f)
                val font = Minecraft.getMinecraft().fontRendererObj
                val s = "${if (existing > 0) existing.toString() + "+" else ""}$amount"
                font.drawString(s, -font.getStringWidth(s) / 2, -font.FONT_HEIGHT, 0xFFFFFF)
                popMatrix()
            }
        }

        RenderHelper.disableStandardItemLighting()
        disableRescaleNormal()
        color(1f, 1f, 1f, 1f)
        popMatrix()
    }

    private fun renderDragStack(x: Double, z: Double) {
        pushMatrix()
        translate(x, 0.0703125, z)
        val stack = dragStack
        if (stack?.item is ItemBlock) {
            scale(2f, 2f, 2f)
        }
        scale(0.140625, 0.140625, 0.140625)
        Minecraft.getMinecraft().renderItem.renderItem(stack, ItemCameraTransforms.TransformType.FIXED)
        popMatrix()
    }
}