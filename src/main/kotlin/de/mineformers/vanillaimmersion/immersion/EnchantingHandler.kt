package de.mineformers.vanillaimmersion.immersion

import de.mineformers.vanillaimmersion.tileentity.EnchantingTableLogic
import de.mineformers.vanillaimmersion.util.*
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import javax.vecmath.Matrix4f
import javax.vecmath.Vector3f
import javax.vecmath.Vector4f

/**
 * Handles interaction with the enchantment table's book "GUI".
 */
object EnchantingHandler {
    /**
     * Handles interaction with enchantment tables when a right click with an empty hand happens.
     */
    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    fun onRightClickEmpty(event: PlayerInteractEvent.RightClickEmpty) {
        if (event.hand == EnumHand.OFF_HAND)
            return
        onInteract(event, Minecraft.getMinecraft().objectMouseOver?.hitVec ?: Vec3d.ZERO)
    }

    /**
     * Handles interaction with enchantment tables when a right click on a block happens.
     */
    @SubscribeEvent
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        if (event.hand == EnumHand.OFF_HAND)
            return
        onInteract(event, event.hitVec)
    }

    /**
     * Handles interaction with any enchantment tables near a player.
     * Will scan a 7x7x7 cube around the player to check against since we can't rely
     * on the currently hovered block being the enchantment table.
     */
    private fun onInteract(event: PlayerInteractEvent, hovered: Vec3d) {
        val player = event.entityPlayer
        val surroundingBlocks = BlockPos.getAllInBox(player.position - BlockPos(3, 3, 3),
                                                     player.position + BlockPos(3, 3, 3))
        // Filter all enchantment tables out of the surrounding blocks and sort them by distance to the player
        // The sorting is required because otherwise an enchantment table further away might be prioritized.
        val enchantingTables =
            surroundingBlocks
                .filter { player.worldObj.getTileEntity(it) is EnchantingTableLogic }
                .sortedBy { player.getDistanceSq(it) }
                .map { player.worldObj.getTileEntity(it) as EnchantingTableLogic }
        if (enchantingTables.isEmpty())
            return

        val partialTicks = Rendering.partialTicks
        val origin = Rendering.getEyePosition(player, partialTicks)
        val direction = player.getLook(partialTicks)
        val hoverDistance = origin.squareDistanceTo(hovered)
        for (te in enchantingTables) {
            // If the active page is smaller than 0 (normally -1), the book is closed.
            if (te.page < 0)
                continue
            // Try to hit the left page first, then the right one
            if (tryHit(te, player, origin, direction, hoverDistance, partialTicks, false)) {
                if (event.isCancelable)
                    event.isCanceled = true
                return
            }
            if (tryHit(te, player, origin, direction, hoverDistance, partialTicks, true)) {
                if (event.isCancelable)
                    event.isCanceled = true
                return
            }
        }
    }

    /**
     * Sends a ray through the specified enchantment table's left or right page.
     * Will invoke the desired action for the appropriate page if the ray intersected it.
     */
    private fun tryHit(te: EnchantingTableLogic,
                       player: EntityPlayer, origin: Vec3d, direction: Vec3d, hoverDistance: Double,
                       partialTicks: Float, right: Boolean): Boolean {
        val quad = listOf(Vec3d(.0, .0, .0), Vec3d(6 * 0.0625, .0, .0),
                          Vec3d(6 * 0.0625, 8 * 0.0625, .0), Vec3d(.0, 8 * 0.0625, .0))
        val matrix = calculateMatrix(te, partialTicks, right)
        val hit = Rays.rayTraceQuad(origin, direction, quad, matrix)
        val distanceCheck =
            if (hit == null)
                false
            else {
                val v = Vector4f(hit.x.toFloat(), hit.y.toFloat(), hit.z.toFloat(), 1f)
                matrix.transform(v)
                origin.squareDistanceTo(v.x.toDouble(), v.y.toDouble(), v.z.toDouble()) < hoverDistance
            }
        if (hit != null && distanceCheck) {
            // The "GUI" pixel position of the hit depends on the clicked page since we have two different
            // reference corners
            val pixelHit =
                if (right)
                    Vec3d(0.0, 125.0, 0.0) - Vec3d(-hit.x.toDouble(), hit.y.toDouble(), 0.0) / 0.004
                else
                    Vec3d(94.0, 125.0, 0.0) - Vec3d(hit.x.toDouble(), hit.y.toDouble(), 0.0) / 0.004
            if (!player.worldObj.isRemote) {
                handleUIHit(te, right, player, pixelHit.x, pixelHit.y)
            } else {
                player.swingArm(EnumHand.MAIN_HAND)
            }
            return true
        }
        return false
    }

    /**
     * Handles a click on the enchantment table's "GUI", performing the appropriate action.
     */
    private fun handleUIHit(table: EnchantingTableLogic, right: Boolean, player: EntityPlayer, x: Double, y: Double) {
        table.performPageAction(player, table.page + if (right) 1 else 0, x, y)
    }

    /**
     * Calculates the transformation matrix for the left or right page of an enchantment table.
     * The inverse can be multiplied with any point in the table's local coordinate space to get its position relative
     * to the respective page.
     */
    private fun calculateMatrix(te: EnchantingTableLogic, partialTicks: Float, right: Boolean): Matrix4f {
        // See the enchantment table's renderer, we need to reproduce the transformations exactly
        val hover = te.tickCount + partialTicks
        var dYaw: Float = te.bookRotation - te.bookRotationPrev
        while (dYaw >= Math.PI) {
            dYaw -= Math.PI.toFloat() * 2f
        }

        while (dYaw < -Math.PI) {
            dYaw += Math.PI.toFloat() * 2f
        }

        val yaw = te.bookRotationPrev + dYaw * partialTicks
        var flipLeft = te.pageFlipPrev + (te.pageFlip - te.pageFlipPrev) * partialTicks + 0.25f

        flipLeft = (flipLeft - flipLeft.toInt()) * 1.6f - 0.3f
        if (flipLeft < 0.0f) {
            flipLeft = 0.0f
        }
        if (flipLeft > 1.0f) {
            flipLeft = 1.0f
        }

        val spread = te.bookSpreadPrev + (te.bookSpread - te.bookSpreadPrev) * partialTicks
        val breath = (MathHelper.sin(hover * 0.02f) * 0.1f + 1.25f) * spread
        val rotation = breath - breath * 2.0f * flipLeft

        // Again, see the renderer, the transformations are applied in the same order
        val result = Matrix4f()
        result.setIdentity()
        val tmp = Matrix4f()
        tmp.set(Vector3f(te.pos.x.toFloat(), te.pos.y.toFloat(), te.pos.z.toFloat()))
        result.mul(tmp)
        tmp.set(Vector3f(0.5f, 0.75f + 1.6f * 0.0625f + 0.0001f + MathHelper.sin(hover * 0.1f) * 0.01f, 0.5f))
        result.mul(tmp)
        tmp.rotY(-yaw)
        result.mul(tmp)
        tmp.rotZ(80 * Math.PI.toFloat() / 180)
        result.mul(tmp)
        tmp.set(Vector3f(MathHelper.sin(breath) / 16, 0f, 0f))
        result.mul(tmp)
        tmp.rotY(if (right) rotation else -rotation)
        result.mul(tmp)
        tmp.set(Vector3f(0f, -0.25f, 0f))
        result.mul(tmp)

        return result
    }
}