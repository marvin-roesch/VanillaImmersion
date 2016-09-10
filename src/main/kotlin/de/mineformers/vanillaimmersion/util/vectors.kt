@file:JvmName("VectorExtensions")

package de.mineformers.vanillaimmersion.util

import net.minecraft.util.Rotation
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import javax.vecmath.AxisAngle4d
import javax.vecmath.Matrix4d
import javax.vecmath.Vector4d

// Various extensions to do with Minecraft's vector types, should be all fairly self explanatory

val AxisAlignedBB.min: Vec3d
    get() = Vec3d(this.minX, this.minY, this.minZ)

val AxisAlignedBB.max: Vec3d
    get() = Vec3d(this.maxX, this.maxY, this.maxZ)

fun AxisAlignedBB.rotateX(rotation: Rotation) = rotate(Vec3d(1.0, .0, .0), rotation)

fun AxisAlignedBB.rotateY(rotation: Rotation) = rotate(Vec3d(.0, 1.0, .0), rotation)

fun AxisAlignedBB.rotateZ(rotation: Rotation) = rotate(Vec3d(.0, .0, 1.0), rotation)

fun AxisAlignedBB.rotate(axis: Vec3d, rotation: Rotation): AxisAlignedBB {
    val angle = when (rotation) {
        Rotation.COUNTERCLOCKWISE_90 -> Math.PI / 2
        Rotation.CLOCKWISE_90 -> -Math.PI / 2
        Rotation.CLOCKWISE_180 -> -Math.PI
        else -> .0
    }
    val offset = this.offset(-.5, -.5, -.5)
    return AxisAlignedBB(offset.min.rotate(axis, angle),
                         offset.max.rotate(axis, angle)).offset(.5, .5, .5)
}

fun AxisAlignedBB.contains(p: Vec3d) =
    p.x in minX..maxX &&
    p.y in minY..maxY &&
    p.z in minZ..maxZ

val Vec3d.x: Double
    get() = this.xCoord

val Vec3d.y: Double
    get() = this.yCoord

val Vec3d.z: Double
    get() = this.zCoord

val Vec3d.blockPos: BlockPos
    get() = BlockPos(this)

fun Vec3d.rotateX(rotation: Rotation) = rotate(Vec3d(1.0, .0, .0), rotation)

fun Vec3d.rotateY(rotation: Rotation) = rotate(Vec3d(.0, 1.0, .0), rotation)

fun Vec3d.rotateZ(rotation: Rotation) = rotate(Vec3d(.0, .0, 1.0), rotation)

fun Vec3d.rotate(axis: Vec3d, rotation: Rotation): Vec3d {
    val angle = when (rotation) {
        Rotation.COUNTERCLOCKWISE_90 -> Math.PI / 2
        Rotation.CLOCKWISE_90 -> -Math.PI / 2
        Rotation.CLOCKWISE_180 -> -Math.PI
        else -> .0
    }
    return rotate(axis, angle)
}

fun Vec3d.rotate(axis: Vec3d, angle: Double): Vec3d {
    val matrix = Matrix4d()
    matrix.setIdentity()
    matrix.setRotation(AxisAngle4d(axis.x, axis.y, axis.z, angle))
    val vec = Vector4d(x, y, z, 1.0)
    matrix.transform(vec)
    return Vec3d(vec.x, vec.y, vec.z)
}

val Rotation.inverse: Rotation
    get() = when (this) {
        Rotation.COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90
        Rotation.CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90
        else -> this
    }

operator fun Vec3d.get(coord: Int) =
    when (coord) {
        0 -> this.x
        1 -> this.y
        else -> this.z
    }

operator fun Vec3d.unaryPlus() = this

operator fun Vec3d.unaryMinus() = Vec3d(-x, -y, -z)

operator fun Vec3d.plus(b: Vec3d) = this.add(b)

operator fun Vec3d.plus(b: Vec3i) = this.add(Vec3d(b))


operator fun Vec3d.minus(b: Vec3d) = this.subtract(b)

operator fun Vec3d.minus(b: Vec3i) = this.subtract(Vec3d(b))


operator fun Byte.times(b: Vec3d) = b * this

operator fun Short.times(b: Vec3d) = b * this

operator fun Int.times(b: Vec3d) = b * this

operator fun Long.times(b: Vec3d) = b * this

operator fun Float.times(b: Vec3d) = b * this

operator fun Double.times(b: Vec3d) = b * this


operator fun Vec3d.times(b: Byte) = scale(b.toDouble())

operator fun Vec3d.times(b: Short) = scale(b.toDouble())

operator fun Vec3d.times(b: Int) = scale(b.toDouble())

operator fun Vec3d.times(b: Long) = scale(b.toDouble())

operator fun Vec3d.times(b: Float) = scale(b.toDouble())

operator fun Vec3d.times(b: Double) = scale(b)


operator fun Vec3d.times(b: Vec3d) = Vec3d(x * b.x, y * b.y, z * b.z)

operator fun Vec3d.times(b: Vec3i) = this * Vec3d(b)


operator fun Vec3d.div(b: Byte) = scale(1 / b.toDouble())

operator fun Vec3d.div(b: Short) = scale(1 / b.toDouble())

operator fun Vec3d.div(b: Int) = scale(1 / b.toDouble())

operator fun Vec3d.div(b: Long) = scale(1 / b.toDouble())

operator fun Vec3d.div(b: Float) = scale(1 / b.toDouble())

operator fun Vec3d.div(b: Double) = scale(1 / b)

operator fun Vec3d.div(b: Vec3d) = Vec3d(x / b.x, y / b.y, z / b.z)

operator fun Vec3d.div(b: Vec3i) = this / Vec3d(b)

val Vec3i.vec3d: Vec3d
    get() = Vec3d(this)

operator fun BlockPos.plus(b: Vec3i) = this.add(b)


operator fun BlockPos.minus(b: Vec3i) = this.subtract(b)


operator fun Byte.times(b: BlockPos) = this.toInt() * b

operator fun Short.times(b: BlockPos) = this.toInt() * b

operator fun Int.times(b: BlockPos) = BlockPos(b.x * this, b.y * this, b.z * this)

operator fun Long.times(b: BlockPos) = this.toInt() * b

operator fun Float.times(b: BlockPos) = Vec3d(this.toDouble(), this.toDouble(), this.toDouble()) * b

operator fun Double.times(b: BlockPos) = Vec3d(this, this, this) * b


operator fun BlockPos.times(b: Byte) = b * this

operator fun BlockPos.times(b: Short) = b * this

operator fun BlockPos.times(b: Int) = b * this

operator fun BlockPos.times(b: Long) = b * this

operator fun BlockPos.times(b: Float) = b * this

operator fun BlockPos.times(b: Double) = b * this


operator fun BlockPos.times(b: Vec3i) = BlockPos(x * b.x, y * b.y, z * b.z)


operator fun BlockPos.div(b: Vec3i) = BlockPos(x / b.x, y / b.y, z / b.z)