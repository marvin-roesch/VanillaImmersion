@file:JvmName("VectorExtensions")

package de.mineformers.vanillaimmersion.util

import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i

// Various extensions to do with Minecraft's vector types, should be all fairly self explanatory

val Vec3d.x: Double
    get() = this.xCoord

val Vec3d.y: Double
    get() = this.yCoord

val Vec3d.z: Double
    get() = this.zCoord

fun Vec3d.toBlockPos() = BlockPos(this)

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