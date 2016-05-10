package de.mineformers.vanillaimmersion.client

import de.mineformers.vanillaimmersion.VanillaImmersion
import de.mineformers.vanillaimmersion.network.EnchantingAction
import de.mineformers.vanillaimmersion.tileentity.EnchantingTableLogic
import de.mineformers.vanillaimmersion.util.*
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumHand
import net.minecraft.util.Timer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.relauncher.ReflectionHelper
import javax.vecmath.Matrix4f
import javax.vecmath.Vector3f
import javax.vecmath.Vector4f

/**
 * ${JDOC}
 */
object EnchantingUIHandler {
    private val TIMER_FIELD by lazy {
        ReflectionHelper.findField(Minecraft::class.java, "field_71428_T", "timer")
    }

    init {
        TIMER_FIELD.isAccessible = true
    }

    val partialTicks: Float
        get() = (TIMER_FIELD.get(Minecraft.getMinecraft()) as Timer).renderPartialTicks

    @SubscribeEvent
    fun onRightClick(event: MouseEvent) {
        if (Minecraft.getMinecraft().theWorld == null || event.button != 1 || !event.isButtonstate)
            return
        val player = Minecraft.getMinecraft().thePlayer
        val surroundingBlocks = BlockPos.getAllInBox(player.position - BlockPos(3, 3, 3),
                                                     player.position + BlockPos(3, 3, 3))
        val enchantingTables =
            surroundingBlocks
                .filter { player.worldObj.getTileEntity(it) is EnchantingTableLogic }
                .sortedBy { player.getDistanceSq(it) }
                .map { player.worldObj.getTileEntity(it) as EnchantingTableLogic }
        if (enchantingTables.isEmpty())
            return
        val partialTicks = this.partialTicks
        val origin = player.getPositionEyes(partialTicks)
        val direction = player.getLook(partialTicks)
        val hovered = Minecraft.getMinecraft().objectMouseOver?.hitVec ?: Vec3d.ZERO
        val hoverDistance = origin.squareDistanceTo(hovered)
        for (te in enchantingTables) {
            if (te.page < 0)
                continue
            val leftPage = listOf(Vector4f(0f, 0f, 0f, 1f), Vector4f(6f * 0.0625f, 0f, 0f, 1f),
                                  Vector4f(6f * 0.0625f, 8f * 0.0625f, 0f, 1f), Vector4f(0f, 8f * 0.0625f, 0f, 1f))
            val leftMatrix = calculateMatrix(te, partialTicks, false)
            leftPage.forEach { leftMatrix.transform(it) }
            val left = leftPage.map { Vec3d(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()) + te.pos }
            val leftHit = mt(origin, direction, left[0], left[1], left[2], false) ?:
                          mt(origin, direction, left[0], left[2], left[3], false)
            leftMatrix.invert()
            if (leftHit != null && origin.squareDistanceTo(leftHit) < hoverDistance) {
                val h = leftHit - te.pos
                val v = Vector4f(h.x.toFloat(), h.y.toFloat(), h.z.toFloat(), 1f)
                leftMatrix.transform(v)
                val hitVec = Vec3d(94.0, 125.0, 0.0) - Vec3d(v.x.toDouble(), v.y.toDouble(), 0.0) / 0.004
                handleUIHit(te, false, player, hitVec.x, hitVec.y)
                player.swingArm(EnumHand.MAIN_HAND)
                event.isCanceled = true
                return
            }

            val rightPage = listOf(Vector4f(0f, 0f, 0f, 1f), Vector4f(6f * 0.0625f, 0f, 0f, 1f),
                                   Vector4f(6f * 0.0625f, 8f * 0.0625f, 0f, 1f), Vector4f(0f, 8f * 0.0625f, 0f, 1f))
            val rightMatrix = calculateMatrix(te, partialTicks, true)
            rightPage.forEach { rightMatrix.transform(it) }
            val right = rightPage.map { Vec3d(it.x.toDouble(), it.y.toDouble(), it.z.toDouble()) + te.pos }
            val rightHit = mt(origin, direction, right[0], right[1], right[2], false) ?:
                           mt(origin, direction, right[0], right[2], right[3], false)
            rightMatrix.invert()
            if (rightHit != null && origin.squareDistanceTo(rightHit) < hoverDistance) {
                val h = rightHit - te.pos
                val v = Vector4f(h.x.toFloat(), h.y.toFloat(), h.z.toFloat(), 1f)
                rightMatrix.transform(v)
                val hitVec = Vec3d(0.0, 125.0, 0.0) - Vec3d(-v.x.toDouble(), v.y.toDouble(), 0.0) / 0.004
                handleUIHit(te, true, player, hitVec.x, hitVec.y)
                player.swingArm(EnumHand.MAIN_HAND)
                event.isCanceled = true
                return
            }
        }
    }

    private fun handleUIHit(table: EnchantingTableLogic, right: Boolean, player: EntityPlayer, x: Double, y: Double) {
        VanillaImmersion.NETWORK.sendToServer(EnchantingAction.PageHitMessage(table.pos, right, x, y))
    }

    private fun mt(origin: Vec3d, dir: Vec3d,
                   v0: Vec3d, v1: Vec3d, v2: Vec3d,
                   cullBack: Boolean = true,
                   epsilon: Double = 1e-6): Vec3d? {
        // 2 edges of a triangle
        val e1 = v1 - v0
        val e2 = v2 - v0
        // determinant of the equation
        val p = dir.crossProduct(e2)
        val det = e1.dotProduct(p)
        if (cullBack) {
            if (det < epsilon) return null
            else {
                val t = origin - v0
                val du = t.dotProduct(p)
                if (du < 0.0 || du > det) return null
                else {
                    val q = t.crossProduct(e2)
                    val dv = dir.dotProduct(q)
                    if (dv < 0.0 || du + dv > det) return null
                    else return v0 + du * e2 + dv * e1
                }
            }
        } else {
            if (det < epsilon && det > -epsilon) return null
            else {
                val invDet = 1.0 / det
                val t = origin - v0
                val u = t.dotProduct(p) * invDet
                if (u < 0.0 || u > 1.0) return null
                else {
                    val q = t.crossProduct(e1)
                    val v = dir.dotProduct(q) * invDet
                    if (v < 0.0 || u + v > 1.0) return null
                    else return v0 + u * e1 + v * e2
                }
            }
        }
    }

    fun calculateMatrix(te: EnchantingTableLogic, partialTicks: Float, right: Boolean): Matrix4f {
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
        flipLeft = (flipLeft - MathHelper.truncateDoubleToInt(flipLeft.toDouble()).toFloat()) * 1.6f - 0.3f
        if (flipLeft < 0.0f) {
            flipLeft = 0.0f
        }
        if (flipLeft > 1.0f) {
            flipLeft = 1.0f
        }

        val spread = te.bookSpreadPrev + (te.bookSpread - te.bookSpreadPrev) * partialTicks
        val breath = (MathHelper.sin(hover * 0.02f) * 0.1f + 1.25f) * spread
        val rotation = breath - breath * 2.0f * flipLeft

        val result = Matrix4f()
        result.setIdentity()
        val tmp = Matrix4f()
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